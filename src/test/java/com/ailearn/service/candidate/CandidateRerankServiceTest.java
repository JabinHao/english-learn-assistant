package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateRerankServiceTest {

    @Test
    void rerank_shouldParseStructuredOutputAndReturnTopCandidates() {
        CandidateRerankService service = new CandidateRerankService(
                config(2, 6.5d),
                new ObjectMapper(),
                prompt -> """
                        {
                          "candidates": [
                            {"url":"https://example.com/c","score":6.1,"reason":"below threshold"},
                            {"url":"https://example.com/b","score":8.2,"reason":"Strong AI learning article"},
                            {"url":"https://example.com/unknown","score":9.9,"reason":"Not in source list"},
                            {"url":"https://example.com/a","score":9.1,"reason":"Timely AI product update"}
                          ]
                        }
                        """,
                "Prompt {{articles}}"
        );

        List<RankedCandidate> result = service.rerank(List.of(
                article("A", "https://example.com/a"),
                article("B", "https://example.com/b"),
                article("C", "https://example.com/c")
        ));

        assertThat(result).extracting(RankedCandidate::url)
                .containsExactly("https://example.com/a", "https://example.com/b");
        assertThat(result).extracting(RankedCandidate::score)
                .containsExactly(9.1d, 8.2d);
        assertThat(result.getFirst().reason()).isEqualTo("Timely AI product update");
    }

    @Test
    void parseResponse_shouldAcceptJsonWrappedInMarkdownFence() {
        CandidateRerankService service = new CandidateRerankService(
                config(5, 0.0d),
                new ObjectMapper(),
                prompt -> "",
                "Prompt {{articles}}"
        );

        List<CandidateRerankService.ScoredUrl> parsed = service.parseResponse("""
                ```json
                {
                  "candidates": [
                    {"url":"https://example.com/a","score":8.0,"reason":"Good fit"}
                  ]
                }
                ```
                """);

        assertThat(parsed)
                .extracting(CandidateRerankService.ScoredUrl::url)
                .containsExactly("https://example.com/a");
    }

    @Test
    void parseResponse_shouldFailWhenCandidatesArrayMissing() {
        CandidateRerankService service = new CandidateRerankService(
                config(5, 0.0d),
                new ObjectMapper(),
                prompt -> "",
                "Prompt {{articles}}"
        );

        assertThatThrownBy(() -> service.parseResponse("{\"items\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidates array");
    }

    private AppConfig config(int maxCandidates, double minScore) {
        AppConfig appConfig = new AppConfig();
        AppConfig.Candidate candidate = new AppConfig.Candidate();
        candidate.setMaxCandidates(maxCandidates);
        candidate.setMinScore(minScore);
        appConfig.setCandidate(candidate);
        return appConfig;
    }

    private FeedArticle article(String title, String url) {
        return new FeedArticle(
                title,
                url,
                "Test",
                "Summary about AI systems.",
                LocalDateTime.parse("2026-04-26T10:00:00")
        );
    }
}
