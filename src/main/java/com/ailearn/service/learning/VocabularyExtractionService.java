package com.ailearn.service.learning;

import com.ailearn.model.VocabularyCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public interface VocabularyChatClient {
        String chat(String prompt);
    }

    private final ObjectMapper objectMapper;
    private final VocabularyChatClient vocabularyChatClient;
    private final String promptTemplate;

    @Autowired
    public VocabularyExtractionService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            VocabularyChatClient vocabularyChatClient
    ) {
        this(objectMapper, vocabularyChatClient, loadPrompt(resourceLoader));
    }

    VocabularyExtractionService(
            ObjectMapper objectMapper,
            VocabularyChatClient vocabularyChatClient,
            String promptTemplate
    ) {
        this.objectMapper = objectMapper;
        this.vocabularyChatClient = vocabularyChatClient;
        this.promptTemplate = promptTemplate;
    }

    public List<VocabularyCandidate> extract(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        String response = vocabularyChatClient.chat(buildPrompt(paragraphs));
        return parseResponse(response);
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
