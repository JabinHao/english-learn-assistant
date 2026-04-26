"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import type { LearningArticle } from "@/lib/api/types";
import { fetchLearningArticle } from "@/lib/api/learning";
import { ArticleSummary } from "@/components/learning/article-summary";
import { BilingualParagraphs } from "@/components/learning/bilingual-paragraphs";
import { VocabularyList } from "@/components/learning/vocabulary-list";

export default function LearningPage() {
  const params = useParams<{ id: string }>();
  const [article, setArticle] = useState<LearningArticle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const id = Number(params.id);
    if (Number.isNaN(id)) {
      setError("Invalid article ID.");
      setLoading(false);
      return;
    }

    fetchLearningArticle(id)
      .then(setArticle)
      .catch((e) =>
        setError(
          e instanceof Error ? e.message : "Failed to load learning article",
        ),
      )
      .finally(() => setLoading(false));
  }, [params.id]);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-64 w-full rounded-lg" />
        <Skeleton className="h-48 w-full rounded-lg" />
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

  if (!article) {
    return (
      <p className="text-muted-foreground">Article not found.</p>
    );
  }

  return (
    <div className="space-y-8">
      <ArticleSummary article={article} />
      <Separator />
      <BilingualParagraphs paragraphs={article.paragraphs} />
      <Separator />
      <VocabularyList items={article.vocabularyItems} />
    </div>
  );
}
