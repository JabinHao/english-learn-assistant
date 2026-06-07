import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DeleteLearningArticleButton } from "./delete-learning-article-button";
import { deleteLearningArticle } from "@/lib/api/learning";

const push = vi.fn();
const refresh = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, refresh }),
}));

vi.mock("@/lib/api/learning", () => ({
  deleteLearningArticle: vi.fn(),
}));

describe("DeleteLearningArticleButton", () => {
  beforeEach(() => {
    push.mockReset();
    refresh.mockReset();
    vi.mocked(deleteLearningArticle).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("deletes an article and refreshes the current page", async () => {
    vi.mocked(deleteLearningArticle).mockResolvedValue(undefined);

    render(<DeleteLearningArticleButton learningArticleId={88} />);

    fireEvent.click(screen.getByRole("button", { name: "Delete article" }));

    await waitFor(() => {
      expect(deleteLearningArticle).toHaveBeenCalledWith(88);
      expect(refresh).toHaveBeenCalled();
    });
    expect(push).not.toHaveBeenCalled();
  });

  it("navigates after deleting when a redirect target is provided", async () => {
    vi.mocked(deleteLearningArticle).mockResolvedValue(undefined);

    render(
      <DeleteLearningArticleButton
        learningArticleId={88}
        redirectTo="/history"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Delete article" }));

    await waitFor(() => {
      expect(deleteLearningArticle).toHaveBeenCalledWith(88);
      expect(push).toHaveBeenCalledWith("/history");
    });
    expect(refresh).not.toHaveBeenCalled();
  });

  it("does not delete when confirmation is cancelled", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);

    render(<DeleteLearningArticleButton learningArticleId={88} />);

    fireEvent.click(screen.getByRole("button", { name: "Delete article" }));

    expect(deleteLearningArticle).not.toHaveBeenCalled();
  });
});
