import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LearningWorkspace } from "./learning-workspace";

vi.mock("@/components/tutor/tutor-panel", () => ({
  TutorPanel: () => <div>Tutor panel</div>,
}));

vi.mock("./vocabulary-list", () => ({
  VocabularyList: () => <div>Vocabulary panel</div>,
}));

describe("LearningWorkspace", () => {
  it("switches between mobile workspace tabs", () => {
    render(
      <LearningWorkspace
        article={{
          id: 88,
          candidateArticleId: 1,
          status: "VOCAB_READY",
          title: "Article",
          url: "https://example.com",
          source: "Example",
          publishedAt: "2026-05-16T10:00:00",
          articleContent: "Article body",
          summary: "Summary",
          paragraphs: [
            {
              paragraphIndex: 1,
              englishText: "Paragraph one.",
              chineseText: "第一段。",
            },
          ],
          vocabularyItems: [],
        }}
      />,
    );

    expect(screen.getByText("Paragraph one.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Vocabulary" }));
    expect(screen.getByText("Vocabulary panel")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Tutor" }));
    expect(screen.getByText("Tutor panel")).toBeInTheDocument();
  });
});
