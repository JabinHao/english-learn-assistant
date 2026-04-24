"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { selectCandidate } from "@/lib/api/candidates";

export function SelectButton({ candidateId }: { candidateId: number }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSelect() {
    setPending(true);
    setError(null);
    try {
      const result = await selectCandidate(candidateId);
      router.push(`/learning/${result.learningArticleId}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Selection failed");
      setPending(false);
    }
  }

  return (
    <div>
      <Button onClick={handleSelect} disabled={pending} size="sm">
        {pending ? "Selecting..." : "Study this article"}
      </Button>
      {error && <p className="mt-1 text-xs text-destructive">{error}</p>}
    </div>
  );
}
