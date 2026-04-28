 "use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { pushVocabularyItem, removeVocabularyItem } from "@/lib/api/learning";
import type { VocabularyItem } from "@/lib/api/types";
import { useState } from "react";

export function VocabularyList({
  learningArticleId,
  items,
}: {
  learningArticleId: number;
  items: VocabularyItem[];
}) {
  const [localItems, setLocalItems] = useState(items);
  const [pushingId, setPushingId] = useState<number | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);

  async function handlePush(itemId: number) {
    setPushingId(itemId);
    try {
      const updated = await pushVocabularyItem(learningArticleId, itemId);
      setLocalItems((current) =>
        current.map((item) => (item.id === itemId ? updated : item)),
      );
    } finally {
      setPushingId(null);
    }
  }

  async function handleRemove(itemId: number) {
    setRemovingId(itemId);
    try {
      const updated = await removeVocabularyItem(learningArticleId, itemId);
      setLocalItems((current) =>
        current.map((item) => (item.id === itemId ? updated : item)),
      );
    } finally {
      setRemovingId(null);
    }
  }

  if (localItems.length === 0) {
    return (
      <Card className="border border-dashed border-foreground/15">
        <CardHeader>
          <CardTitle className="text-lg">Vocabulary</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          No vocabulary items have been extracted yet.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border border-foreground/10">
      <CardHeader>
        <CardTitle className="text-lg">Vocabulary</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {localItems.map((item) => (
          <div
            key={item.id}
            className="rounded-2xl border border-foreground/10 bg-background/80 p-4"
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <div className="text-base font-semibold">{item.word}</div>
                  <Badge variant="secondary">{item.type}</Badge>
                  {item.eudicPushed ? (
                    <Badge variant="outline">Eudic synced</Badge>
                  ) : null}
                </div>
                {item.ipa ? (
                  <p className="font-mono text-xs text-muted-foreground">
                    {item.ipa}
                  </p>
                ) : (
                  <p className="text-xs text-muted-foreground">
                    IPA unavailable
                  </p>
                )}
              </div>
              <Button
                size="sm"
                variant="outline"
                disabled={pushingId === item.id || removingId === item.id}
                onClick={() =>
                  item.eudicPushed ? handleRemove(item.id) : handlePush(item.id)
                }
              >
                {pushingId === item.id
                  ? "Adding..."
                  : removingId === item.id
                    ? "Removing..."
                    : item.eudicPushed
                      ? "Remove from Eudic"
                      : "Add to Eudic"}
              </Button>
            </div>
            <div className="mt-2 space-y-1 text-sm">
              {item.chineseDefinition ? (
                <p className="text-foreground/90">{item.chineseDefinition}</p>
              ) : null}
              {item.englishDefinition ? (
                <p className="text-muted-foreground">{item.englishDefinition}</p>
              ) : null}
              {item.sourceSentence ? (
                <p className="rounded-xl bg-muted/50 px-3 py-2 text-xs leading-6 text-muted-foreground">
                  {item.sourceSentence}
                </p>
              ) : null}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
