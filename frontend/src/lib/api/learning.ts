import { apiFetch } from "./client";
import type { LearningArticle, VocabularyItem } from "./types";

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
