"use client";

import { useEffect, useState } from "react";
import { MessageSquareText, SendHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { loadChatHistory, sendChatMessage } from "@/lib/api/chat";
import type { ChatMessage, TutorChatRequest } from "@/lib/api/types";

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
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [sending, setSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [lastFailedRequest, setLastFailedRequest] =
    useState<TutorChatRequest | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadHistory() {
      setLoadingHistory(true);
      try {
        const response = await loadChatHistory(learningArticleId);
        if (!cancelled) {
          setMessages(response.messages);
        }
      } catch {
        if (!cancelled) {
          setMessages([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingHistory(false);
        }
      }
    }

    void loadHistory();
    return () => {
      cancelled = true;
    };
  }, [learningArticleId]);

  useEffect(() => {
    if (!pendingRequest) {
      return;
    }

    void handleSend(pendingRequest);
    onPendingRequestHandled?.();
    // pendingRequest is intentionally treated as an edge-triggered action.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingRequest]);

  async function handleSend(request: TutorChatRequest) {
    setSending(true);
    setErrorMessage(null);
    setLastFailedRequest(null);
    try {
      const response = await sendChatMessage(learningArticleId, request);
      setMessages(response.messages);
      setInput("");
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

  return (
    <Card className="border border-foreground/10">
      <CardHeader className="space-y-3">
        <div className="flex items-center gap-2">
          <MessageSquareText className="size-4" />
          <CardTitle className="text-lg">Tutor</CardTitle>
        </div>
        <div className="flex gap-2">
          <Button size="sm">Ask</Button>
          <Button size="sm" variant="outline" disabled>
            Quiz
          </Button>
          <Button size="sm" variant="outline" disabled>
            Review
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {loadingHistory ? (
          <p className="text-sm text-muted-foreground">Loading chat history...</p>
        ) : messages.length === 0 ? (
          <div className="space-y-3">
            <div>
              <p className="font-medium">Ask your tutor</p>
              <p className="text-sm text-muted-foreground">
                Ask about the article while you read.
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
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
          <div className="space-y-3">
            {messages.map((message) => (
              <div
                key={message.id}
                className={
                  message.role === "assistant"
                    ? "rounded-2xl bg-muted/60 p-3 text-sm leading-6"
                    : "rounded-2xl bg-primary/10 p-3 text-sm leading-6"
                }
              >
                {message.content}
              </div>
            ))}
          </div>
        )}

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

        <div className="space-y-2">
          <textarea
            className="min-h-24 w-full resize-none rounded-2xl border border-foreground/10 bg-background px-3 py-2 text-sm outline-none focus:border-primary"
            placeholder="Ask about this article..."
            value={input}
            onChange={(event) => setInput(event.target.value)}
          />
          <div className="flex justify-end">
            <Button disabled={sending} onClick={handleSubmit}>
              <SendHorizontal />
              Send
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
