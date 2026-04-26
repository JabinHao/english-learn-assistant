package com.ailearn.api.learning;

public record SelectCandidateResponse(
        Long learningArticleId,
        Long candidateArticleId,
        String status
) {
}
