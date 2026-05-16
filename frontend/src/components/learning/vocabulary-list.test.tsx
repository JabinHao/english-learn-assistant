import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { VocabularyList } from "./vocabulary-list";
import type { VocabularyItem } from "@/lib/api/types";
import { pushVocabularyItem } from "@/lib/api/learning";

vi.mock("@/lib/api/learning", () => ({
  pushVocabularyItem: vi.fn(),
  removeVocabularyItem: vi.fn(),
}));

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
  afterEach(() => {
    vi.mocked(pushVocabularyItem).mockReset();
  });

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

  it("shows remove button for synced items", () => {
    render(<VocabularyList learningArticleId={88} items={items} />);
    expect(screen.getByText("Remove from Eudic")).toBeInTheDocument();
  });

  it("shows empty state when no items", () => {
    render(<VocabularyList learningArticleId={88} items={[]} />);
    expect(
      screen.getByText("No vocabulary items have been extracted yet."),
    ).toBeInTheDocument();
  });

  it("shows an inline error when Eudic sync fails", async () => {
    vi.mocked(pushVocabularyItem).mockRejectedValue(
      new Error("Failed to sync vocabulary item to Eudic"),
    );

    render(<VocabularyList learningArticleId={88} items={items} />);
    fireEvent.click(screen.getByText("Add to Eudic"));

    await waitFor(() => {
      expect(
        screen.getByText("Failed to sync vocabulary item to Eudic"),
      ).toBeInTheDocument();
    });
  });

  it("offers tutor follow-up for a vocabulary item", () => {
    const onAskTutor = vi.fn();
    render(
      <VocabularyList
        learningArticleId={88}
        items={items}
        onAskTutor={onAskTutor}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: "Ask Tutor" })[0]);

    expect(onAskTutor).toHaveBeenCalledWith(items[0]);
  });
});
