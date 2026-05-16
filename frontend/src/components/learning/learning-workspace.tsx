"use client";

import { useState } from "react";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import { VocabularyList } from "./vocabulary-list";
import { TutorPanel } from "@/components/tutor/tutor-panel";
import { ArticleSummary } from "./article-summary";
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
      <div className="space-y-4 md:hidden">
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

      <div
        className={
          vocabularyExpanded && tutorExpanded
            ? "grid gap-4 md:grid-flow-row-dense md:grid-cols-[14rem_minmax(0,1fr)_16rem] lg:gap-6"
            : vocabularyExpanded
              ? "grid gap-4 md:grid-flow-row-dense md:grid-cols-[14rem_minmax(0,1fr)_4rem] lg:gap-6"
              : tutorExpanded
                ? "grid gap-4 md:grid-flow-row-dense md:grid-cols-[4rem_minmax(0,1fr)_16rem] lg:gap-6"
                : "grid gap-4 md:grid-flow-row-dense md:grid-cols-[4rem_minmax(0,1fr)_4rem] lg:gap-6"
        }
      >
        <div
          className={
            activePanel === "reading"
              ? "block space-y-8 md:col-start-2 md:mx-auto md:w-full md:max-w-3xl"
              : "hidden space-y-8 md:col-start-2 md:mx-auto md:block md:w-full md:max-w-3xl"
          }
        >
          <ArticleSummary article={article} />
          {readingPanel}
        </div>

        <aside
          aria-label="Vocabulary sidebar"
          className={
            activePanel === "vocabulary"
              ? "block"
              : vocabularyExpanded
                ? "hidden md:col-start-1 md:block"
                : "hidden md:col-start-1 md:block"
          }
        >
          {vocabularyExpanded ? (
            <div className="sticky top-20 max-h-[calc(100vh-6.5rem)] overflow-y-auto">
              <div className="relative">
                {vocabularyPanel}
                <button
                  type="button"
                  aria-label="Collapse vocabulary"
                  className="absolute -right-3 top-6 hidden size-6 items-center justify-center rounded-full border bg-background shadow-sm md:flex"
                  onClick={() => setVocabularyExpanded(false)}
                >
                  <ChevronLeft className="size-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="sticky top-20 hidden h-[calc(100vh-6.5rem)] flex-col items-center gap-3 rounded-2xl border bg-background/95 px-2 py-4 shadow-sm md:flex">
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
                ? "hidden md:col-start-3 md:block"
                : "hidden md:col-start-3 md:block"
          }
        >
          {tutorExpanded ? (
            <div className="sticky top-20 max-h-[calc(100vh-6.5rem)] overflow-y-auto">
              <div className="relative">
                {tutorPanel}
                <button
                  type="button"
                  aria-label="Collapse tutor"
                  className="absolute -left-3 top-6 hidden size-6 items-center justify-center rounded-full border bg-background shadow-sm md:flex"
                  onClick={() => setTutorExpanded(false)}
                >
                  <ChevronRight className="size-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="sticky top-20 hidden h-[calc(100vh-6.5rem)] flex-col items-center gap-3 rounded-2xl border bg-background/95 px-2 py-4 shadow-sm md:flex">
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
