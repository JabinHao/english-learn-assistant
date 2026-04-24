import { apiFetch } from "./client";
import type { LearningArticle } from "./types";

export function fetchLearningArticle(id: number): Promise<LearningArticle> {
  return apiFetch<LearningArticle>(`/api/learning-articles/${id}`);
}
