package com.ailearn.integration;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
import com.ailearn.service.candidate.CandidateGenerationService;
import com.ailearn.service.candidate.CandidateRerankService;
import com.ailearn.service.rss.RssFetchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CandidatePipelineIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ailearn")
            .withUsername("ailearn")
            .withPassword("ailearn");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CandidateGenerationService candidateGenerationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateToday_andReadTodayCandidates_shouldPersistAndExposeCandidates() throws Exception {
        candidateGenerationService.generateToday();

        mockMvc.perform(get("/api/candidates/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("OpenAI launches reasoning update"))
                .andExpect(jsonPath("$[0].recommendationReason").value("Timely AI product update"))
                .andExpect(jsonPath("$[1].title").value("Inference optimization guide"));
    }

    @TestConfiguration
    static class IntegrationOverrides {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        RssFetchService rssFetchService() {
            return new RssFetchService(new AppConfig()) {
                @Override
                public List<FeedArticle> fetchAll() {
                    return List.of(
                            article("OpenAI launches reasoning update", "https://example.com/a", "OpenAI released a new reasoning-focused model with practical details about model behavior, evaluation, deployment tradeoffs, and AI product learning.", -2),
                            article("Inference optimization guide", "https://example.com/b", "A practical guide to lower inference cost for AI systems, including batching, caching, latency targets, model routing, and production monitoring.", -3),
                            article("Company hiring update", "https://example.com/c", "General business update with no AI keywords and enough detail to prove that reranking, not text-length filtering, removes this item from results.", -4)
                    );
                }
            };
        }

        @Bean
        @Primary
        CandidateRerankService candidateRerankService(ResourceLoader resourceLoader) {
            return new CandidateRerankService(new AppConfig(), new ObjectMapper(), resourceLoader, prompt -> "") {
                @Override
                public List<RankedCandidate> rerank(List<FeedArticle> articles) {
                    return articles.stream()
                            .filter(article -> !article.url().equals("https://example.com/c"))
                            .sorted(Comparator.comparing((FeedArticle article) -> article.url().endsWith("/a") ? 9.2d : 8.4d).reversed())
                            .map(article -> new RankedCandidate(
                                    article.title(),
                                    article.url(),
                                    article.source(),
                                    article.summary(),
                                    article.publishedAt(),
                                    article.url().endsWith("/a") ? 9.2d : 8.4d,
                                    article.url().endsWith("/a") ? "Timely AI product update" : "Good technical reading candidate"
                            ))
                            .toList();
                }
            };
        }

        private static FeedArticle article(String title, String url, String summary, int hoursOffset) {
            return new FeedArticle(
                    title,
                    url,
                    "Test Feed",
                    summary,
                    LocalDateTime.of(2026, 4, 26, 8, 0).plusHours(hoursOffset)
            );
        }
    }
}
