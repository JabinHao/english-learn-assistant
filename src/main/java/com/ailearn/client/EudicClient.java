package com.ailearn.client;

import com.ailearn.config.AppConfig;
import com.ailearn.entity.VocabularyItemEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class EudicClient {

    private final AppConfig appConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public EudicClient(AppConfig appConfig) {
        this(appConfig, HttpClient.newHttpClient(), new ObjectMapper());
    }

    EudicClient(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return appConfig.getEudic().getAuthToken() != null && !appConfig.getEudic().getAuthToken().isBlank();
    }

    public String ensureStudyList() {
        String existingId = findStudyListId();
        if (existingId != null) {
            return existingId;
        }
        return createStudyList();
    }

    public boolean pushWord(String studyListId, VocabularyItemEntity item) {
        Map<String, String> payload = Map.of(
                "id", studyListId,
                "language", appConfig.getEudic().getLanguage(),
                "word", item.getWord(),
                "exp", item.getSourceSentence() == null ? "" : item.getSourceSentence()
        );

        HttpRequest request = requestBuilder("/api/open/v1/studylist/word")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to push vocabulary item to Eudic", exception);
        }
    }

    public boolean pushNote(String studyListId, VocabularyItemEntity item) {
        Map<String, String> payload = Map.of(
                "id", studyListId,
                "language", appConfig.getEudic().getLanguage(),
                "word", item.getWord(),
                "note", buildNote(item)
        );

        HttpRequest request = requestBuilder("/api/open/v1/studylist/note")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to push vocabulary note to Eudic", exception);
        }
    }

    public boolean deleteWord(String studyListId, VocabularyItemEntity item) {
        String path = "/api/open/v1/studylist/word?id=" + encode(studyListId)
                + "&language=" + encode(appConfig.getEudic().getLanguage())
                + "&word=" + encode(item.getWord());
        HttpRequest request = requestBuilder(path)
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to delete vocabulary item from Eudic", exception);
        }
    }

    private String findStudyListId() {
        HttpRequest request = requestBuilder("/api/open/v1/studylist/category?language=" + appConfig.getEudic().getLanguage())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode categories = objectMapper.readTree(response.body());
            if (!categories.isArray()) {
                return null;
            }
            for (JsonNode category : categories) {
                if (appConfig.getEudic().getStudyListName().equals(category.path("name").asText())) {
                    return category.path("id").asText();
                }
            }
            return null;
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to resolve Eudic study list", exception);
        }
    }

    private String createStudyList() {
        Map<String, String> payload = Map.of(
                "language", appConfig.getEudic().getLanguage(),
                "name", appConfig.getEudic().getStudyListName()
        );
        HttpRequest request = requestBuilder("/api/open/v1/studylist/category")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode created = objectMapper.readTree(response.body());
            return created.path("id").asText();
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to create Eudic study list", exception);
        }
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(appConfig.getEudic().getBaseUrl() + path))
                .header("Authorization", "NIS " + appConfig.getEudic().getAuthToken())
                .header("Content-Type", "application/json");
    }

    private String writeJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize Eudic request body", exception);
        }
    }

    private String buildNote(VocabularyItemEntity item) {
        List<String> parts = List.of(
                item.getIpa() == null || item.getIpa().isBlank() ? null : "IPA: " + item.getIpa(),
                item.getEnglishDefinition() == null || item.getEnglishDefinition().isBlank() ? null : "EN: " + item.getEnglishDefinition(),
                item.getChineseDefinition() == null || item.getChineseDefinition().isBlank() ? null : "ZH: " + item.getChineseDefinition()
        );
        return parts.stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
