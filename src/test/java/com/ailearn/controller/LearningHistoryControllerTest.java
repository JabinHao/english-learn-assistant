package com.ailearn.controller;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.LearningArticleRepository;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningHistoryControllerTest {

    @Test
    void getLearningHistory_shouldReturnSelectedArticlesNewestFirst() throws Exception {
        LearningArticleEntity newest = article(
                12L,
                "Newest article",
                "OpenAI",
                "VOCAB_READY",
                "2026-04-28T09:00:00",
                "2026-04-28T20:00:00"
        );
        LearningArticleEntity older = article(
                10L,
                "Older article",
                "Anthropic",
                "TRANSLATED",
                "2026-04-26T09:00:00",
                "2026-04-27T20:00:00"
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LearningHistoryController(repository(List.of(newest, older))))
                .setMessageConverters(jacksonConverter())
                .build();

        mockMvc.perform(get("/api/learning-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].title").value("Newest article"))
                .andExpect(jsonPath("$[0].source").value("OpenAI"))
                .andExpect(jsonPath("$[0].publishedAt").value("2026-04-28T09:00:00"))
                .andExpect(jsonPath("$[0].selectedAt").value("2026-04-28T20:00:00"))
                .andExpect(jsonPath("$[0].status").value("VOCAB_READY"))
                .andExpect(jsonPath("$[1].id").value(10));
    }

    private LearningArticleRepository repository(List<LearningArticleEntity> articles) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByOrderBySelectedAtDescCreatedAtDesc" -> articles;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private MappingJackson2HttpMessageConverter jacksonConverter() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private LearningArticleEntity article(
            Long id,
            String title,
            String source,
            String status,
            String publishedAt,
            String selectedAt
    ) {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", id + 100);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", id);
        article.setCandidateArticle(candidateArticle);
        article.setTitle(title);
        article.setUrl("https://example.com/" + id);
        article.setSource(source);
        article.setStatus(status);
        article.setPublishedAt(LocalDateTime.parse(publishedAt));
        article.setSelectedAt(LocalDateTime.parse(selectedAt));
        article.setCreatedAt(LocalDateTime.parse(selectedAt).minusMinutes(1));
        return article;
    }
}
