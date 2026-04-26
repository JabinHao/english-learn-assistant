package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CandidateCoarseFilter {

    private final AppConfig appConfig;
    private final Clock clock;

    public CandidateCoarseFilter(AppConfig appConfig) {
        this(appConfig, Clock.systemDefaultZone());
    }

    CandidateCoarseFilter(AppConfig appConfig, Clock clock) {
        this.appConfig = appConfig;
        this.clock = clock;
    }

    public List<FeedArticle> filter(List<FeedArticle> articles) {
        Map<String, FeedArticle> unique = new LinkedHashMap<>();
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(appConfig.getCandidate().getLookbackHours());

        for (FeedArticle article : articles) {
            if (article.url() == null || article.url().isBlank()) {
                continue;
            }
            if (article.publishedAt() != null && article.publishedAt().isBefore(cutoff)) {
                continue;
            }
            if (!matchesKeywords(article)) {
                continue;
            }
            unique.putIfAbsent(article.url(), article);
        }

        return List.copyOf(unique.values());
    }

    private boolean matchesKeywords(FeedArticle article) {
        if (appConfig.getFilters().getKeywords().isEmpty()) {
            return true;
        }

        String haystack = (article.title() + " " + article.summary()).toLowerCase(Locale.ROOT);
        return appConfig.getFilters().getKeywords().stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(haystack::contains);
    }
}
