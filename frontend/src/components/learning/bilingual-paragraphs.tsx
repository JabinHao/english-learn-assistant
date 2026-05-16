import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ArticleParagraph } from "@/lib/api/types";
import { Button } from "@/components/ui/button";
import { useState } from "react";

export function BilingualParagraphs({
  paragraphs,
  onExplainParagraph,
  onAskTutor,
}: {
  paragraphs: ArticleParagraph[];
  onExplainParagraph?: (paragraph: ArticleParagraph) => void;
  onAskTutor?: (paragraph: ArticleParagraph, selectedText: string) => void;
}) {
  const [selection, setSelection] = useState<{
    paragraph: ArticleParagraph;
    text: string;
  } | null>(null);

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
              <div
                className="rounded-2xl bg-muted/40 p-4 text-sm leading-7"
                onMouseUp={() => {
                  const text = window.getSelection()?.toString().trim();
                  if (text && onAskTutor) {
                    setSelection({ paragraph, text });
                  }
                }}
              >
                {paragraph.englishText}
              </div>
              <div className="rounded-2xl bg-primary/5 p-4 text-sm leading-7 text-foreground/90">
                {paragraph.chineseText || "Translation pending."}
              </div>
            </div>
            {selection?.paragraph.paragraphIndex === paragraph.paragraphIndex ? (
              <div className="flex justify-end">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    onAskTutor?.(selection.paragraph, selection.text);
                    setSelection(null);
                  }}
                >
                  Ask Tutor
                </Button>
              </div>
            ) : null}
            {index < paragraphs.length - 1 ? <Separator /> : null}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
