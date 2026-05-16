import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LearningWorkspace } from "./learning-workspace";

const tutorPanelProps = vi.fn();

vi.mock("@/components/tutor/tutor-panel", () => ({
  TutorPanel: (props: unknown) => {
    tutorPanelProps(props);
    return <div>Tutor panel</div>;
  },
}));

vi.mock("./vocabulary-list", () => ({
  VocabularyList: ({
    onAskTutor,
  }: {
    onAskTutor?: (item: { word: string; sourceSentence: string | null }) => void;
  }) => (
    <button
      type="button"
      onClick={() =>
        onAskTutor?.({
          word: "breakthrough",
          sourceSentence: "New breakthroughs emerge daily.",
        })
      }
    >
      Vocabulary panel
    </button>
  ),
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

  it("routes vocabulary questions into the tutor panel", () => {
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
          paragraphs: [],
          vocabularyItems: [],
        }}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Vocabulary" }));
    fireEvent.click(screen.getByRole("button", { name: "Vocabulary panel" }));

    expect(tutorPanelProps).toHaveBeenLastCalledWith(
      expect.objectContaining({
        pendingRequest: expect.objectContaining({
          selectedText: "breakthrough",
          intent: "VOCABULARY_HELP",
        }),
      }),
    );
  });

  it("can collapse vocabulary and tutor panels for focused reading", () => {
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
          paragraphs: [],
          vocabularyItems: [],
        }}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Collapse vocabulary" }));
    fireEvent.click(screen.getByRole("button", { name: "Collapse tutor" }));

    expect(screen.getByRole("button", { name: "Expand vocabulary" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Expand tutor" })).toBeInTheDocument();
  });
});
