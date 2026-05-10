"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { createLearningArticleFromUrl } from "@/lib/api/learning";

export function ManualArticleForm() {
  const router = useRouter();
  const [url, setUrl] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);

    try {
      const result = await createLearningArticleFromUrl(url.trim());
      router.push(`/learning/${result.learningArticleId}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to start learning");
      setPending(false);
    }
  }

  return (
    <form
      className="space-y-3 rounded-lg border border-foreground/10 bg-card/90 p-4"
      onSubmit={handleSubmit}
    >
      <label className="block text-sm font-medium" htmlFor="manual-article-url">
        Article URL
      </label>
      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          className="h-9 min-w-0 flex-1 rounded-md border border-input bg-background px-3 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          id="manual-article-url"
          name="url"
          onChange={(event) => setUrl(event.target.value)}
          placeholder="https://example.com/article"
          required
          type="url"
          value={url}
        />
        <Button disabled={pending || !url.trim()} type="submit">
          {pending ? "Starting..." : "Start learning"}
        </Button>
      </div>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
    </form>
  );
}
