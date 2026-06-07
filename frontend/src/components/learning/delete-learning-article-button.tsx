"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { deleteLearningArticle } from "@/lib/api/learning";

type DeleteLearningArticleButtonProps = {
  learningArticleId: number;
  redirectTo?: string;
};

export function DeleteLearningArticleButton({
  learningArticleId,
  redirectTo,
}: DeleteLearningArticleButtonProps) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleDelete() {
    const confirmed = window.confirm(
      "Delete this learning article? This also removes its paragraphs, vocabulary, and tutor chat history.",
    );
    if (!confirmed || pending) {
      return;
    }

    setPending(true);
    setError(null);
    try {
      await deleteLearningArticle(learningArticleId);
      if (redirectTo) {
        router.push(redirectTo);
      } else {
        router.refresh();
      }
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Delete failed");
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        type="button"
        size="sm"
        variant="outline"
        disabled={pending}
        onClick={() => void handleDelete()}
        aria-label="Delete article"
      >
        <Trash2 />
        {pending ? "Deleting..." : "Delete"}
      </Button>
      {error ? <p className="text-xs text-destructive">{error}</p> : null}
    </div>
  );
}
