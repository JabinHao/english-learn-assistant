package com.ailearn.api.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLearningArticleRequest(
        @NotBlank
        @Size(max = 10000)
        String url
) {
}
