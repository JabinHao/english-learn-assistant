package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CandidateRerankService {

    public interface RerankChatClient {
        String chat(String prompt);
    }

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final RerankChatClient rerankChatClient;
    private final String promptTemplate;

    public CandidateRerankService(
            AppConfig appConfig,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            RerankChatClient rerankChatClient
    ) {
        this(appConfig, objectMapper, rerankChatClient, loadPrompt(resourceLoader));
    }

    CandidateRerankService(
            AppConfig appConfig,
            ObjectMapper objectMapper,
            RerankChatClient rerankChatClient,
            String promptTemplate
    ) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.rerankChatClient = rerankChatClient;
        this.promptTemplate = promptTemplate;
    }

    public List<RankedCandidate> rerank(List<FeedArticle> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }

        Map<String, FeedArticle> articleByUrl = new LinkedHashMap<>();
        for (FeedArticle article : articles) {
            articleByUrl.put(article.url(), article);
        }

        String response = rerankChatClient.chat(buildPrompt(articles));
        List<ScoredUrl> scoredUrls = parseResponse(response);

        return scoredUrls.stream()
                .filter(candidate -> articleByUrl.containsKey(candidate.url()))
                .filter(candidate -> candidate.score() >= appConfig.getCandidate().getMinScore())
                .sorted(Comparator.comparingDouble(ScoredUrl::score).reversed())
                .limit(appConfig.getCandidate().getMaxCandidates())
                .map(candidate -> toRankedCandidate(articleByUrl.get(candidate.url()), candidate))
                .toList();
    }

    List<ScoredUrl> parseResponse(String response) {
        try {
            String normalized = stripCodeFence(response);
            JsonNode root = objectMapper.readTree(normalized);
            JsonNode candidatesNode = root.path("candidates");
            if (!candidatesNode.isArray()) {
                throw new IllegalArgumentException("LLM response missing candidates array");
            }

            List<ScoredUrl> candidates = new ArrayList<>();
            for (JsonNode candidateNode : candidatesNode) {
                String url = candidateNode.path("url").asText("").trim();
                double score = candidateNode.path("score").asDouble(Double.NaN);
                String reason = candidateNode.path("reason").asText("").trim();
                if (url.isBlank() || Double.isNaN(score)) {
                    continue;
                }
                candidates.add(new ScoredUrl(url, score, reason));
            }
            return candidates;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse candidate rerank response", exception);
        }
    }

    private RankedCandidate toRankedCandidate(FeedArticle article, ScoredUrl candidate) {
        return new RankedCandidate(
                article.title(),
                article.url(),
                article.source(),
                article.summary(),
                article.publishedAt(),
                candidate.score(),
                candidate.reason()
        );
    }

    private String buildPrompt(List<FeedArticle> articles) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < articles.size(); index++) {
            FeedArticle article = articles.get(index);
            builder.append(index + 1)
                    .append(". title: ").append(article.title())
                    .append("\n   url: ").append(article.url())
                    .append("\n   source: ").append(article.source())
                    .append("\n   publishedAt: ").append(article.publishedAt())
                    .append("\n   summary: ").append(article.summary())
                    .append("\n\n");
        }
        return promptTemplate.replace("{{articles}}", builder.toString().trim());
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
        Resource resource = resourceLoader.getResource("classpath:prompts/candidate-rerank-prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load candidate rerank prompt", exception);
        }
    }

    record ScoredUrl(String url, double score, String reason) {
    }
}
