package com.ailearn.observability;

import com.ailearn.config.AppConfig;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class LlmTraceLogger {

    private final AppConfig appConfig;

    public LlmTraceLogger(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void logRequest(Logger logger, String operation, String prompt) {
        if (!appConfig.getObservability().isLogLlmRequests()) {
            return;
        }
        logger.info("llm.request operation={} prompt={}", operation, truncate(prompt, appConfig.getObservability().getMaxPromptLength()));
    }

    public void logResponse(Logger logger, String operation, String response) {
        if (!appConfig.getObservability().isLogLlmResponses()) {
            return;
        }
        logger.info("llm.response operation={} response={}", operation, truncate(response, appConfig.getObservability().getMaxResponseLength()));
    }

    public void logFailure(Logger logger, String operation, Exception exception) {
        logger.error("llm.failure operation={} error={}", operation, exception.getMessage(), exception);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...[truncated]";
    }
}
