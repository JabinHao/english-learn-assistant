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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TutorChatSessionsControllerTest {

    @Test
    void listSessions_shouldReturnSummariesOrderedNewestFirst() throws Exception {
        LearningArticleEntity article = learningArticle();
        ChatSessionEntity older = session(11L, article, "Older chat", LocalDateTime.of(2026, 5, 10, 10, 0));
        ChatSessionEntity newer = session(12L, article, null, LocalDateTime.of(2026, 5, 15, 10, 0));
        ChatMessageEntity message = message(1L, older, "user", "hi", LocalDateTime.of(2026, 5, 10, 10, 1));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepositoryWith(Map.of(11L, older, 12L, newer), List.of(newer, older)),
                        messageRepositoryWith(Map.of(11L, List.of(message), 12L, List.of())),
                        tutorService("ignored", null)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/learning-articles/88/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].title").doesNotExist())
                .andExpect(jsonPath("$[0].messageCount").value(0))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].title").value("Older chat"))
                .andExpect(jsonPath("$[1].messageCount").value(1));
    }

    @Test
    void createSession_shouldReturnNewEmptySession() throws Exception {
        LearningArticleEntity article = learningArticle();
        AtomicLong sessionId = new AtomicLong(20L);
        AtomicReference<ChatSessionEntity> savedSession = new AtomicReference<>();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        creatingSessionRepository(sessionId, savedSession),
                        messageRepositoryWith(Map.of()),
                        tutorService("ignored", null)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/learning-articles/88/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(20))
                .andExpect(jsonPath("$.messages.length()").value(0));
        assertThat(savedSession.get()).isNotNull();
        assertThat(savedSession.get().getTitle()).isNull();
    }

    @Test
    void sendToSession_shouldAutoTitleFromFirstUserMessage() throws Exception {
        LearningArticleEntity article = learningArticle();
        ChatSessionEntity session = session(55L, article, null, LocalDateTime.of(2026, 5, 10, 10, 0));
        AtomicLong messageId = new AtomicLong(1L);
        List<ChatMessageEntity> stored = new ArrayList<>();
        AtomicReference<String> titleAfterSave = new AtomicReference<>();

        ChatSessionRepository sessionRepository = (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdAndLearningArticleId" -> Optional.of(session);
                    case "findFirstByLearningArticleIdOrderByCreatedAtDesc" -> Optional.of(session);
                    case "save" -> {
                        ChatSessionEntity saved = (ChatSessionEntity) args[0];
                        if (saved.getTitle() != null) {
                            titleAfterSave.set(saved.getTitle());
                        }
                        yield saved;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository,
                        messageRepository(stored, messageId, session),
                        tutorService("reply", null)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/learning-articles/88/chat/sessions/55/messages")
                        .contentType("application/json")
                        .content("{\"message\":\"Explain paragraph two please\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(55))
                .andExpect(jsonPath("$.messages.length()").value(2));
        assertThat(titleAfterSave.get()).isEqualTo("Explain paragraph two please");
    }

    @Test
    void renameSession_shouldUpdateTitle() throws Exception {
        LearningArticleEntity article = learningArticle();
        ChatSessionEntity session = session(55L, article, "old", LocalDateTime.of(2026, 5, 10, 10, 0));
        AtomicReference<String> savedTitle = new AtomicReference<>();

        ChatSessionRepository sessionRepository = (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdAndLearningArticleId" -> Optional.of(session);
                    case "save" -> {
                        savedTitle.set(((ChatSessionEntity) args[0]).getTitle());
                        yield args[0];
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository,
                        messageRepositoryWith(Map.of(55L, List.of())),
                        tutorService("ignored", null)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(patch("/api/learning-articles/88/chat/sessions/55")
                        .contentType("application/json")
                        .content("{\"title\":\"Vocabulary deep-dive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vocabulary deep-dive"));
        assertThat(savedTitle.get()).isEqualTo("Vocabulary deep-dive");
    }

    @Test
    void deleteSession_shouldRemoveSession() throws Exception {
        LearningArticleEntity article = learningArticle();
        ChatSessionEntity session = session(55L, article, null, LocalDateTime.of(2026, 5, 10, 10, 0));
        AtomicReference<ChatSessionEntity> deleted = new AtomicReference<>();

        ChatSessionRepository sessionRepository = (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdAndLearningArticleId" -> Optional.of(session);
                    case "delete" -> {
                        deleted.set((ChatSessionEntity) args[0]);
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TutorChatController(
                        learningRepository(article),
                        sessionRepository,
                        messageRepositoryWith(Map.of()),
                        tutorService("ignored", null)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(delete("/api/learning-articles/88/chat/sessions/55"))
                .andExpect(status().isNoContent());
        assertThat(deleted.get()).isSameAs(session);
    }

    private LearningArticleEntity learningArticle() {
        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        return article;
    }

    private ChatSessionEntity session(Long id, LearningArticleEntity article, String title, LocalDateTime createdAt) {
        ChatSessionEntity session = new ChatSessionEntity();
        ReflectionTestUtils.setField(session, "id", id);
        session.setLearningArticle(article);
        session.setTitle(title);
        session.setCreatedAt(createdAt);
        return session;
    }

    private ChatMessageEntity message(Long id, ChatSessionEntity session, String role, String content, LocalDateTime createdAt) {
        ChatMessageEntity msg = new ChatMessageEntity();
        ReflectionTestUtils.setField(msg, "id", id);
        msg.setChatSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(createdAt);
        return msg;
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

    private ChatSessionRepository sessionRepositoryWith(
            Map<Long, ChatSessionEntity> byId,
            List<ChatSessionEntity> orderedDesc
    ) {
        return (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLearningArticleIdOrderByCreatedAtDesc" -> orderedDesc;
                    case "findByIdAndLearningArticleId" -> Optional.ofNullable(byId.get(args[0]));
                    case "save" -> args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ChatSessionRepository creatingSessionRepository(
            AtomicLong idSequence,
            AtomicReference<ChatSessionEntity> savedSession
    ) {
        return (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class[]{ChatSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        ChatSessionEntity saved = (ChatSessionEntity) args[0];
                        ReflectionTestUtils.setField(saved, "id", idSequence.getAndIncrement());
                        savedSession.set(saved);
                        yield saved;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ChatMessageRepository messageRepositoryWith(Map<Long, List<ChatMessageEntity>> bySession) {
        return (ChatMessageRepository) Proxy.newProxyInstance(
                ChatMessageRepository.class.getClassLoader(),
                new Class[]{ChatMessageRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByChatSessionIdOrderByCreatedAtAsc" -> bySession.getOrDefault(args[0], List.of());
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
                        message.setCreatedAt(LocalDateTime.of(2026, 5, 10, 10, (int) idSequence.get()));
                        storedMessages.add(message);
                        yield message;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
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
}
