package com.ailearn.api.learning;

import java.time.LocalDateTime;

public record LearningHistoryItemResponse(
        Long id,
        String title,
        String source,
        LocalDateTime publishedAt,
        LocalDateTime selectedAt,
        String status
) {
}
