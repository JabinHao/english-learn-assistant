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
    <Card>
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
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        <p>{candidate.summary}</p>
        <p className="text-muted-foreground italic">
          {candidate.llmReason}
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
