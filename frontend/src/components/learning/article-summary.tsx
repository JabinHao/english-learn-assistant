import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { LearningArticle } from "@/lib/api/types";

const statusTone: Record<LearningArticle["status"], string> = {
  SELECTED: "bg-amber-100 text-amber-900",
  CONTENT_READY: "bg-sky-100 text-sky-900",
  TRANSLATED: "bg-indigo-100 text-indigo-900",
  VOCAB_READY: "bg-emerald-100 text-emerald-900",
  EUDIC_PUSHED: "bg-green-100 text-green-900",
  FAILED: "bg-rose-100 text-rose-900",
};

export function ArticleSummary({ article }: { article: LearningArticle }) {
  return (
    <Card className="overflow-hidden border border-foreground/10 bg-[linear-gradient(135deg,rgba(252,248,240,0.92),rgba(240,246,255,0.92))]">
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <Badge variant="secondary">{article.source}</Badge>
          <span>{new Date(article.publishedAt).toLocaleString()}</span>
          <span
            className={`rounded-full px-2 py-1 font-medium ${statusTone[article.status]}`}
          >
            {article.status}
          </span>
        </div>
        <CardTitle className="max-w-4xl text-2xl leading-tight md:text-3xl">
          {article.title}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        <p className="max-w-3xl text-muted-foreground">
          {article.summary || "This article is selected for study. Translation and vocabulary notes appear below once processing completes."}
        </p>
        {article.articleContent ? (
          <div className="rounded-2xl border border-foreground/10 bg-white/70 p-4">
            <p className="line-clamp-5 whitespace-pre-line text-[13px] text-foreground/80">
              {article.articleContent}
            </p>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
