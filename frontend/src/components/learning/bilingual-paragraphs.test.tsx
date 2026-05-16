import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import type { ArticleParagraph } from "@/lib/api/types";

const paragraphs: ArticleParagraph[] = [
  {
    id: 1,
    paragraphIndex: 0,
    englishText: "AI is transforming the world.",
    chineseText: "人工智能正在改变世界。",
  },
  {
    id: 2,
    paragraphIndex: 1,
    englishText: "New breakthroughs emerge daily.",
    chineseText: "每天都有新突破出现。",
  },
];

describe("BilingualParagraphs", () => {
  it("renders all English and Chinese paragraphs", () => {
    render(<BilingualParagraphs paragraphs={paragraphs} />);

    expect(
      screen.getByText("AI is transforming the world."),
    ).toBeInTheDocument();
    expect(screen.getByText("人工智能正在改变世界。")).toBeInTheDocument();
    expect(
      screen.getByText("New breakthroughs emerge daily."),
    ).toBeInTheDocument();
    expect(screen.getByText("每天都有新突破出现。")).toBeInTheDocument();
  });

  it("renders section heading", () => {
    render(<BilingualParagraphs paragraphs={paragraphs} />);
    expect(screen.getByText("Bilingual Reading")).toBeInTheDocument();
  });

  it("shows empty state when no paragraphs", () => {
    render(<BilingualParagraphs paragraphs={[]} />);
    expect(
      screen.getByText("No translated paragraphs are available yet."),
    ).toBeInTheDocument();
  });

  it("exposes an explain action for each paragraph when configured", () => {
    const onExplain = vi.fn();
    render(
      <BilingualParagraphs paragraphs={paragraphs} onExplainParagraph={onExplain} />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: "Explain" })[1]);

    expect(onExplain).toHaveBeenCalledWith(paragraphs[1]);
  });
});
