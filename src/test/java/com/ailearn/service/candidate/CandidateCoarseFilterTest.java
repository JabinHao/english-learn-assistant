package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateCoarseFilterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void filter_shouldRemoveDuplicateUrls() {
        CandidateCoarseFilter filter = new CandidateCoarseFilter(config(), clock);

        List<FeedArticle> result = filter.filter(List.of(
                article("AI agents for developers", "https://example.com/a", -1),
                article("AI agents for developers duplicate", "https://example.com/a", -1)
        ));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("AI agents for developers");
    }

    @Test
    void filter_shouldRemoveArticlesOutsideLookbackWindow() {
        CandidateCoarseFilter filter = new CandidateCoarseFilter(config(), clock);

        List<FeedArticle> result = filter.filter(List.of(
                article("Modern LLM serving patterns", "https://example.com/fresh", -10),
                article("Old LLM serving patterns", "https://example.com/stale", -72)
        ));

        assertThat(result).extracting(FeedArticle::url).containsExactly("https://example.com/fresh");
    }

    @Test
    void filter_shouldRequireConfiguredKeywords() {
        CandidateCoarseFilter filter = new CandidateCoarseFilter(config(), clock);

        List<FeedArticle> result = filter.filter(List.of(
                article("RAG reliability patterns", "https://example.com/rag", -2),
                new FeedArticle(
                        "Quarterly hiring update",
                        "https://example.com/hr",
                        "Test",
                        "Team expansion and office move.",
                        LocalDateTime.now(clock).minusHours(2)
                )
        ));

        assertThat(result).extracting(FeedArticle::url).containsExactly("https://example.com/rag");
    }

    @Test
    void filter_shouldRemoveArticlesWithTooLittleText() {
        CandidateCoarseFilter filter = new CandidateCoarseFilter(config(), clock);

        List<FeedArticle> result = filter.filter(List.of(
                new FeedArticle(
                        "AI",
                        "https://example.com/short",
                        "Test",
                        "Brief.",
                        LocalDateTime.now(clock).minusHours(1)
                ),
                article("LLM orchestration patterns for product teams", "https://example.com/long", -1)
        ));

        assertThat(result).extracting(FeedArticle::url).containsExactly("https://example.com/long");
    }

    private AppConfig config() {
        AppConfig appConfig = new AppConfig();
        AppConfig.Candidate candidate = new AppConfig.Candidate();
        candidate.setLookbackHours(36);
        appConfig.setCandidate(candidate);

        AppConfig.Filters filters = new AppConfig.Filters();
        filters.setKeywords(List.of("ai", "llm", "rag", "agent", "inference"));
        filters.setMinTextLength(40);
        appConfig.setFilters(filters);
        return appConfig;
    }

    private FeedArticle article(String title, String url, int hoursOffset) {
        return new FeedArticle(
                title,
                url,
                "Test",
                title + " summary about AI systems.",
                LocalDateTime.now(clock).plusHours(hoursOffset)
        );
    }
}
