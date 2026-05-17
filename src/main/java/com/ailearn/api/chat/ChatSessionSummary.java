package com.ailearn.api.chat;

import java.time.LocalDateTime;

public record ChatSessionSummary(
        Long id,
        Long learningArticleId,
        String title,
        LocalDateTime createdAt,
        int messageCount,
        LocalDateTime lastMessageAt
) {
}
