package com.ailearn.controller;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.service.candidate.CandidateGenerationService;
import com.ailearn.service.learning.CandidateSelectionService;
import com.ailearn.service.learning.LearningWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateControllerTest {

    @Test
    void getTodayCandidates_shouldReturnRankedArticlesForCurrentDate() throws Exception {
        CandidateBatchEntity batch = new CandidateBatchEntity();
        batch.setRunDate(LocalDate.of(2026, 4, 26));

        CandidateArticleEntity first = article(1L, batch, "Latest reasoning model", "https://example.com/a", 1, 8.9d, "Strong AI relevance");
        CandidateArticleEntity second = article(2L, batch, "Inference cost guide", "https://example.com/b", 2, 8.1d, "Good learning depth");

        CandidateArticleRepository repository = repository(List.of(first, second));
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);
        CandidateSelectionService selectionService = new CandidateSelectionService(repository, null, workflowService(), clock) {
        };
        CandidateGenerationService generationService = generationService();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateController(repository, generationService, selectionService, clock))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/candidates/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Latest reasoning model"))
                .andExpect(jsonPath("$[0].chineseTitle").value("中文 Latest reasoning model"))
                .andExpect(jsonPath("$[0].score").value(8.9d))
                .andExpect(jsonPath("$[0].chineseSummary").value("中文摘要"))
                .andExpect(jsonPath("$[0].recommendationReason").value("Strong AI relevance"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void generateTodayCandidates_shouldTriggerGenerationAndReturnTodayCandidates() throws Exception {
        CandidateBatchEntity batch = new CandidateBatchEntity();
        batch.setRunDate(LocalDate.of(2026, 4, 26));

        CandidateArticleEntity article = article(1L, batch, "Latest reasoning model", "https://example.com/a", 1, 8.9d, "Strong AI relevance");
        CandidateArticleRepository repository = repository(List.of(article));
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);
        AtomicBoolean triggered = new AtomicBoolean(false);
        CandidateGenerationService generationService = generationService(triggered);
        CandidateSelectionService selectionService = new CandidateSelectionService(repository, null, workflowService(), clock) {
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateController(repository, generationService, selectionService, clock))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/candidates/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Latest reasoning model"));

        org.junit.jupiter.api.Assertions.assertTrue(triggered.get());
    }

    private LearningWorkflowService workflowService() {
        return new LearningWorkflowService(null, null, null, null, null, null, null, null, null, Clock.systemUTC()) {
            @Override
            public com.ailearn.entity.LearningArticleEntity processLearningArticle(Long learningArticleId) {
                return null;
            }
        };
    }

    private CandidateGenerationService generationService() {
        return generationService(new AtomicBoolean(false));
    }

    private CandidateGenerationService generationService(AtomicBoolean triggered) {
        return new CandidateGenerationService(null, null, null, null, null, Clock.systemUTC()) {
            @Override
            public CandidateBatchEntity generateToday() {
                triggered.set(true);
                return new CandidateBatchEntity();
            }
        };
    }

    private CandidateArticleRepository repository(List<CandidateArticleEntity> articles) {
        return (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByBatchRunDateOrderByRankOrderAscCreatedAtAsc" -> articles;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CandidateArticleEntity article(
            Long id,
            CandidateBatchEntity batch,
            String title,
            String url,
            int rankOrder,
            double score,
            String reason
    ) {
        CandidateArticleEntity entity = new CandidateArticleEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setBatch(batch);
        entity.setTitle(title);
        entity.setChineseTitle("中文 " + title);
        entity.setUrl(url);
        entity.setSource("Test");
        entity.setPublishedAt(LocalDateTime.parse("2026-04-26T09:00:00"));
        entity.setSummary("Summary");
        entity.setChineseSummary("中文摘要");
        entity.setRankOrder(rankOrder);
        entity.setLlmScore(score);
        entity.setLlmReason(reason);
        entity.setSelected(false);
        return entity;
    }
}
