import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { LearningHistoryItem } from "@/lib/api/types";
import { formatDisplayDate } from "@/lib/date";

export function HistoryList({ items }: { items: LearningHistoryItem[] }) {
  if (items.length === 0) {
    return (
      <Card className="border border-dashed border-foreground/15">
        <CardContent className="p-6 text-sm text-muted-foreground">
          No learning records yet.
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="grid gap-4">
      {items.map((item) => (
        <Card key={item.id} className="border border-foreground/10">
          <CardHeader className="gap-2">
            <CardTitle className="text-lg">
              <Link
                href={`/learning/${item.id}`}
                className="transition-colors hover:text-primary"
              >
                {item.title}
              </Link>
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-4 text-sm text-muted-foreground">
            <span>{item.source}</span>
            <span>{formatDisplayDate(item.publishedAt)}</span>
            <span>{item.status}</span>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
