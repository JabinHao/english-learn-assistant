package com.ailearn.integration;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.repository.LearningArticleRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TutorChatIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private CandidateBatchRepository candidateBatchRepository;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private LearningArticleRepository learningArticleRepository;

    private Long learningArticleId;

    @BeforeEach
    void setUp() {
        learningArticleRepository.deleteAll();
        candidateArticleRepository.deleteAll();
        candidateBatchRepository.deleteAll();

        CandidateBatchEntity batch = new CandidateBatchEntity();
        batch.setRunDate(LocalDate.of(2026, 4, 26));
        batch.setStatus("COMPLETED");
        batch = candidateBatchRepository.save(batch);

        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        candidateArticle.setBatch(batch);
        candidateArticle.setTitle("OpenAI reasoning update");
        candidateArticle.setUrl("https://example.com/a");
        candidateArticle.setSource("OpenAI");
        candidateArticle.setPublishedAt(LocalDateTime.of(2026, 4, 26, 9, 0));
        candidateArticle.setSummary("A strong candidate for study.");
        candidateArticle.setSelected(true);
        candidateArticle = candidateArticleRepository.save(candidateArticle);

        LearningArticleEntity learningArticle = new LearningArticleEntity();
        learningArticle.setCandidateArticle(candidateArticle);
        learningArticle.setStatus("EUDIC_PUSHED");
        learningArticle.setTitle(candidateArticle.getTitle());
        learningArticle.setUrl(candidateArticle.getUrl());
        learningArticle.setSource(candidateArticle.getSource());
        learningArticle.setPublishedAt(candidateArticle.getPublishedAt());
        learningArticle.setSummary(candidateArticle.getSummary());
        learningArticle.setArticleContent("Paragraph one.\n\nParagraph two.");
        learningArticle.setSelectedAt(LocalDateTime.of(2026, 4, 26, 9, 5));
        learningArticle = learningArticleRepository.save(learningArticle);
        learningArticleId = learningArticle.getId();
    }

    @Test
    void chatFlow_shouldPersistAndReturnConversation() throws Exception {
        mockMvc.perform(post("/api/learning-articles/{id}/chat", learningArticleId)
                        .contentType("application/json")
                        .content("{\"message\":\"请总结这篇文章\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("这是 tutor 的回答"))
                .andExpect(jsonPath("$.messages.length()").value(2));

        mockMvc.perform(get("/api/learning-articles/{id}/chat", learningArticleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("这是 tutor 的回答"))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }

    @TestConfiguration
    static class TutorChatOverrides {

        @Bean
        @Primary
        ChatLanguageModel testChatLanguageModel() {
            return new ChatLanguageModel() {
                @Override
                public ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest chatRequest) {
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from("这是 tutor 的回答"))
                            .build();
                }
            };
        }
    }
}
