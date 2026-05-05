package com.ailearn.model;

public record RankedCandidate(
        String title,
        String chineseTitle,
        String url,
        String source,
        String summary,
        String chineseSummary,
        java.time.LocalDateTime publishedAt,
        double score,
        String reason
) {
}
