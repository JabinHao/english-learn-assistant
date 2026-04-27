package com.ailearn.config;

import com.ailearn.service.candidate.CandidateRerankService;
import com.ailearn.service.learning.TranslationService;
import com.ailearn.service.learning.VocabularyExtractionService;
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
            @Value("${langchain4j.open-ai.chat-model.temperature}") Double temperature,
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
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

    @Bean
    TranslationService.TranslationChatClient translationChatClient(ChatLanguageModel chatLanguageModel) {
        return chatLanguageModel::chat;
    }

    @Bean
    VocabularyExtractionService.VocabularyChatClient vocabularyChatClient(ChatLanguageModel chatLanguageModel) {
        return chatLanguageModel::chat;
    }
}
