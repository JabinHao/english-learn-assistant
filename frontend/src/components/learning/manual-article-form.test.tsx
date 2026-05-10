import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ManualArticleForm } from "./manual-article-form";
import { createLearningArticleFromUrl } from "@/lib/api/learning";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("@/lib/api/learning", () => ({
  createLearningArticleFromUrl: vi.fn(),
}));

describe("ManualArticleForm", () => {
  it("submits an article URL and navigates to the learning page", async () => {
    vi.mocked(createLearningArticleFromUrl).mockResolvedValue({
      learningArticleId: 88,
      candidateArticleId: 7,
      status: "VOCAB_READY",
    });

    render(<ManualArticleForm />);

    fireEvent.change(screen.getByLabelText("Article URL"), {
      target: { value: "https://example.com/article" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start learning" }));

    await waitFor(() => {
      expect(createLearningArticleFromUrl).toHaveBeenCalledWith(
        "https://example.com/article",
      );
      expect(push).toHaveBeenCalledWith("/learning/88");
    });
  });
});
