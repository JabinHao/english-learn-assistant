import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { VocabularyList } from "./vocabulary-list";
import type { VocabularyItem } from "@/lib/api/types";

const items: VocabularyItem[] = [
  {
    id: 1,
    word: "breakthrough",
    lemma: "breakthrough",
    type: "WORD",
    ipa: "/ˈbreɪkˌθruː/",
    englishDefinition: "an important discovery or development",
    chineseDefinition: "突破",
    sourceSentence: "New breakthroughs emerge daily.",
    eudicPushed: true,
  },
  {
    id: 2,
    word: "cutting-edge",
    lemma: "cutting-edge",
    type: "EXPRESSION",
    ipa: "",
    englishDefinition: "very advanced; innovative",
    chineseDefinition: "前沿的",
    sourceSentence: "The cutting-edge technology was impressive.",
    eudicPushed: false,
  },
];

describe("VocabularyList", () => {
  it("renders all vocabulary items", () => {
    render(<VocabularyList learningArticleId={88} items={items} />);

    expect(screen.getByText("breakthrough")).toBeInTheDocument();
    expect(screen.getByText("/ˈbreɪkˌθruː/")).toBeInTheDocument();
    expect(
      screen.getByText("an important discovery or development"),
    ).toBeInTheDocument();
    expect(screen.getByText("突破")).toBeInTheDocument();

    expect(screen.getByText("cutting-edge")).toBeInTheDocument();
    expect(screen.getByText("very advanced; innovative")).toBeInTheDocument();
  });

  it("shows Eudic badge when pushed", () => {
    render(<VocabularyList learningArticleId={88} items={items} />);
    expect(screen.getByText("Eudic synced")).toBeInTheDocument();
  });

  it("shows type badges", () => {
    render(<VocabularyList learningArticleId={88} items={items} />);
    expect(screen.getByText("WORD")).toBeInTheDocument();
    expect(screen.getByText("EXPRESSION")).toBeInTheDocument();
  });

  it("shows manual push button for unsynced items", () => {
    render(<VocabularyList learningArticleId={88} items={items} />);
    expect(screen.getByText("Add to Eudic")).toBeInTheDocument();
  });

  it("shows empty state when no items", () => {
    render(<VocabularyList learningArticleId={88} items={[]} />);
    expect(
      screen.getByText("No vocabulary items have been extracted yet."),
    ).toBeInTheDocument();
  });
});
