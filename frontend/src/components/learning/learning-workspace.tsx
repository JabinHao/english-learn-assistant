"use client";

import { useState } from "react";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import { VocabularyList } from "./vocabulary-list";
import { TutorPanel } from "@/components/tutor/tutor-panel";
import type { LearningArticle, TutorChatRequest } from "@/lib/api/types";
import { ChevronLeft, ChevronRight } from "lucide-react";

export function LearningWorkspace({ article }: { article: LearningArticle }) {
  const [pendingRequest, setPendingRequest] = useState<TutorChatRequest | null>(
    null,
  );
  const [activePanel, setActivePanel] = useState<"reading" | "vocabulary" | "tutor">(
    "reading",
  );
  const [vocabularyExpanded, setVocabularyExpanded] = useState(true);
  const [tutorExpanded, setTutorExpanded] = useState(true);

  const readingPanel = (
    <BilingualParagraphs
      paragraphs={article.paragraphs}
      onExplainParagraph={(paragraph) =>
        setPendingRequest({
          message: `Explain paragraph ${paragraph.paragraphIndex} in simpler English and Chinese.`,
          paragraphIndex: paragraph.paragraphIndex,
          mode: "ASK",
          intent: "EXPLAIN_PARAGRAPH",
        })
      }
      onAskTutor={(paragraph, selectedText) => {
        setPendingRequest({
          message: "Explain this selected text in context.",
          paragraphIndex: paragraph.paragraphIndex,
          selectedText,
          mode: "ASK",
          intent: "EXPLAIN_SELECTION",
        });
        setActivePanel("tutor");
      }}
    />
  );

  const vocabularyPanel = (
    <VocabularyList
      learningArticleId={article.id}
      items={article.vocabularyItems}
      onAskTutor={(item) => {
        setPendingRequest({
          message: `Explain how "${item.word}" is used in this article.`,
          selectedText: item.word,
          mode: "ASK",
          intent: "VOCABULARY_HELP",
        });
        setActivePanel("tutor");
      }}
    />
  );

  const tutorPanel = (
    <TutorPanel
      learningArticleId={article.id}
      pendingRequest={pendingRequest}
      onPendingRequestHandled={() => setPendingRequest(null)}
    />
  );

  return (
    <>
      <div className="space-y-4 xl:hidden">
        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            className={activePanel === "reading" ? "rounded-lg bg-primary px-3 py-2 text-sm text-primary-foreground" : "rounded-lg border px-3 py-2 text-sm"}
            onClick={() => setActivePanel("reading")}
          >
            Reading
          </button>
          <button
            type="button"
            className={activePanel === "vocabulary" ? "rounded-lg bg-primary px-3 py-2 text-sm text-primary-foreground" : "rounded-lg border px-3 py-2 text-sm"}
            onClick={() => setActivePanel("vocabulary")}
          >
            Vocabulary
          </button>
          <button
            type="button"
            className={activePanel === "tutor" ? "rounded-lg bg-primary px-3 py-2 text-sm text-primary-foreground" : "rounded-lg border px-3 py-2 text-sm"}
            onClick={() => setActivePanel("tutor")}
          >
            Tutor
          </button>
        </div>
      </div>

      <div className="relative">
        <div
          className={
            activePanel === "reading"
              ? "block xl:mx-auto xl:max-w-3xl"
              : "hidden xl:mx-auto xl:block xl:max-w-3xl"
          }
        >
          {readingPanel}
        </div>

        <aside
          aria-label="Vocabulary sidebar"
          className={
            activePanel === "vocabulary"
              ? "block"
              : vocabularyExpanded
                ? "hidden xl:fixed xl:bottom-6 xl:left-4 xl:top-20 xl:block xl:w-72 xl:overflow-y-auto"
                : "hidden xl:fixed xl:bottom-6 xl:left-4 xl:top-20 xl:block xl:w-16"
          }
        >
          {vocabularyExpanded ? (
            <div className="relative">
              {vocabularyPanel}
              <button
                type="button"
                aria-label="Collapse vocabulary"
                className="absolute -right-3 top-6 hidden size-6 items-center justify-center rounded-full border bg-background shadow-sm xl:flex"
                onClick={() => setVocabularyExpanded(false)}
              >
                <ChevronLeft className="size-4" />
              </button>
            </div>
          ) : (
            <div className="hidden h-full flex-col items-center gap-3 rounded-2xl border bg-background/95 px-2 py-4 shadow-sm xl:flex">
              <span className="[writing-mode:vertical-rl] rotate-180 text-xs font-medium text-muted-foreground">
                Vocabulary hidden
              </span>
              <button
                type="button"
                aria-label="Expand vocabulary"
                className="flex size-8 items-center justify-center rounded-full border bg-background shadow-sm"
                onClick={() => setVocabularyExpanded(true)}
              >
                <ChevronRight className="size-4" />
              </button>
            </div>
          )}
        </aside>

        <aside
          aria-label="Tutor sidebar"
          className={
            activePanel === "tutor"
              ? "block"
              : tutorExpanded
                ? "hidden xl:fixed xl:bottom-6 xl:right-4 xl:top-20 xl:block xl:w-80 xl:overflow-y-auto"
                : "hidden xl:fixed xl:bottom-6 xl:right-4 xl:top-20 xl:block xl:w-16"
          }
        >
          {tutorExpanded ? (
            <div className="relative">
              {tutorPanel}
              <button
                type="button"
                aria-label="Collapse tutor"
                className="absolute -left-3 top-6 hidden size-6 items-center justify-center rounded-full border bg-background shadow-sm xl:flex"
                onClick={() => setTutorExpanded(false)}
              >
                <ChevronRight className="size-4" />
              </button>
            </div>
          ) : (
            <div className="hidden h-full flex-col items-center gap-3 rounded-2xl border bg-background/95 px-2 py-4 shadow-sm xl:flex">
              <span className="[writing-mode:vertical-rl] rotate-180 text-xs font-medium text-muted-foreground">
                Tutor hidden
              </span>
              <button
                type="button"
                aria-label="Expand tutor"
                className="flex size-8 items-center justify-center rounded-full border bg-background shadow-sm"
                onClick={() => setTutorExpanded(true)}
              >
                <ChevronLeft className="size-4" />
              </button>
            </div>
          )}
        </aside>
      </div>
    </>
  );
}
