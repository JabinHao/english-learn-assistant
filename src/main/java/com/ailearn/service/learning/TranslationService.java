package com.ailearn.service.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TranslationService {

    public interface TranslationChatClient {
        String chat(String prompt);
    }

    private final ObjectMapper objectMapper;
    private final TranslationChatClient translationChatClient;
    private final String promptTemplate;

    public TranslationService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            TranslationChatClient translationChatClient
    ) {
        this(objectMapper, translationChatClient, loadPrompt(resourceLoader));
    }

    TranslationService(
            ObjectMapper objectMapper,
            TranslationChatClient translationChatClient,
            String promptTemplate
    ) {
        this.objectMapper = objectMapper;
        this.translationChatClient = translationChatClient;
        this.promptTemplate = promptTemplate;
    }

    public List<String> translate(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        String response = translationChatClient.chat(buildPrompt(paragraphs));
        return parseResponse(response);
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
