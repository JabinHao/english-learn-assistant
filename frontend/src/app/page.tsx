import { CandidateList } from "@/components/candidate/candidate-list";
import { ManualArticleForm } from "@/components/learning/manual-article-form";

export default function Home() {
  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">
          Today&apos;s Candidates
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose one article to study today.
        </p>
      </div>
      <ManualArticleForm />
      <CandidateList />
    </div>
  );
}
