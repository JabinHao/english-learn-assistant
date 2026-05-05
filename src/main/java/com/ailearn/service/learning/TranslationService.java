package com.ailearn.service.learning;

import com.ailearn.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ailearn.observability.LlmTraceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final int MAX_BATCH_PARAGRAPH_CHARS = 4_000;

    public interface TranslationChatClient {
        String chat(String prompt);
    }

    private final ObjectMapper objectMapper;
    private final LlmTraceLogger llmTraceLogger;
    private final TranslationChatClient translationChatClient;
    private final String promptTemplate;
    private final int maxTimeoutRetries;

    @Autowired
    public TranslationService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            LlmTraceLogger llmTraceLogger,
            AppConfig appConfig,
            TranslationChatClient translationChatClient
    ) {
        this(objectMapper, llmTraceLogger, translationChatClient, loadPrompt(resourceLoader), appConfig.getLlm().getMaxRetries());
    }

    TranslationService(
            ObjectMapper objectMapper,
            LlmTraceLogger llmTraceLogger,
            TranslationChatClient translationChatClient,
            String promptTemplate,
            int maxTimeoutRetries
    ) {
        this.objectMapper = objectMapper;
        this.llmTraceLogger = llmTraceLogger;
        this.translationChatClient = translationChatClient;
        this.promptTemplate = promptTemplate;
        this.maxTimeoutRetries = Math.max(0, maxTimeoutRetries);
    }

    TranslationService(
            ObjectMapper objectMapper,
            TranslationChatClient translationChatClient,
            String promptTemplate
    ) {
        this(objectMapper, new LlmTraceLogger(new AppConfig()), translationChatClient, promptTemplate, new AppConfig().getLlm().getMaxRetries());
    }

    public List<String> translate(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        List<String> translations = new ArrayList<>();
        List<List<String>> batches = batchParagraphs(paragraphs);
        log.info("translation.batch.start paragraphCount={} batchCount={}", paragraphs.size(), batches.size());
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<String> batch = batches.get(batchIndex);
            translations.addAll(translateBatch(batch, batchIndex + 1, batches.size()));
        }
        return translations;
    }

    private List<String> translateBatch(List<String> paragraphs, int batchNumber, int batchCount) {
        String prompt = buildPrompt(paragraphs);
        String operation = "paragraph_translation_batch_" + batchNumber + "_of_" + batchCount;
        llmTraceLogger.logRequest(log, operation, prompt);
        try {
            String response = chatWithTimeoutRetry(prompt, operation);
            llmTraceLogger.logResponse(log, operation, response);
            return parseResponse(response);
        } catch (RuntimeException exception) {
            llmTraceLogger.logFailure(log, operation, exception);
            throw exception;
        }
    }

    private String chatWithTimeoutRetry(String prompt, String operation) {
        RuntimeException lastException = null;
        int maxAttempts = maxTimeoutRetries + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return translationChatClient.chat(prompt);
            } catch (RuntimeException exception) {
                if (!isTimeout(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                lastException = exception;
                log.warn(
                        "translation.batch.timeout_retry operation={} attempt={} maxAttempts={} error={}",
                        operation,
                        attempt,
                        maxAttempts,
                        exception.getMessage()
                );
            }
        }
        throw lastException;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<List<String>> batchParagraphs(List<String> paragraphs) {
        List<List<String>> batches = new ArrayList<>();
        List<String> currentBatch = new ArrayList<>();
        int currentChars = 0;

        for (String paragraph : paragraphs) {
            int paragraphChars = paragraph.length();
            if (!currentBatch.isEmpty() && currentChars + paragraphChars > MAX_BATCH_PARAGRAPH_CHARS) {
                batches.add(List.copyOf(currentBatch));
                currentBatch.clear();
                currentChars = 0;
            }
            currentBatch.add(paragraph);
            currentChars += paragraphChars;
        }

        if (!currentBatch.isEmpty()) {
            batches.add(List.copyOf(currentBatch));
        }
        return batches;
    }

    List<String> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(response));
            JsonNode translationsNode = root.path("translations");
            if (!translationsNode.isArray()) {
                throw new IllegalArgumentException("Translation response missing translations array");
            }

            List<IndexedTranslation> items = new ArrayList<>();
            for (JsonNode translationNode : translationsNode) {
                int index = translationNode.path("index").asInt(-1);
                String chineseText = translationNode.path("chineseText").asText("").trim();
                if (index < 1 || chineseText.isBlank()) {
                    continue;
                }
                items.add(new IndexedTranslation(index, chineseText));
            }

            return items.stream()
                    .sorted(Comparator.comparingInt(IndexedTranslation::index))
                    .map(IndexedTranslation::chineseText)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse translation response", exception);
        }
    }

    private String buildPrompt(List<String> paragraphs) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < paragraphs.size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(paragraphs.get(index))
                    .append("\n\n");
        }
        return promptTemplate.replace("{{paragraphs}}", builder.toString().trim());
    }

    private String stripCodeFence(String response) {
        String trimmed = response.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        String withoutFence = trimmed.replaceFirst("^```(?:json)?\\s*", "");
        return withoutFence.replaceFirst("\\s*```\\s*$", "").trim();
    }

    private static String loadPrompt(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:prompts/translation-prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load translation prompt", exception);
        }
    }

    record IndexedTranslation(int index, String chineseText) {
    }
}
