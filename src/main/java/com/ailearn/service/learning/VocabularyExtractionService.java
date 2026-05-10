package com.ailearn.service.learning;

import com.ailearn.model.VocabularyCandidate;
import com.ailearn.observability.LlmTraceLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class VocabularyExtractionService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyExtractionService.class);
    private static final String CACHE_OPERATION = "vocabulary_extraction_v1";
    private static final TypeReference<List<VocabularyCandidate>> VOCABULARY_CACHE_TYPE = new TypeReference<>() {
    };

    public interface VocabularyChatClient {
        String chat(String prompt);
    }

    private final ObjectMapper objectMapper;
    private final LlmTraceLogger llmTraceLogger;
    private final VocabularyChatClient vocabularyChatClient;
    private final String promptTemplate;
    private final LlmPipelineCacheService cacheService;

    @Autowired
    public VocabularyExtractionService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            LlmTraceLogger llmTraceLogger,
            LlmPipelineCacheService cacheService,
            VocabularyChatClient vocabularyChatClient
    ) {
        this(objectMapper, llmTraceLogger, vocabularyChatClient, loadPrompt(resourceLoader), cacheService);
    }

    VocabularyExtractionService(
            ObjectMapper objectMapper,
            LlmTraceLogger llmTraceLogger,
            VocabularyChatClient vocabularyChatClient,
            String promptTemplate,
            LlmPipelineCacheService cacheService
    ) {
        this.objectMapper = objectMapper;
        this.llmTraceLogger = llmTraceLogger;
        this.vocabularyChatClient = vocabularyChatClient;
        this.promptTemplate = promptTemplate;
        this.cacheService = cacheService;
    }

    VocabularyExtractionService(
            ObjectMapper objectMapper,
            LlmTraceLogger llmTraceLogger,
            VocabularyChatClient vocabularyChatClient,
            String promptTemplate
    ) {
        this(objectMapper, llmTraceLogger, vocabularyChatClient, promptTemplate, null);
    }

    VocabularyExtractionService(
            ObjectMapper objectMapper,
            VocabularyChatClient vocabularyChatClient,
            String promptTemplate
    ) {
        this(objectMapper, new LlmTraceLogger(new com.ailearn.config.AppConfig()), vocabularyChatClient, promptTemplate, null);
    }

    public List<VocabularyCandidate> extract(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        String prompt = buildPrompt(paragraphs);
        if (cacheService != null) {
            List<VocabularyCandidate> cached = cacheService.read(CACHE_OPERATION, prompt, VOCABULARY_CACHE_TYPE).orElse(null);
            if (cached != null) {
                return cached;
            }
        }
        llmTraceLogger.logRequest(log, "vocabulary_extraction", prompt);
        try {
            String response = vocabularyChatClient.chat(prompt);
            llmTraceLogger.logResponse(log, "vocabulary_extraction", response);
            List<VocabularyCandidate> candidates = parseResponse(response);
            if (cacheService != null) {
                cacheService.write(CACHE_OPERATION, prompt, candidates);
            }
            return candidates;
        } catch (RuntimeException exception) {
            llmTraceLogger.logFailure(log, "vocabulary_extraction", exception);
            throw exception;
        }
    }

    List<VocabularyCandidate> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(response));
            JsonNode itemsNode = root.path("items");
            if (!itemsNode.isArray()) {
                throw new IllegalArgumentException("Vocabulary response missing items array");
            }

            List<VocabularyCandidate> items = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                String word = itemNode.path("word").asText("").trim();
                String type = itemNode.path("type").asText("").trim();
                if (word.isBlank() || type.isBlank()) {
                    continue;
                }
                items.add(new VocabularyCandidate(
                        word,
                        itemNode.path("lemma").asText("").trim(),
                        type,
                        itemNode.path("ipa").asText("").trim(),
                        itemNode.path("englishDefinition").asText("").trim(),
                        itemNode.path("chineseDefinition").asText("").trim(),
                        itemNode.path("sourceSentence").asText("").trim()
                ));
            }
            return items;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse vocabulary response", exception);
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
        Resource resource = resourceLoader.getResource("classpath:prompts/vocabulary-extraction-prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load vocabulary extraction prompt", exception);
        }
    }
}
