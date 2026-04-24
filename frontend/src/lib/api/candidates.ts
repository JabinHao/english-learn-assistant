import { apiFetch } from "./client";
import type { CandidateArticle, SelectCandidateResponse } from "./types";

export function fetchTodayCandidates(): Promise<CandidateArticle[]> {
  return apiFetch<CandidateArticle[]>("/api/candidates/today");
}

export function selectCandidate(
  id: number,
): Promise<SelectCandidateResponse> {
  return apiFetch<SelectCandidateResponse>(
    `/api/candidates/${id}/select`,
    { method: "POST" },
  );
}
