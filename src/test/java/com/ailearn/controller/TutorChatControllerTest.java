package com.ailearn.controller;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.ChatMessageEntity;
import com.ailearn.entity.ChatSessionEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.ChatMessageRepository;
import com.ailearn.repository.ChatSessionRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.service.tutor.TutorAgentService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TutorChatControllerTest {

    @Test
    void chat_shouldPersistUserAndAssistantMessages() throws Exception {
        LearningArticleEntity article = learningArticle();
        AtomicLong messageId = new AtomicLong(1L);
        List<ChatMessageEntity> storedMessages = new ArrayList<>();
        ChatSessionEntity session = new ChatSessionEntity();
        ReflectionTestUtils.setField(session, "id", 55L);
        session.setLearningArticle(article);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository(session),
                        messageRepository(storedMessages, messageId, session),
                        tutorService("这是回答")))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/learning-articles/88/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"解释第一段\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(55))
                .andExpect(jsonPath("$.reply").value("这是回答"))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }

    @Test
    void chat_shouldPassRequestContextToTutorService() throws Exception {
        LearningArticleEntity article = learningArticle();
        AtomicLong messageId = new AtomicLong(1L);
        List<ChatMessageEntity> storedMessages = new ArrayList<>();
        ChatSessionEntity session = new ChatSessionEntity();
        ReflectionTestUtils.setField(session, "id", 55L);
        session.setLearningArticle(article);
        AtomicReference<TutorAgentService.TutorRequestContext> capturedContext = new AtomicReference<>();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository(session),
                        messageRepository(storedMessages, messageId, session),
                        tutorService("这是回答", capturedContext)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/learning-articles/88/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"解释这一段\",\"paragraphIndex\":2,\"mode\":\"ASK\",\"intent\":\"EXPLAIN_PARAGRAPH\"}"))
                .andExpect(status().isOk());

        assertThat(capturedContext.get()).isEqualTo(
                new TutorAgentService.TutorRequestContext(2, null, "ASK", "EXPLAIN_PARAGRAPH")
        );
    }

    private LearningArticleRepository learningRepository(LearningArticleEntity article) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(article);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ChatSessionRepository sessionRepository(ChatSessionEntity session) {
        return (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByLearningArticleIdOrderByCreatedAtAsc" -> Optional.of(session);
                    case "save" -> args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ChatMessageRepository messageRepository(
            List<ChatMessageEntity> storedMessages,
            AtomicLong idSequence,
            ChatSessionEntity session
    ) {
        return (ChatMessageRepository) Proxy.newProxyInstance(
                ChatMessageRepository.class.getClassLoader(),
                new Class[]{ChatMessageRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByChatSessionIdOrderByCreatedAtAsc" -> storedMessages;
                    case "save" -> {
                        ChatMessageEntity message = (ChatMessageEntity) args[0];
                        ReflectionTestUtils.setField(message, "id", idSequence.getAndIncrement());
                        message.setChatSession(session);
                        message.setCreatedAt(LocalDateTime.of(2026, 4, 26, 12, 30, (int) idSequence.get()));
                        storedMessages.add(message);
                        yield message;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private TutorAgentService tutorService(String reply) {
        return tutorService(reply, null);
    }

    private TutorAgentService tutorService(
            String reply,
            AtomicReference<TutorAgentService.TutorRequestContext> capturedContext
    ) {
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override
            public ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest chatRequest) {
                throw new UnsupportedOperationException();
            }
        };
        return new TutorAgentService(model, new DefaultResourceLoader(), new com.ailearn.service.tutor.ArticleTutorContextService(null, null, null) {
            @Override
            public String buildContext(Long learningArticleId) {
                return "context";
            }
        }) {
            @Override
            public String reply(Long learningArticleId, List<HistoricalChatMessage> history, String userMessage) {
                return reply;
            }

            @Override
            public String reply(
                    Long learningArticleId,
                    List<HistoricalChatMessage> history,
                    String userMessage,
                    TutorRequestContext requestContext
            ) {
                if (capturedContext != null) {
                    capturedContext.set(requestContext);
                }
                return reply;
            }
        };
    }

    private LearningArticleEntity learningArticle() {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 7L);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        article.setCandidateArticle(candidateArticle);
        article.setTitle("Selected article");
        return article;
    }
}
