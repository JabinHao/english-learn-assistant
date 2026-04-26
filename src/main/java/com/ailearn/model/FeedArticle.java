package com.ailearn.model;

import java.time.LocalDateTime;

public record FeedArticle(
        String title,
        String url,
        String source,
        String summary,
        LocalDateTime publishedAt
) {
}
