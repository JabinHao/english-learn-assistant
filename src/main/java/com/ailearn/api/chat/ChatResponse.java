package com.ailearn.api.chat;

import java.util.List;

public record ChatResponse(
        Long sessionId,
        Long learningArticleId,
        String reply,
        List<ChatMessageResponse> messages
) {
}
