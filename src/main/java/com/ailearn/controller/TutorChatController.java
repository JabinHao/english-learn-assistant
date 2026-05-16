package com.ailearn.controller;

import com.ailearn.api.chat.ChatMessageResponse;
import com.ailearn.api.chat.ChatRequest;
import com.ailearn.api.chat.ChatResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/learning-articles/{learningArticleId}/chat")
public class TutorChatController {

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
        LearningArticleEntity learningArticle = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));

        ChatSessionEntity session = chatSessionRepository.findFirstByLearningArticleIdOrderByCreatedAtAsc(learningArticleId)
                .orElseGet(() -> createSession(learningArticle));
        List<ChatMessageEntity> history = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());

        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setChatSession(session);
        userMessage.setRole("user");
        userMessage.setContent(request.message());
        chatMessageRepository.save(userMessage);

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

    @GetMapping
    public ChatResponse history(@PathVariable Long learningArticleId) {
        ChatSessionEntity session = chatSessionRepository.findFirstByLearningArticleIdOrderByCreatedAtAsc(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));
        List<ChatMessageEntity> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        String lastAssistantReply = messages.stream()
                .filter(message -> "assistant".equalsIgnoreCase(message.getRole()))
                .reduce((first, second) -> second)
                .map(ChatMessageEntity::getContent)
                .orElse("");
        return toResponse(session, learningArticleId, lastAssistantReply, messages);
    }

    private ChatSessionEntity createSession(LearningArticleEntity learningArticle) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setLearningArticle(learningArticle);
        return chatSessionRepository.save(session);
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
}
