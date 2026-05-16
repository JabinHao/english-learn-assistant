import { apiFetch } from "./client";
import type { TutorChatRequest, TutorChatResponse } from "./types";

export function loadChatHistory(
  learningArticleId: number,
): Promise<TutorChatResponse> {
  return apiFetch<TutorChatResponse>(
    `/api/learning-articles/${learningArticleId}/chat`,
  );
}

export function sendChatMessage(
  learningArticleId: number,
  request: TutorChatRequest,
): Promise<TutorChatResponse> {
  return apiFetch<TutorChatResponse>(
    `/api/learning-articles/${learningArticleId}/chat`,
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );
}
