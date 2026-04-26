import { HistoryList } from "@/components/history/history-list";

export default function HistoryPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">
          Learning History
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Articles you&apos;ve studied.
        </p>
      </div>
      <HistoryList />
    </div>
  );
}
