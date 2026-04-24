"use client";

import { useEffect, useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import type { CandidateArticle } from "@/lib/api/types";
import { fetchTodayCandidates } from "@/lib/api/candidates";
import { CandidateCard } from "./candidate-card";

export function CandidateList() {
  const [candidates, setCandidates] = useState<CandidateArticle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchTodayCandidates()
      .then(setCandidates)
      .catch((e) =>
        setError(e instanceof Error ? e.message : "Failed to load candidates"),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-48 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
        {error}
      </div>
    );
  }

  if (candidates.length === 0) {
    return (
      <p className="text-muted-foreground">
        No candidates available today. Check back later.
      </p>
    );
  }

  return (
    <div className="space-y-4">
      {candidates.map((c) => (
        <CandidateCard key={c.id} candidate={c} />
      ))}
    </div>
  );
}
