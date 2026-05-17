"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  Bot,
  Check,
  MessageSquareText,
  Pencil,
  Plus,
  PanelLeft,
  SendHorizontal,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  createChatSession,
  deleteChatSession,
  listChatSessions,
  loadChatSession,
  renameChatSession,
  sendChatSessionMessage,
} from "@/lib/api/chat";
import type {
  ChatMessage,
  ChatSessionSummary,
  TutorChatRequest,
} from "@/lib/api/types";

type TutorPanelProps = {
  learningArticleId: number;
  pendingRequest?: TutorChatRequest | null;
  onPendingRequestHandled?: () => void;
};

const quickActions: Array<{ label: string; request: TutorChatRequest }> = [
  {
    label: "Summarize this article",
    request: {
      message: "Summarize this article for an intermediate English learner.",
      mode: "ASK",
      intent: "FREEFORM",
    },
  },
  {
    label: "Explain current paragraph",
    request: {
      message: "Explain the paragraph I am reading in simpler English and Chinese.",
      mode: "ASK",
      intent: "EXPLAIN_PARAGRAPH",
    },
  },
];

export function TutorPanel({
  learningArticleId,
  pendingRequest,
  onPendingRequestHandled,
}: TutorPanelProps) {
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [lastFailedRequest, setLastFailedRequest] =
    useState<TutorChatRequest | null>(null);
  const [showSidebar, setShowSidebar] = useState(false);
  const [renamingSessionId, setRenamingSessionId] = useState<number | null>(null);
  const [renameDraft, setRenameDraft] = useState("");
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const activeSession = useMemo(
    () => sessions.find((session) => session.id === activeSessionId) ?? null,
    [sessions, activeSessionId],
  );

  useEffect(() => {
    let cancelled = false;

    async function loadSessions() {
      setLoadingSessions(true);
      try {
        const result = await listChatSessions(learningArticleId);
        if (cancelled) {
          return;
        }
        setSessions(result);
        if (result.length > 0) {
          setActiveSessionId(result[0].id);
        } else {
          setActiveSessionId(null);
          setMessages([]);
        }
      } catch {
        if (!cancelled) {
          setSessions([]);
          setActiveSessionId(null);
          setMessages([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingSessions(false);
        }
      }
    }

    void loadSessions();
    return () => {
      cancelled = true;
    };
  }, [learningArticleId]);

  useEffect(() => {
    if (activeSessionId == null) {
      setMessages([]);
      return;
    }

    let cancelled = false;
    setLoadingMessages(true);
    void (async () => {
      try {
        const response = await loadChatSession(learningArticleId, activeSessionId);
        if (!cancelled) {
          setMessages(response.messages);
        }
      } catch {
        if (!cancelled) {
          setMessages([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingMessages(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [learningArticleId, activeSessionId]);

  useEffect(() => {
    if (!pendingRequest) {
      return;
    }

    void handleSend(pendingRequest);
    onPendingRequestHandled?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingRequest]);

  useEffect(() => {
    if (!scrollRef.current) {
      return;
    }
    scrollRef.current.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, sending]);

  function autosizeTextarea() {
    const node = textareaRef.current;
    if (!node) {
      return;
    }
    node.style.height = "auto";
    node.style.height = `${Math.min(node.scrollHeight, 160)}px`;
  }

  async function ensureSession(): Promise<number> {
    if (activeSessionId != null) {
      return activeSessionId;
    }
    const created = await createChatSession(learningArticleId);
    const summary: ChatSessionSummary = {
      id: created.sessionId,
      learningArticleId: created.learningArticleId,
      title: null,
      createdAt: new Date().toISOString(),
      messageCount: 0,
      lastMessageAt: null,
    };
    setSessions((prev) => [summary, ...prev]);
    setActiveSessionId(created.sessionId);
    return created.sessionId;
  }

  async function refreshSessions() {
    try {
      const result = await listChatSessions(learningArticleId);
      setSessions(result);
    } catch {
      // keep existing sessions
    }
  }

  async function handleSend(request: TutorChatRequest) {
    setSending(true);
    setErrorMessage(null);
    setLastFailedRequest(null);
    try {
      const sessionId = await ensureSession();
      const response = await sendChatSessionMessage(
        learningArticleId,
        sessionId,
        request,
      );
      setMessages(response.messages);
      setInput("");
      requestAnimationFrame(autosizeTextarea);
      void refreshSessions();
    } catch (error) {
      setLastFailedRequest(request);
      setErrorMessage(error instanceof Error ? error.message : "Failed to send message");
    } finally {
      setSending(false);
    }
  }

  function handleSubmit() {
    const message = input.trim();
    if (!message || sending) {
      return;
    }

    void handleSend({
      message,
      mode: "ASK",
      intent: "FREEFORM",
    });
  }

  function handleQuizStart() {
    void handleSend({
      message: "Quiz me on this article. Ask one question at a time.",
      mode: "QUIZ",
      intent: "GENERATE_QUIZ",
    });
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      handleSubmit();
    }
  }

  async function handleNewChat() {
    setSending(false);
    setErrorMessage(null);
    setLastFailedRequest(null);
    try {
      const created = await createChatSession(learningArticleId);
      await refreshSessions();
      setActiveSessionId(created.sessionId);
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Failed to start new chat",
      );
    }
  }

  function startRename(session: ChatSessionSummary) {
    setRenamingSessionId(session.id);
    setRenameDraft(session.title ?? "");
  }

  function cancelRename() {
    setRenamingSessionId(null);
    setRenameDraft("");
  }

  async function commitRename(sessionId: number) {
    const next = renameDraft.trim();
    setRenamingSessionId(null);
    setRenameDraft("");
    try {
      await renameChatSession(learningArticleId, sessionId, next);
      await refreshSessions();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Failed to rename chat",
      );
    }
  }

  async function handleDelete(sessionId: number) {
    try {
      await deleteChatSession(learningArticleId, sessionId);
      const remaining = sessions.filter((session) => session.id !== sessionId);
      setSessions(remaining);
      if (activeSessionId === sessionId) {
        setActiveSessionId(remaining.length > 0 ? remaining[0].id : null);
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Failed to delete chat",
      );
    }
  }

  const sessionLabel = activeSession?.title?.trim() || "New chat";

  return (
    <div className="flex h-[calc(100vh-6.5rem)] overflow-hidden rounded-2xl border border-foreground/10 bg-card">
      {showSidebar ? (
        <aside className="flex w-56 shrink-0 flex-col border-r border-foreground/10 bg-muted/30">
          <div className="flex items-center justify-between gap-1 border-b border-foreground/10 px-2 py-2">
            <span className="px-1 text-xs font-medium text-muted-foreground">
              Chats
            </span>
            <Button
              size="icon-xs"
              variant="ghost"
              aria-label="New chat"
              onClick={() => void handleNewChat()}
            >
              <Plus />
            </Button>
          </div>
          <div className="flex-1 space-y-0.5 overflow-y-auto p-1.5">
            {loadingSessions ? (
              <p className="px-2 py-1 text-xs text-muted-foreground">Loading…</p>
            ) : sessions.length === 0 ? (
              <p className="px-2 py-1 text-xs text-muted-foreground">
                No chats yet
              </p>
            ) : (
              sessions.map((session) => {
                const isActive = session.id === activeSessionId;
                const isRenaming = renamingSessionId === session.id;
                const displayTitle = session.title?.trim() || "New chat";
                return (
                  <div
                    key={session.id}
                    className={
                      isActive
                        ? "group flex items-center gap-1 rounded-lg bg-background px-1.5 py-1.5 text-xs shadow-sm"
                        : "group flex items-center gap-1 rounded-lg px-1.5 py-1.5 text-xs hover:bg-background/60"
                    }
                  >
                    {isRenaming ? (
                      <>
                        <input
                          autoFocus
                          value={renameDraft}
                          onChange={(event) => setRenameDraft(event.target.value)}
                          onKeyDown={(event) => {
                            if (event.key === "Enter") {
                              event.preventDefault();
                              void commitRename(session.id);
                            } else if (event.key === "Escape") {
                              event.preventDefault();
                              cancelRename();
                            }
                          }}
                          className="min-w-0 flex-1 rounded border border-foreground/10 bg-background px-1.5 py-0.5 text-xs outline-none focus:border-primary"
                        />
                        <Button
                          size="icon-xs"
                          variant="ghost"
                          aria-label="Save"
                          onClick={() => void commitRename(session.id)}
                        >
                          <Check />
                        </Button>
                        <Button
                          size="icon-xs"
                          variant="ghost"
                          aria-label="Cancel rename"
                          onClick={cancelRename}
                        >
                          <X />
                        </Button>
                      </>
                    ) : (
                      <>
                        <button
                          type="button"
                          className="min-w-0 flex-1 truncate text-left"
                          aria-label={displayTitle}
                          onClick={() => setActiveSessionId(session.id)}
                        >
                          {displayTitle}
                        </button>
                        <Button
                          size="icon-xs"
                          variant="ghost"
                          aria-label="Rename chat"
                          className="opacity-0 group-hover:opacity-100"
                          onClick={() => startRename(session)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          size="icon-xs"
                          variant="ghost"
                          aria-label="Delete chat"
                          className="opacity-0 group-hover:opacity-100"
                          onClick={() => void handleDelete(session.id)}
                        >
                          <Trash2 />
                        </Button>
                      </>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </aside>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-2 border-b border-foreground/10 px-3 py-2.5">
          <div className="flex min-w-0 items-center gap-2">
            <Button
              size="icon-xs"
              variant="ghost"
              aria-label="Toggle chat list"
              aria-pressed={showSidebar}
              onClick={() => setShowSidebar((value) => !value)}
            >
              <PanelLeft />
            </Button>
            <MessageSquareText className="size-4 shrink-0" />
            <span className="truncate text-sm font-medium" title={sessionLabel}>
              {sessionLabel}
            </span>
          </div>
          <div className="flex gap-1">
            <Button
              size="xs"
              variant="outline"
              onClick={() => void handleNewChat()}
              aria-label="New chat"
            >
              <Plus />
              New
            </Button>
            <Button size="xs" variant="outline" disabled={sending} onClick={handleQuizStart}>
              Quiz
            </Button>
            <Button size="xs" variant="outline" disabled>
              Review
            </Button>
          </div>
        </header>

        <div
          ref={scrollRef}
          className="flex-1 space-y-3 overflow-y-auto px-3 py-3"
          aria-label="Chat messages"
        >
          {loadingMessages ? (
            <p className="text-sm text-muted-foreground">Loading messages...</p>
          ) : messages.length === 0 ? (
            <div className="space-y-3">
              <div className="flex items-start gap-2">
                <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Sparkles className="size-3.5" />
                </div>
                <div className="rounded-2xl rounded-tl-sm bg-muted/60 px-3 py-2 text-sm leading-6">
                  <p className="font-medium">Ask your tutor</p>
                  <p className="text-muted-foreground">
                    Ask about the article while you read.
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap gap-2 pl-9">
                {quickActions.map((action) => (
                  <Button
                    key={action.label}
                    size="sm"
                    variant="outline"
                    disabled={sending}
                    onClick={() => void handleSend(action.request)}
                  >
                    {action.label}
                  </Button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((message) => {
              const isUser = message.role !== "assistant";
              return (
                <div
                  key={message.id}
                  className={isUser ? "flex justify-end" : "flex items-start gap-2"}
                >
                  {!isUser ? (
                    <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                      <Bot className="size-3.5" />
                    </div>
                  ) : null}
                  <div
                    className={
                      isUser
                        ? "max-w-[85%] rounded-2xl rounded-tr-sm bg-primary px-3 py-2 text-sm leading-6 text-primary-foreground whitespace-pre-wrap"
                        : "max-w-[85%] rounded-2xl rounded-tl-sm bg-muted/60 px-3 py-2 text-sm leading-6 whitespace-pre-wrap"
                    }
                  >
                    {message.content}
                  </div>
                </div>
              );
            })
          )}

          {sending ? (
            <div className="flex items-start gap-2" aria-label="Tutor is typing">
              <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Bot className="size-3.5" />
              </div>
              <div className="rounded-2xl rounded-tl-sm bg-muted/60 px-3 py-2.5 text-sm leading-6">
                <span className="inline-flex gap-1">
                  <span className="size-1.5 animate-bounce rounded-full bg-foreground/40 [animation-delay:-0.3s]" />
                  <span className="size-1.5 animate-bounce rounded-full bg-foreground/40 [animation-delay:-0.15s]" />
                  <span className="size-1.5 animate-bounce rounded-full bg-foreground/40" />
                </span>
              </div>
            </div>
          ) : null}

          {errorMessage ? (
            <div className="space-y-2 rounded-2xl border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
              <p>{errorMessage}</p>
              {lastFailedRequest ? (
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={() => void handleSend(lastFailedRequest)}
                >
                  Retry
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>

        <div className="border-t border-foreground/10 bg-card p-2.5">
          <div className="relative">
            <textarea
              ref={textareaRef}
              rows={1}
              className="w-full resize-none rounded-2xl border border-foreground/10 bg-background py-2 pr-11 pl-3 text-sm leading-6 outline-none focus:border-primary"
              placeholder="Ask about this article..."
              value={input}
              onChange={(event) => {
                setInput(event.target.value);
                autosizeTextarea();
              }}
              onKeyDown={handleKeyDown}
            />
            <Button
              size="icon-sm"
              className="absolute right-1.5 bottom-1.5"
              disabled={sending || !input.trim()}
              onClick={handleSubmit}
              aria-label="Send"
            >
              <SendHorizontal />
            </Button>
          </div>
          <p className="mt-1 px-1 text-[10px] text-muted-foreground">
            Enter to send · Shift+Enter for newline
          </p>
        </div>
      </div>
    </div>
  );
}
