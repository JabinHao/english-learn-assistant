package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
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

    public CandidateCoarseFilter(AppConfig appConfig, Clock clock) {
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
            if (!looksLikeArticleUrl(article.url())) {
                continue;
            }
            if (article.publishedAt() != null && article.publishedAt().isBefore(cutoff)) {
                continue;
            }
            if (articleTextLength(article) < appConfig.getFilters().getMinTextLength()) {
                continue;
            }
            if (!matchesKeywords(article)) {
                continue;
            }
            unique.putIfAbsent(article.url(), article);
        }

        return List.copyOf(unique.values());
    }

    private boolean looksLikeArticleUrl(String url) {
        URI uri;
        try {
            uri = new URI(url.strip());
        } catch (URISyntaxException exception) {
            return false;
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (isNonArticleHost(normalizedHost)) {
            return false;
        }

        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".tar.gz")
                || path.endsWith(".tgz") || path.endsWith(".dmg") || path.endsWith(".exe")) {
            return false;
        }

        return !path.contains("/issues/")
                && !path.contains("/pull/")
                && !path.contains("/releases/")
                && !path.contains("/commit/")
                && !path.contains("/tree/")
                && !path.contains("/blob/");
    }

    private boolean isNonArticleHost(String host) {
        return host.equals("github.com")
                || host.endsWith(".github.com")
                || host.equals("gitlab.com")
                || host.endsWith(".gitlab.com")
                || host.equals("bitbucket.org")
                || host.endsWith(".bitbucket.org")
                || host.equals("raw.githubusercontent.com")
                || host.equals("gist.github.com")
                || host.equals("npmjs.com")
                || host.equals("www.npmjs.com")
                || host.equals("pypi.org")
                || host.equals("crates.io");
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

    private int articleTextLength(FeedArticle article) {
        return (article.title() + " " + article.summary()).trim().length();
    }
}
