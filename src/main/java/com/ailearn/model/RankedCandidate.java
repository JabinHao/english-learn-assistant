package com.ailearn.model;

public record RankedCandidate(
        String title,
        String url,
        String source,
        String summary,
        java.time.LocalDateTime publishedAt,
        double score,
        String reason
) {
}
