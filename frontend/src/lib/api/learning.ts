import { apiFetch } from "./client";
import type {
  CreateLearningArticleResponse,
  LearningArticle,
  VocabularyItem,
} from "./types";

export function createLearningArticleFromUrl(
  url: string,
): Promise<CreateLearningArticleResponse> {
  return apiFetch<CreateLearningArticleResponse>("/api/learning-articles", {
    method: "POST",
    body: JSON.stringify({ url }),
  });
}

export function fetchLearningArticle(id: number): Promise<LearningArticle> {
  return apiFetch<LearningArticle>(`/api/learning-articles/${id}`);
}

export function pushVocabularyItem(
  learningArticleId: number,
  vocabularyItemId: number,
): Promise<VocabularyItem> {
  return apiFetch<VocabularyItem>(
    `/api/learning-articles/${learningArticleId}/vocabulary/${vocabularyItemId}/push`,
    { method: "POST" },
  );
}

export function removeVocabularyItem(
  learningArticleId: number,
  vocabularyItemId: number,
): Promise<VocabularyItem> {
  return apiFetch<VocabularyItem>(
    `/api/learning-articles/${learningArticleId}/vocabulary/${vocabularyItemId}/push`,
    { method: "DELETE" },
  );
}
