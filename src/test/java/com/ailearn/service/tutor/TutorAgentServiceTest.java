package com.ailearn.service.tutor;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TutorAgentServiceTest {

    @Test
    void reply_shouldUseArticleContextHistoryAndUserQuestion() {
        List<ChatMessage> capturedMessages = new ArrayList<>();
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override
            public ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest chatRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ChatResponse chat(List<ChatMessage> messages) {
                capturedMessages.addAll(messages);
                return ChatResponse.builder()
                        .aiMessage(dev.langchain4j.data.message.AiMessage.from("这是解释"))
                        .build();
            }
        };

        TutorAgentService service = new TutorAgentService(
                model,
                new DefaultResourceLoader(),
                new ArticleTutorContextService(null, null, null) {
                    @Override
                    public String buildContext(Long learningArticleId) {
                        return "Article Title: Selected article\nParagraphs:\n1. EN: Hello";
                    }
                }
        );

        String reply = service.reply(
                88L,
                List.of(
                        new TutorAgentService.HistoricalChatMessage("user", "先总结一下"),
                        new TutorAgentService.HistoricalChatMessage("assistant", "这是总结")
                ),
                "解释第一段"
        );

        assertThat(reply).isEqualTo("这是解释");
        assertThat(capturedMessages.getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(capturedMessages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) capturedMessages.get(1)).singleText()).contains("Article Title: Selected article");
        assertThat(((UserMessage) capturedMessages.getLast()).singleText()).isEqualTo("解释第一段");
    }
}
