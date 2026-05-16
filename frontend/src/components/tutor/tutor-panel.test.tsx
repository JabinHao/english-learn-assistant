import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TutorPanel } from "./tutor-panel";

const loadChatHistory = vi.fn();
const sendChatMessage = vi.fn();

vi.mock("@/lib/api/chat", () => ({
  loadChatHistory: (...args: unknown[]) => loadChatHistory(...args),
  sendChatMessage: (...args: unknown[]) => sendChatMessage(...args),
}));

describe("TutorPanel", () => {
  beforeEach(() => {
    loadChatHistory.mockReset();
    sendChatMessage.mockReset();
  });

  it("renders quick actions when no history exists", async () => {
    loadChatHistory.mockRejectedValue(new Error("Chat session not found"));

    render(<TutorPanel learningArticleId={88} />);

    expect(await screen.findByText("Ask your tutor")).toBeInTheDocument();
    expect(screen.getByText("Summarize this article")).toBeInTheDocument();
    expect(screen.getByText("Explain current paragraph")).toBeInTheDocument();
  });

  it("renders loaded history", async () => {
    loadChatHistory.mockResolvedValue({
      sessionId: 1,
      learningArticleId: 88,
      reply: "这是回答",
      messages: [
        {
          id: 1,
          role: "user",
          content: "请总结",
          createdAt: "2026-05-16T10:00:00",
        },
        {
          id: 2,
          role: "assistant",
          content: "这是回答",
          createdAt: "2026-05-16T10:00:01",
        },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    expect(await screen.findByText("请总结")).toBeInTheDocument();
    expect(screen.getByText("这是回答")).toBeInTheDocument();
  });

  it("sends a free-form question", async () => {
    loadChatHistory.mockRejectedValue(new Error("Chat session not found"));
    sendChatMessage.mockResolvedValue({
      sessionId: 1,
      learningArticleId: 88,
      reply: "这是解释",
      messages: [
        {
          id: 1,
          role: "user",
          content: "解释这篇文章",
          createdAt: "2026-05-16T10:00:00",
        },
        {
          id: 2,
          role: "assistant",
          content: "这是解释",
          createdAt: "2026-05-16T10:00:01",
        },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    fireEvent.change(await screen.findByPlaceholderText("Ask about this article..."), {
      target: { value: "解释这篇文章" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    await waitFor(() => {
      expect(sendChatMessage).toHaveBeenCalledWith(88, {
        message: "解释这篇文章",
        mode: "ASK",
        intent: "FREEFORM",
      });
    });
    expect(await screen.findByText("这是解释")).toBeInTheDocument();
  });

  it("shows a retryable error when send fails", async () => {
    loadChatHistory.mockRejectedValue(new Error("Chat session not found"));
    sendChatMessage.mockRejectedValue(new Error("Backend timeout"));

    render(<TutorPanel learningArticleId={88} />);

    fireEvent.change(await screen.findByPlaceholderText("Ask about this article..."), {
      target: { value: "解释这篇文章" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(await screen.findByText("Backend timeout")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });
});
