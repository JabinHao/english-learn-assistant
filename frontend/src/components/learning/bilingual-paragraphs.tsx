import { Separator } from "@/components/ui/separator";
import type { ArticleParagraph } from "@/lib/api/types";

export function BilingualParagraphs({
  paragraphs,
}: {
  paragraphs: ArticleParagraph[];
}) {
  if (paragraphs.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Paragraphs are not available yet.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold">Bilingual Reading</h2>
      <div className="space-y-4">
        {paragraphs.map((p, i) => (
          <div key={p.id}>
            <div className="space-y-2">
              <p className="text-sm leading-relaxed">{p.englishText}</p>
              <p className="text-sm leading-relaxed text-muted-foreground">
                {p.chineseText}
              </p>
            </div>
            {i < paragraphs.length - 1 && <Separator className="mt-4" />}
          </div>
        ))}
      </div>
    </div>
  );
}
