import { apiFetch } from "./client";
import type {
  ChatSessionSummary,
  TutorChatRequest,
  TutorChatResponse,
} from "./types";

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

export function listChatSessions(
  learningArticleId: number,
): Promise<ChatSessionSummary[]> {
  return apiFetch<ChatSessionSummary[]>(
    `/api/learning-articles/${learningArticleId}/chat/sessions`,
  );
}

export function createChatSession(
  learningArticleId: number,
): Promise<TutorChatResponse> {
  return apiFetch<TutorChatResponse>(
    `/api/learning-articles/${learningArticleId}/chat/sessions`,
    { method: "POST" },
  );
}

export function loadChatSession(
  learningArticleId: number,
  sessionId: number,
): Promise<TutorChatResponse> {
  return apiFetch<TutorChatResponse>(
    `/api/learning-articles/${learningArticleId}/chat/sessions/${sessionId}`,
  );
}

export function sendChatSessionMessage(
  learningArticleId: number,
  sessionId: number,
  request: TutorChatRequest,
): Promise<TutorChatResponse> {
  return apiFetch<TutorChatResponse>(
    `/api/learning-articles/${learningArticleId}/chat/sessions/${sessionId}/messages`,
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );
}

export function renameChatSession(
  learningArticleId: number,
  sessionId: number,
  title: string,
): Promise<ChatSessionSummary> {
  return apiFetch<ChatSessionSummary>(
    `/api/learning-articles/${learningArticleId}/chat/sessions/${sessionId}`,
    {
      method: "PATCH",
      body: JSON.stringify({ title }),
    },
  );
}

export function deleteChatSession(
  learningArticleId: number,
  sessionId: number,
): Promise<void> {
  return apiFetch<void>(
    `/api/learning-articles/${learningArticleId}/chat/sessions/${sessionId}`,
    { method: "DELETE" },
  );
}
