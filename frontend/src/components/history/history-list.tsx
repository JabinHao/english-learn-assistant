import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DeleteLearningArticleButton } from "@/components/learning/delete-learning-article-button";
import type { LearningHistoryItem } from "@/lib/api/types";
import { formatDisplayDate } from "@/lib/date";

export function HistoryList({ items }: { items: LearningHistoryItem[] }) {
  if (items.length === 0) {
    return (
      <Card className="border border-dashed border-foreground/15">
        <CardContent className="p-6 text-sm text-muted-foreground">
          No learning records yet.
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="grid gap-4">
      {items.map((item) => {
        const displayTitle = item.chineseTitle?.trim() || item.title;
        const showEnglishTitle = displayTitle !== item.title;
        const displaySummary = item.chineseSummary?.trim() || item.summary;

        return (
          <Card key={item.id} className="border border-foreground/10">
            <CardHeader className="gap-2">
              <CardTitle className="text-lg">
                <Link
                  href={`/learning/${item.id}`}
                  className="transition-colors hover:text-primary"
                >
                  {displayTitle}
                </Link>
              </CardTitle>
              {showEnglishTitle ? (
                <p className="line-clamp-1 text-sm text-muted-foreground">
                  {item.title}
                </p>
              ) : null}
              {displaySummary ? (
                <p className="line-clamp-2 text-sm text-muted-foreground">
                  {displaySummary}
                </p>
              ) : null}
            </CardHeader>
            <CardContent className="flex flex-wrap items-center justify-between gap-4 text-sm text-muted-foreground">
              <div className="flex flex-wrap gap-4">
                <span>{item.source}</span>
                <span>{formatDisplayDate(item.publishedAt)}</span>
                <span>{item.status}</span>
              </div>
              <DeleteLearningArticleButton learningArticleId={item.id} />
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
