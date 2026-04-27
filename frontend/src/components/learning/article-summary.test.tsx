import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ArticleSummary } from "./article-summary";
import type { LearningArticle } from "@/lib/api/types";

const article: LearningArticle = {
  id: 10,
  candidateArticleId: 1,
  status: "TRANSLATED",
  title: "Test Article Title",
  url: "https://example.com/article",
  source: "BBC",
  publishedAt: "2026-04-22T00:00:00Z",
  summary: "This is the article summary.",
  selectedAt: "2026-04-22T08:00:00Z",
  paragraphs: [],
  vocabularyItems: [],
};

describe("ArticleSummary", () => {
  it("renders title, source, and summary", () => {
    render(<ArticleSummary article={article} />);

    expect(screen.getByText("Test Article Title")).toBeInTheDocument();
    expect(screen.getByText("BBC")).toBeInTheDocument();
    expect(
      screen.getByText("This is the article summary."),
    ).toBeInTheDocument();
  });

  it("renders status badge", () => {
    render(<ArticleSummary article={article} />);
    expect(screen.getByText("TRANSLATED")).toBeInTheDocument();
  });

  it("renders article metadata", () => {
    render(<ArticleSummary article={article} />);
    expect(screen.getByText("BBC")).toBeInTheDocument();
    expect(screen.getByText("TRANSLATED")).toBeInTheDocument();
  });

  it("hides summary when not provided", () => {
    render(
      <ArticleSummary article={{ ...article, summary: "" }} />,
    );
    expect(
      screen.queryByText("This is the article summary."),
    ).not.toBeInTheDocument();
  });
});
