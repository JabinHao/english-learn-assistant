"use client";

import { useState } from "react";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import { VocabularyList } from "./vocabulary-list";
import { TutorPanel } from "@/components/tutor/tutor-panel";
import type { LearningArticle, TutorChatRequest } from "@/lib/api/types";

export function LearningWorkspace({ article }: { article: LearningArticle }) {
  const [pendingRequest, setPendingRequest] = useState<TutorChatRequest | null>(
    null,
  );
  const [activePanel, setActivePanel] = useState<"reading" | "vocabulary" | "tutor">(
    "reading",
  );

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

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(280px,0.7fr)_minmax(320px,0.8fr)]">
        <div className={activePanel === "reading" ? "block" : "hidden xl:block"}>
          {readingPanel}
        </div>
        <div className={activePanel === "vocabulary" ? "block" : "hidden xl:block"}>
          {vocabularyPanel}
        </div>
        <div className={activePanel === "tutor" ? "block" : "hidden xl:block"}>
          {tutorPanel}
        </div>
      </div>
    </>
  );
}
