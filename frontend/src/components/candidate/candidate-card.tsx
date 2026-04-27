import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { CandidateArticle } from "@/lib/api/types";
import { SelectButton } from "./select-button";

export function CandidateCard({ candidate }: { candidate: CandidateArticle }) {
  return (
    <Card className="border border-foreground/10 bg-card/90 backdrop-blur-sm">
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <div className="space-y-1">
            <CardTitle className="text-base leading-snug">
              <a
                href={candidate.url}
                target="_blank"
                rel="noopener noreferrer"
                className="hover:underline"
              >
                {candidate.title}
              </a>
            </CardTitle>
            <CardDescription className="flex items-center gap-2 text-xs">
              <Badge variant="secondary">{candidate.source}</Badge>
              <span>
                {new Date(candidate.publishedAt).toLocaleDateString()}
              </span>
              {typeof candidate.score === "number" ? (
                <span className="rounded-full bg-primary/10 px-2 py-0.5 font-medium text-primary">
                  Score {candidate.score.toFixed(1)}
                </span>
              ) : null}
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        <p>{candidate.summary}</p>
        <p className="text-muted-foreground italic">
          {candidate.recommendationReason}
        </p>
      </CardContent>
      <CardFooter>
        {candidate.selected ? (
          <Badge variant="outline">Selected</Badge>
        ) : (
          <SelectButton candidateId={candidate.id} />
        )}
      </CardFooter>
    </Card>
  );
}
