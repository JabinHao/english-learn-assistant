import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { VocabularyItem } from "@/lib/api/types";

export function VocabularyList({ items }: { items: VocabularyItem[] }) {
  if (items.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Vocabulary is not available yet.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold">Vocabulary</h2>
      <div className="grid gap-3 sm:grid-cols-2">
        {items.map((item) => (
          <Card key={item.id}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between gap-2">
                <CardTitle className="text-base">{item.word}</CardTitle>
                <div className="flex items-center gap-1.5">
                  {item.eudicPushed && (
                    <Badge variant="outline" className="text-xs">
                      Eudic
                    </Badge>
                  )}
                  <Badge variant="secondary" className="text-xs">
                    {item.type.toLowerCase()}
                  </Badge>
                </div>
              </div>
              {item.ipa && (
                <p className="text-xs text-muted-foreground">{item.ipa}</p>
              )}
            </CardHeader>
            <CardContent className="space-y-1.5 text-sm">
              <p>{item.englishDefinition}</p>
              <p className="text-muted-foreground">{item.chineseDefinition}</p>
              <p className="text-xs italic text-muted-foreground mt-2">
                &ldquo;{item.sourceSentence}&rdquo;
              </p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
