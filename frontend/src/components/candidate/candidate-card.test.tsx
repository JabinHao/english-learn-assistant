import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CandidateCard } from "./candidate-card";
import type { CandidateArticle } from "@/lib/api/types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

const base: CandidateArticle = {
  id: 1,
  title: "AI Advances in 2026",
  url: "https://example.com/ai",
  source: "TechCrunch",
  publishedAt: "2026-04-20T00:00:00Z",
  summary: "A summary of AI progress.",
  llmScore: 0.9,
  llmReason: "Highly relevant for vocabulary building.",
  selected: false,
};

describe("CandidateCard", () => {
  it("renders title, source, summary, and recommendation", () => {
    render(<CandidateCard candidate={base} />);

    expect(screen.getByText("AI Advances in 2026")).toBeInTheDocument();
    expect(screen.getByText("TechCrunch")).toBeInTheDocument();
    expect(screen.getByText("A summary of AI progress.")).toBeInTheDocument();
    expect(
      screen.getByText("Highly relevant for vocabulary building."),
    ).toBeInTheDocument();
  });

  it("shows select button when not selected", () => {
    render(<CandidateCard candidate={base} />);
    expect(screen.getByText("Study this article")).toBeInTheDocument();
  });

  it("shows selected badge when already selected", () => {
    render(<CandidateCard candidate={{ ...base, selected: true }} />);
    expect(screen.getByText("Selected")).toBeInTheDocument();
    expect(screen.queryByText("Study this article")).not.toBeInTheDocument();
  });
});
