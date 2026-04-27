import { notFound } from "next/navigation";
import { ArticleSummary } from "@/components/learning/article-summary";
import { BilingualParagraphs } from "@/components/learning/bilingual-paragraphs";
import { VocabularyList } from "@/components/learning/vocabulary-list";
import { ApiError } from "@/lib/api/client";
import { fetchLearningArticle } from "@/lib/api/learning";

export default async function LearningArticlePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  let article;

  try {
    article = await fetchLearningArticle(Number(id));
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }

    return (
      <div className="rounded-3xl border border-destructive/30 bg-destructive/10 p-6 text-sm text-destructive">
        Failed to load this learning article. Check that the backend is
        running and the article has finished processing.
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <ArticleSummary article={article} />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.6fr)_minmax(320px,0.9fr)]">
        <BilingualParagraphs paragraphs={article.paragraphs} />
        <VocabularyList
          learningArticleId={article.id}
          items={article.vocabularyItems}
        />
      </div>
    </div>
  );
}
