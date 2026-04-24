import { CandidateList } from "@/components/candidate/candidate-list";

export default function Home() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">
          Today&apos;s Candidates
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose one article to study today.
        </p>
      </div>
      <CandidateList />
    </div>
  );
}
