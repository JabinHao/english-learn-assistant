"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import type { LearningHistoryItem } from "@/lib/api/types";
import { fetchLearningHistory } from "@/lib/api/history";

export function HistoryList() {
  const [items, setItems] = useState<LearningHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchLearningHistory()
      .then(setItems)
      .catch((e) =>
        setError(e instanceof Error ? e.message : "Failed to load history"),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-20 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
        {error}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <p className="text-muted-foreground">
        No learning history yet. Select an article to get started.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <Link key={item.id} href={`/learning/${item.id}`}>
          <Card className="transition-colors hover:bg-muted/50">
            <CardHeader>
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <CardTitle className="text-base leading-snug">
                    {item.title}
                  </CardTitle>
                  <CardDescription className="flex items-center gap-2 text-xs">
                    <span>{item.source}</span>
                    <span>&middot;</span>
                    <span>
                      {new Date(item.publishedAt).toLocaleDateString()}
                    </span>
                    <span>&middot;</span>
                    <span>
                      Selected{" "}
                      {new Date(item.selectedAt).toLocaleDateString()}
                    </span>
                  </CardDescription>
                </div>
                <Badge variant="outline" className="shrink-0">
                  {item.status.replace(/_/g, " ")}
                </Badge>
              </div>
            </CardHeader>
          </Card>
        </Link>
      ))}
    </div>
  );
}
