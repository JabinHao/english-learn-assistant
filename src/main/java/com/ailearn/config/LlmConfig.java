package com.ailearn.config;

import com.ailearn.service.candidate.CandidateRerankService;
import com.ailearn.service.learning.TranslationService;
import com.ailearn.service.learning.VocabularyExtractionService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Bean
    ChatLanguageModel chatLanguageModel(
            AppConfig appConfig,
            org.springframework.core.env.Environment environment
    ) {
        String apiKey = environment.getRequiredProperty("langchain4j.open-ai.chat-model.api-key");
        String modelName = environment.getRequiredProperty("langchain4j.open-ai.chat-model.model-name");
        Double temperature = environment.getRequiredProperty("langchain4j.open-ai.chat-model.temperature", Double.class);
        String baseUrl = environment.getRequiredProperty("langchain4j.open-ai.chat-model.base-url");

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(appConfig.getLlm().getTimeoutSeconds()))
                .maxRetries(appConfig.getLlm().getMaxRetries())
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
