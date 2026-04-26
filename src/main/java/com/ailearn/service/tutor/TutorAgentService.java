package com.ailearn.service.tutor;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TutorAgentService {

    private final ChatLanguageModel chatLanguageModel;
    private final String systemPrompt;
    private final ArticleTutorContextService articleTutorContextService;

    public TutorAgentService(
            ChatLanguageModel chatLanguageModel,
            ResourceLoader resourceLoader,
            ArticleTutorContextService articleTutorContextService
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.systemPrompt = loadPrompt(resourceLoader);
        this.articleTutorContextService = articleTutorContextService;
    }

    public String reply(Long learningArticleId, List<HistoricalChatMessage> history, String userMessage) {
        String context = articleTutorContextService.buildContext(learningArticleId);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from("Article context:\n" + context));

        for (HistoricalChatMessage message : history) {
            if ("assistant".equalsIgnoreCase(message.role())) {
                messages.add(dev.langchain4j.data.message.AiMessage.from(message.content()));
            } else {
                messages.add(UserMessage.from(message.content()));
            }
        }

        messages.add(UserMessage.from(userMessage));
        return chatLanguageModel.chat(messages).aiMessage().text();
    }

    private static String loadPrompt(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:prompts/tutor-system-prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load tutor system prompt", exception);
        }
    }

    public record HistoricalChatMessage(String role, String content) {
    }
}
