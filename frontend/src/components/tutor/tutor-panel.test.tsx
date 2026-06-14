import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TutorPanel } from "./tutor-panel";

const listChatSessions = vi.fn();
const createChatSession = vi.fn();
const loadChatSession = vi.fn();
const sendChatSessionMessage = vi.fn();
const renameChatSession = vi.fn();
const deleteChatSession = vi.fn();

vi.mock("@/lib/api/chat", () => ({
  listChatSessions: (...args: unknown[]) => listChatSessions(...args),
  createChatSession: (...args: unknown[]) => createChatSession(...args),
  loadChatSession: (...args: unknown[]) => loadChatSession(...args),
  sendChatSessionMessage: (...args: unknown[]) => sendChatSessionMessage(...args),
  renameChatSession: (...args: unknown[]) => renameChatSession(...args),
  deleteChatSession: (...args: unknown[]) => deleteChatSession(...args),
}));

describe("TutorPanel", () => {
  beforeEach(() => {
    listChatSessions.mockReset();
    createChatSession.mockReset();
    loadChatSession.mockReset();
    sendChatSessionMessage.mockReset();
    renameChatSession.mockReset();
    deleteChatSession.mockReset();
  });

  it("renders quick actions when no sessions exist", async () => {
    listChatSessions.mockResolvedValue([]);

    render(<TutorPanel learningArticleId={88} />);

    expect(await screen.findByText("Ask your tutor")).toBeInTheDocument();
    expect(screen.getByText("Summarize this article")).toBeInTheDocument();
    expect(screen.getByText("Explain current paragraph")).toBeInTheDocument();
  });

  it("loads the latest session and its messages on mount", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: "Vocabulary deep-dive",
        createdAt: "2026-05-16T10:00:00",
        messageCount: 2,
        lastMessageAt: "2026-05-16T10:01:00",
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "这是回答",
      messages: [
        { id: 1, role: "user", content: "请总结", createdAt: "2026-05-16T10:00:00" },
        { id: 2, role: "assistant", content: "这是回答", createdAt: "2026-05-16T10:00:01" },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    expect(await screen.findByText("请总结")).toBeInTheDocument();
    expect(screen.getByText("这是回答")).toBeInTheDocument();
    await waitFor(() => {
      expect(loadChatSession).toHaveBeenCalledWith(88, 12);
    });
  });

  it("renders assistant markdown while keeping user text plain", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: "Markdown chat",
        createdAt: "2026-05-16T10:00:00",
        messageCount: 2,
        lastMessageAt: "2026-05-16T10:01:00",
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "",
      messages: [
        {
          id: 1,
          role: "user",
          content: "**do not render**",
          createdAt: "2026-05-16T10:00:00",
        },
        {
          id: 2,
          role: "assistant",
          content: "**Key point**\n\n- first item\n- second item\n\nUse `latency` carefully.",
          createdAt: "2026-05-16T10:00:01",
        },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    const userText = await screen.findByText("**do not render**");
    expect(userText.tagName).toBe("DIV");

    const boldText = await screen.findByText("Key point");
    expect(boldText.tagName).toBe("STRONG");
    expect(screen.getByRole("list")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText("latency").tagName).toBe("CODE");
  });

  it("sends free-form questions to the active session", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: null,
        createdAt: "2026-05-16T10:00:00",
        messageCount: 0,
        lastMessageAt: null,
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });
    sendChatSessionMessage.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "这是解释",
      messages: [
        { id: 1, role: "user", content: "解释这篇文章", createdAt: "2026-05-16T10:00:00" },
        { id: 2, role: "assistant", content: "这是解释", createdAt: "2026-05-16T10:00:01" },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    const textarea = await screen.findByPlaceholderText("Ask about this article...");
    fireEvent.change(textarea, { target: { value: "解释这篇文章" } });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    await waitFor(() => {
      expect(sendChatSessionMessage).toHaveBeenCalledWith(88, 12, {
        message: "解释这篇文章",
        mode: "ASK",
        intent: "FREEFORM",
      });
    });
    expect(await screen.findByText("这是解释")).toBeInTheDocument();
  });

  it("creates a session on demand when none exist before first send", async () => {
    listChatSessions.mockResolvedValue([]);
    createChatSession.mockResolvedValue({
      sessionId: 99,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });
    sendChatSessionMessage.mockResolvedValue({
      sessionId: 99,
      learningArticleId: 88,
      reply: "ok",
      messages: [
        { id: 1, role: "user", content: "hi", createdAt: "2026-05-16T10:00:00" },
        { id: 2, role: "assistant", content: "ok", createdAt: "2026-05-16T10:00:01" },
      ],
    });

    render(<TutorPanel learningArticleId={88} />);

    const textarea = await screen.findByPlaceholderText("Ask about this article...");
    fireEvent.change(textarea, { target: { value: "hi" } });
    fireEvent.keyDown(textarea, { key: "Enter" });

    await waitFor(() => {
      expect(createChatSession).toHaveBeenCalledWith(88);
      expect(sendChatSessionMessage).toHaveBeenCalledWith(88, 99, {
        message: "hi",
        mode: "ASK",
        intent: "FREEFORM",
      });
    });
  });

  it("sends on Enter and not on Shift+Enter", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: null,
        createdAt: "2026-05-16T10:00:00",
        messageCount: 0,
        lastMessageAt: null,
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });
    sendChatSessionMessage.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "ok",
      messages: [],
    });

    render(<TutorPanel learningArticleId={88} />);
    const textarea = await screen.findByPlaceholderText("Ask about this article...");

    fireEvent.change(textarea, { target: { value: "hello" } });
    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: true });
    expect(sendChatSessionMessage).not.toHaveBeenCalled();

    fireEvent.keyDown(textarea, { key: "Enter" });
    await waitFor(() => {
      expect(sendChatSessionMessage).toHaveBeenCalledWith(88, 12, {
        message: "hello",
        mode: "ASK",
        intent: "FREEFORM",
      });
    });
  });

  it("creates a new session from the header button", async () => {
    listChatSessions.mockResolvedValueOnce([]).mockResolvedValueOnce([
      {
        id: 100,
        learningArticleId: 88,
        title: null,
        createdAt: "2026-05-16T11:00:00",
        messageCount: 0,
        lastMessageAt: null,
      },
    ]);
    createChatSession.mockResolvedValue({
      sessionId: 100,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });

    render(<TutorPanel learningArticleId={88} />);

    await screen.findByText("Ask your tutor");
    fireEvent.click(screen.getByRole("button", { name: "New chat" }));

    await waitFor(() => {
      expect(createChatSession).toHaveBeenCalledWith(88);
    });
  });

  it("switches between sessions on click", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: "First chat",
        createdAt: "2026-05-16T10:00:00",
        messageCount: 1,
        lastMessageAt: "2026-05-16T10:01:00",
      },
      {
        id: 11,
        learningArticleId: 88,
        title: "Second chat",
        createdAt: "2026-05-15T10:00:00",
        messageCount: 2,
        lastMessageAt: "2026-05-15T10:01:00",
      },
    ]);
    loadChatSession.mockResolvedValueOnce({
      sessionId: 12,
      learningArticleId: 88,
      reply: "first",
      messages: [{ id: 1, role: "assistant", content: "from first", createdAt: "2026-05-16T10:00:01" }],
    });
    loadChatSession.mockResolvedValueOnce({
      sessionId: 11,
      learningArticleId: 88,
      reply: "second",
      messages: [{ id: 2, role: "assistant", content: "from second", createdAt: "2026-05-15T10:00:01" }],
    });

    render(<TutorPanel learningArticleId={88} />);

    expect(await screen.findByText("from first")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Toggle chat list" }));
    fireEvent.click(screen.getByRole("button", { name: "Second chat" }));

    expect(await screen.findByText("from second")).toBeInTheDocument();
    await waitFor(() => {
      expect(loadChatSession).toHaveBeenLastCalledWith(88, 11);
    });
  });

  it("shows a retryable error when send fails", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: null,
        createdAt: "2026-05-16T10:00:00",
        messageCount: 0,
        lastMessageAt: null,
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });
    sendChatSessionMessage.mockRejectedValue(new Error("Backend timeout"));

    render(<TutorPanel learningArticleId={88} />);

    fireEvent.change(await screen.findByPlaceholderText("Ask about this article..."), {
      target: { value: "解释这篇文章" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(await screen.findByText("Backend timeout")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("starts a quiz from the quiz button", async () => {
    listChatSessions.mockResolvedValue([
      {
        id: 12,
        learningArticleId: 88,
        title: null,
        createdAt: "2026-05-16T10:00:00",
        messageCount: 0,
        lastMessageAt: null,
      },
    ]);
    loadChatSession.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "",
      messages: [],
    });
    sendChatSessionMessage.mockResolvedValue({
      sessionId: 12,
      learningArticleId: 88,
      reply: "Question 1",
      messages: [],
    });

    render(<TutorPanel learningArticleId={88} />);

    fireEvent.click(await screen.findByRole("button", { name: "Quiz" }));

    await waitFor(() => {
      expect(sendChatSessionMessage).toHaveBeenCalledWith(88, 12, {
        message: "Quiz me on this article. Ask one question at a time.",
        mode: "QUIZ",
        intent: "GENERATE_QUIZ",
      });
    });
  });
});
