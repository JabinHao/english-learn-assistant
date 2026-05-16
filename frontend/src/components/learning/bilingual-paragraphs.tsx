import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ArticleParagraph } from "@/lib/api/types";
import { Button } from "@/components/ui/button";

export function BilingualParagraphs({
  paragraphs,
  onExplainParagraph,
}: {
  paragraphs: ArticleParagraph[];
  onExplainParagraph?: (paragraph: ArticleParagraph) => void;
}) {
  if (paragraphs.length === 0) {
    return (
      <Card className="border border-dashed border-foreground/15">
        <CardHeader>
          <CardTitle className="text-lg">Bilingual Reading</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          No translated paragraphs are available yet.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border border-foreground/10">
      <CardHeader>
        <CardTitle className="text-lg">Bilingual Reading</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {paragraphs.map((paragraph, index) => (
          <div key={`${paragraph.paragraphIndex}-${index}`} className="space-y-3">
            <div className="flex items-center justify-between gap-3">
              <div className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Paragraph {paragraph.paragraphIndex}
              </div>
              {onExplainParagraph ? (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onExplainParagraph(paragraph)}
                >
                  Explain
                </Button>
              ) : null}
            </div>
            <div className="grid gap-3 lg:grid-cols-2">
              <div className="rounded-2xl bg-muted/40 p-4 text-sm leading-7">
                {paragraph.englishText}
              </div>
              <div className="rounded-2xl bg-primary/5 p-4 text-sm leading-7 text-foreground/90">
                {paragraph.chineseText || "Translation pending."}
              </div>
            </div>
            {index < paragraphs.length - 1 ? <Separator /> : null}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
