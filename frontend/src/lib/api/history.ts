import { apiFetch } from "./client";
import type { LearningHistoryItem } from "./types";

export function fetchLearningHistory(): Promise<LearningHistoryItem[]> {
  return apiFetch<LearningHistoryItem[]>("/api/learning-history");
}
