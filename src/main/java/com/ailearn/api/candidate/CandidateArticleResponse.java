package com.ailearn.api.candidate;

import java.time.LocalDateTime;

public record CandidateArticleResponse(
        Long id,
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String summary,
        Double score,
        String recommendationReason,
        boolean selected
) {
}
