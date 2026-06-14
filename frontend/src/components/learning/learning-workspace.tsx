"use client";

import type { CSSProperties } from "react";
import { useEffect, useState } from "react";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import { VocabularyList } from "./vocabulary-list";
import { TutorPanel } from "@/components/tutor/tutor-panel";
import { ArticleSummary } from "./article-summary";
import type { LearningArticle, TutorChatRequest } from "@/lib/api/types";
import {
  ChevronLeft,
  ChevronRight,
  GripVertical,
  Maximize2,
  Minimize2,
} from "lucide-react";

const REM_IN_PX = 16;
const COLLAPSED_WIDTH_REM = 4;
const VOCABULARY_DEFAULT_WIDTH_REM = 14;
const VOCABULARY_WIDE_WIDTH_REM = 22;
const TUTOR_DEFAULT_WIDTH_REM = 16;
const TUTOR_WIDE_WIDTH_REM = 28;
const MIN_SIDEBAR_WIDTH_REM = 12;
const MAX_SIDEBAR_WIDTH_REM = 34;

type ResizingSidebar = "vocabulary" | "tutor";

interface ResizeState {
  sidebar: ResizingSidebar;
  startX: number;
  startWidth: number;
}

export function LearningWorkspace({ article }: { article: LearningArticle }) {
  const [pendingRequest, setPendingRequest] = useState<TutorChatRequest | null>(
    null,
  );
  const [activePanel, setActivePanel] = useState<"reading" | "vocabulary" | "tutor">(
    "reading",
  );
  const [vocabularyExpanded, setVocabularyExpanded] = useState(true);
  const [tutorExpanded, setTutorExpanded] = useState(true);
  const [vocabularyWidth, setVocabularyWidth] = useState(
    VOCABULARY_DEFAULT_WIDTH_REM,
  );
  const [tutorWidth, setTutorWidth] = useState(TUTOR_DEFAULT_WIDTH_REM);
  const [resizing, setResizing] = useState<ResizeState | null>(null);

  useEffect(() => {
    if (!resizing) return;

    const handlePointerMove = (event: PointerEvent) => {
      const deltaRem = (event.clientX - resizing.startX) / REM_IN_PX;
      const nextWidth =
        resizing.sidebar === "vocabulary"
          ? resizing.startWidth + deltaRem
          : resizing.startWidth - deltaRem;
      const clampedWidth = clampSidebarWidth(nextWidth);
      if (resizing.sidebar === "vocabulary") {
        setVocabularyWidth(clampedWidth);
      } else {
        setTutorWidth(clampedWidth);
      }
    };
    const stopResizing = () => setResizing(null);

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", stopResizing);
    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", stopResizing);
    };
  }, [resizing]);

  const workspaceColumns = `${vocabularyExpanded ? `${vocabularyWidth}rem` : `${COLLAPSED_WIDTH_REM}rem`} minmax(0,1fr) ${tutorExpanded ? `${tutorWidth}rem` : `${COLLAPSED_WIDTH_REM}rem`}`;
  const workspaceStyle = {
    "--learning-columns": workspaceColumns,
  } as CSSProperties;

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
        data-testid="learning-workspace-grid"
        className="grid grid-cols-1 gap-4 md:grid-flow-row-dense md:grid-cols-[var(--learning-columns)] lg:gap-6"
        style={workspaceStyle}
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
                <div className="mb-2 hidden items-center justify-end gap-1 md:flex">
                  <button
                    type="button"
                    aria-label={
                      vocabularyWidth >= VOCABULARY_WIDE_WIDTH_REM
                        ? "Reset vocabulary width"
                        : "Expand vocabulary width"
                    }
                    className="flex size-7 items-center justify-center rounded-full border bg-background shadow-sm"
                    onClick={() =>
                      setVocabularyWidth((width) =>
                        width >= VOCABULARY_WIDE_WIDTH_REM
                          ? VOCABULARY_DEFAULT_WIDTH_REM
                          : VOCABULARY_WIDE_WIDTH_REM,
                      )
                    }
                  >
                    {vocabularyWidth >= VOCABULARY_WIDE_WIDTH_REM ? (
                      <Minimize2 className="size-3.5" />
                    ) : (
                      <Maximize2 className="size-3.5" />
                    )}
                  </button>
                </div>
                {vocabularyPanel}
                <div
                  role="separator"
                  aria-label="Resize vocabulary sidebar"
                  aria-orientation="vertical"
                  aria-valuemin={MIN_SIDEBAR_WIDTH_REM}
                  aria-valuemax={MAX_SIDEBAR_WIDTH_REM}
                  aria-valuenow={vocabularyWidth}
                  className="absolute -right-3 top-12 hidden h-28 w-4 cursor-col-resize items-center justify-center rounded-full text-muted-foreground hover:bg-muted md:flex"
                  onPointerDown={(event) => {
                    event.preventDefault();
                    setResizing({
                      sidebar: "vocabulary",
                      startX: event.clientX,
                      startWidth: vocabularyWidth,
                    });
                  }}
                >
                  <GripVertical className="size-4" />
                </div>
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
                <div className="mb-2 hidden items-center justify-start gap-1 md:flex">
                  <button
                    type="button"
                    aria-label={
                      tutorWidth >= TUTOR_WIDE_WIDTH_REM
                        ? "Reset tutor width"
                        : "Expand tutor width"
                    }
                    className="flex size-7 items-center justify-center rounded-full border bg-background shadow-sm"
                    onClick={() =>
                      setTutorWidth((width) =>
                        width >= TUTOR_WIDE_WIDTH_REM
                          ? TUTOR_DEFAULT_WIDTH_REM
                          : TUTOR_WIDE_WIDTH_REM,
                      )
                    }
                  >
                    {tutorWidth >= TUTOR_WIDE_WIDTH_REM ? (
                      <Minimize2 className="size-3.5" />
                    ) : (
                      <Maximize2 className="size-3.5" />
                    )}
                  </button>
                </div>
                {tutorPanel}
                <div
                  role="separator"
                  aria-label="Resize tutor sidebar"
                  aria-orientation="vertical"
                  aria-valuemin={MIN_SIDEBAR_WIDTH_REM}
                  aria-valuemax={MAX_SIDEBAR_WIDTH_REM}
                  aria-valuenow={tutorWidth}
                  className="absolute -left-3 top-12 hidden h-28 w-4 cursor-col-resize items-center justify-center rounded-full text-muted-foreground hover:bg-muted md:flex"
                  onPointerDown={(event) => {
                    event.preventDefault();
                    setResizing({
                      sidebar: "tutor",
                      startX: event.clientX,
                      startWidth: tutorWidth,
                    });
                  }}
                >
                  <GripVertical className="size-4" />
                </div>
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

function clampSidebarWidth(width: number) {
  return Math.round(
    Math.min(MAX_SIDEBAR_WIDTH_REM, Math.max(MIN_SIDEBAR_WIDTH_REM, width)),
  );
}
