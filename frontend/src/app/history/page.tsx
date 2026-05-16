import { HistoryList } from "@/components/history/history-list";
import { ApiError } from "@/lib/api/client";
import { fetchLearningHistory } from "@/lib/api/history";

export default async function HistoryPage() {
  let items;

  try {
    items = await fetchLearningHistory();
  } catch (error) {
    const isNotReady = error instanceof ApiError && error.status === 404;

    return (
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Learning History</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Review previously selected articles and reopen completed study
            sessions.
          </p>
        </div>
        <div className="rounded-3xl border border-foreground/10 bg-card/80 p-6 text-sm text-muted-foreground">
          {isNotReady
            ? "The learning history API is not available yet. This page is wired to the expected contract and will populate once the backend route is implemented."
            : "Failed to load history. Check that the backend is running."}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Learning History</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Review previously selected articles and reopen completed study
          sessions.
        </p>
      </div>
      <HistoryList items={items} />
    </div>
  );
}
