import { Badge } from "@/components/ui/badge";
import type { LearningArticle } from "@/lib/api/types";

export function ArticleSummary({ article }: { article: LearningArticle }) {
  return (
    <div className="space-y-3">
      <div className="flex items-start justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight leading-tight">
          {article.title}
        </h1>
        <Badge variant="outline" className="shrink-0">
          {article.status.replace(/_/g, " ")}
        </Badge>
      </div>
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>{article.source}</span>
        <span>&middot;</span>
        <span>{new Date(article.publishedAt).toLocaleDateString()}</span>
        <span>&middot;</span>
        <a
          href={article.url}
          target="_blank"
          rel="noopener noreferrer"
          className="underline hover:text-foreground"
        >
          Original
        </a>
      </div>
      {article.summary && (
        <p className="text-sm leading-relaxed">{article.summary}</p>
      )}
    </div>
  );
}
