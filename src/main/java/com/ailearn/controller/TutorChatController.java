package com.ailearn.controller;

import com.ailearn.api.chat.ChatMessageResponse;
import com.ailearn.api.chat.ChatRequest;
import com.ailearn.api.chat.ChatResponse;
import com.ailearn.api.chat.ChatSessionSummary;
import com.ailearn.api.chat.UpdateChatSessionRequest;
import com.ailearn.entity.ChatMessageEntity;
import com.ailearn.entity.ChatSessionEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.ChatMessageRepository;
import com.ailearn.repository.ChatSessionRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.service.tutor.TutorAgentService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/learning-articles/{learningArticleId}/chat")
public class TutorChatController {

    private static final int AUTO_TITLE_MAX_LENGTH = 40;

    private final LearningArticleRepository learningArticleRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TutorAgentService tutorAgentService;

    public TutorChatController(
            LearningArticleRepository learningArticleRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            TutorAgentService tutorAgentService
    ) {
        this.learningArticleRepository = learningArticleRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.tutorAgentService = tutorAgentService;
    }

    @PostMapping
    @Transactional
    public ChatResponse chat(
            @PathVariable Long learningArticleId,
            @Valid @RequestBody ChatRequest request
    ) {
        LearningArticleEntity learningArticle = requireArticle(learningArticleId);
        ChatSessionEntity session = chatSessionRepository.findFirstByLearningArticleIdOrderByCreatedAtDesc(learningArticleId)
                .orElseGet(() -> createSession(learningArticle, null));
        return sendMessage(session, learningArticleId, request);
    }

    @GetMapping
    public ChatResponse history(@PathVariable Long learningArticleId) {
        ChatSessionEntity session = chatSessionRepository.findFirstByLearningArticleIdOrderByCreatedAtDesc(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));
        return loadSession(session, learningArticleId);
    }

    @GetMapping("/sessions")
    public List<ChatSessionSummary> listSessions(@PathVariable Long learningArticleId) {
        requireArticle(learningArticleId);
        return chatSessionRepository.findByLearningArticleIdOrderByCreatedAtDesc(learningArticleId).stream()
                .map(this::toSummary)
                .toList();
    }

    @PostMapping("/sessions")
    @Transactional
    public ChatResponse createNewSession(@PathVariable Long learningArticleId) {
        LearningArticleEntity learningArticle = requireArticle(learningArticleId);
        ChatSessionEntity session = createSession(learningArticle, null);
        return new ChatResponse(session.getId(), learningArticleId, "", List.of());
    }

    @GetMapping("/sessions/{sessionId}")
    public ChatResponse getSession(
            @PathVariable Long learningArticleId,
            @PathVariable Long sessionId
    ) {
        ChatSessionEntity session = requireSession(learningArticleId, sessionId);
        return loadSession(session, learningArticleId);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Transactional
    public ChatResponse sendToSession(
            @PathVariable Long learningArticleId,
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest request
    ) {
        requireArticle(learningArticleId);
        ChatSessionEntity session = requireSession(learningArticleId, sessionId);
        return sendMessage(session, learningArticleId, request);
    }

    @PatchMapping("/sessions/{sessionId}")
    @Transactional
    public ChatSessionSummary renameSession(
            @PathVariable Long learningArticleId,
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateChatSessionRequest request
    ) {
        ChatSessionEntity session = requireSession(learningArticleId, sessionId);
        session.setTitle(request.title() != null && request.title().isBlank() ? null : request.title());
        chatSessionRepository.save(session);
        return toSummary(session);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteSession(
            @PathVariable Long learningArticleId,
            @PathVariable Long sessionId
    ) {
        ChatSessionEntity session = requireSession(learningArticleId, sessionId);
        chatSessionRepository.delete(session);
    }

    private LearningArticleEntity requireArticle(Long learningArticleId) {
        return learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));
    }

    private ChatSessionEntity requireSession(Long learningArticleId, Long sessionId) {
        return chatSessionRepository.findByIdAndLearningArticleId(sessionId, learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));
    }

    private ChatSessionEntity createSession(LearningArticleEntity learningArticle, String title) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setLearningArticle(learningArticle);
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    private ChatResponse sendMessage(ChatSessionEntity session, Long learningArticleId, ChatRequest request) {
        List<ChatMessageEntity> history = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());

        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setChatSession(session);
        userMessage.setRole("user");
        userMessage.setContent(request.message());
        chatMessageRepository.save(userMessage);

        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle(autoTitle(request.message()));
            chatSessionRepository.save(session);
        }

        String reply = tutorAgentService.reply(
                learningArticleId,
                history.stream()
                        .map(message -> new TutorAgentService.HistoricalChatMessage(message.getRole(), message.getContent()))
                        .toList(),
                request.message(),
                new TutorAgentService.TutorRequestContext(
                        request.paragraphIndex(),
                        request.selectedText(),
                        request.mode(),
                        request.intent()
                )
        );

        ChatMessageEntity assistantMessage = new ChatMessageEntity();
        assistantMessage.setChatSession(session);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        chatMessageRepository.save(assistantMessage);

        List<ChatMessageEntity> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        return toResponse(session, learningArticleId, reply, messages);
    }

    private ChatResponse loadSession(ChatSessionEntity session, Long learningArticleId) {
        List<ChatMessageEntity> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        String lastAssistantReply = messages.stream()
                .filter(message -> "assistant".equalsIgnoreCase(message.getRole()))
                .reduce((first, second) -> second)
                .map(ChatMessageEntity::getContent)
                .orElse("");
        return toResponse(session, learningArticleId, lastAssistantReply, messages);
    }

    private ChatSessionSummary toSummary(ChatSessionEntity session) {
        List<ChatMessageEntity> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        return new ChatSessionSummary(
                session.getId(),
                session.getLearningArticle().getId(),
                session.getTitle(),
                session.getCreatedAt(),
                messages.size(),
                messages.isEmpty() ? null : messages.get(messages.size() - 1).getCreatedAt()
        );
    }

    private ChatResponse toResponse(
            ChatSessionEntity session,
            Long learningArticleId,
            String reply,
            List<ChatMessageEntity> messages
    ) {
        return new ChatResponse(
                session.getId(),
                learningArticleId,
                reply,
                messages.stream()
                        .map(message -> new ChatMessageResponse(
                                message.getId(),
                                message.getRole(),
                                message.getContent(),
                                message.getCreatedAt()
                        ))
                        .toList()
        );
    }

    private String autoTitle(String message) {
        String trimmed = message.strip().replaceAll("\\s+", " ");
        if (trimmed.length() <= AUTO_TITLE_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, AUTO_TITLE_MAX_LENGTH).strip() + "…";
    }
}
