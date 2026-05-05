package com.ailearn.api.candidate;

import java.time.LocalDateTime;

public record CandidateArticleResponse(
        Long id,
        String title,
        String chineseTitle,
        String url,
        String source,
        LocalDateTime publishedAt,
        String summary,
        String chineseSummary,
        Double score,
        String recommendationReason,
        boolean selected
) {
}
