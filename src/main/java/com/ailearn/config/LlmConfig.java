package com.ailearn.config;

import com.ailearn.service.candidate.CandidateRerankService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Bean
    ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature}") Double temperature
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(1)
                .build();
    }

    @Bean
    CandidateRerankService.RerankChatClient rerankChatClient(ChatLanguageModel chatLanguageModel) {
        return chatLanguageModel::chat;
    }
}
