package com.ailearn.controller;

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
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TutorChatHistoryControllerTest {

    @Test
    void history_shouldReturnStoredMessages() throws Exception {
        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        ChatSessionEntity session = new ChatSessionEntity();
        ReflectionTestUtils.setField(session, "id", 55L);
        session.setLearningArticle(article);

        ChatMessageEntity user = new ChatMessageEntity();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setRole("user");
        user.setContent("解释第一段");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 26, 12, 0));

        ChatMessageEntity assistant = new ChatMessageEntity();
        ReflectionTestUtils.setField(assistant, "id", 2L);
        assistant.setRole("assistant");
        assistant.setContent("这是回答");
        assistant.setCreatedAt(LocalDateTime.of(2026, 4, 26, 12, 1));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository(session),
                        messageRepository(List.of(user, assistant)),
                        tutorService()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/learning-articles/88/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(55))
                .andExpect(jsonPath("$.reply").value("这是回答"))
                .andExpect(jsonPath("$.messages.length()").value(2));
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
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ChatMessageRepository messageRepository(List<ChatMessageEntity> messages) {
        return (ChatMessageRepository) Proxy.newProxyInstance(
                ChatMessageRepository.class.getClassLoader(),
                new Class[]{ChatMessageRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByChatSessionIdOrderByCreatedAtAsc" -> messages;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private TutorAgentService tutorService() {
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
        });
    }
}
