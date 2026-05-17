package com.ailearn.api.chat;

import jakarta.validation.constraints.Size;

public record UpdateChatSessionRequest(
        @Size(max = 255, message = "title must be at most 255 characters")
        String title
) {
}
