# Interactive Tutor Workspace Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first usable version of an embedded article-aware tutor workspace to the learning article page.

**Architecture:** Extend the existing tutor API with optional context metadata, teach the backend to build focused context when a paragraph is targeted, then add a client-side tutor panel to the article page that loads history, sends free-form prompts, and exposes paragraph-level explain actions. Keep persistence and existing tutor session behavior intact.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, React 19, Next.js 16, TypeScript, Vitest, Testing Library

---

## Chunk 1: Backend Context Extension

### Task 1: Focused tutor context

**Files:**
- Modify: `src/main/java/com/ailearn/service/tutor/ArticleTutorContextService.java`
- Test: `src/test/java/com/ailearn/service/tutor/ArticleTutorContextServiceTest.java`

- [ ] Add a failing test proving paragraph-scoped context includes the targeted paragraph and excludes unrelated paragraphs.
- [ ] Run the focused test and confirm it fails.
- [ ] Add a new overload that accepts an optional paragraph index and builds focused context when provided.
- [ ] Re-run the focused test and existing tutor context tests.

### Task 2: Richer chat request propagation

**Files:**
- Modify: `src/main/java/com/ailearn/api/chat/ChatRequest.java`
- Modify: `src/main/java/com/ailearn/controller/TutorChatController.java`
- Modify: `src/main/java/com/ailearn/service/tutor/TutorAgentService.java`
- Test: `src/test/java/com/ailearn/controller/TutorChatControllerTest.java`
- Test: `src/test/java/com/ailearn/service/tutor/TutorAgentServiceTest.java`

- [ ] Add failing tests showing paragraph metadata reaches the tutor service and changes context lookup.
- [ ] Run the focused backend tests and confirm they fail for the new behavior.
- [ ] Extend `ChatRequest` with optional `paragraphIndex`, `selectedText`, `mode`, and `intent`.
- [ ] Pass request metadata from controller to tutor service.
- [ ] Update tutor service to request focused context when a paragraph index is provided.
- [ ] Re-run the focused backend tests.

## Chunk 2: Frontend Tutor Workspace

### Task 3: Chat API client and types

**Files:**
- Modify: `frontend/src/lib/api/types.ts`
- Create: `frontend/src/lib/api/chat.ts`

- [ ] Add minimal chat request and response types.
- [ ] Add typed helpers for loading chat history and sending chat messages.

### Task 4: Tutor panel

**Files:**
- Create: `frontend/src/components/tutor/tutor-panel.tsx`
- Test: `frontend/src/components/tutor/tutor-panel.test.tsx`

- [ ] Add failing tests for empty state, history rendering, free-form send, and retryable error display.
- [ ] Run the focused frontend test and confirm it fails.
- [ ] Implement a client component that loads history, renders messages, sends prompts, and exposes quick actions.
- [ ] Re-run the focused frontend test.

### Task 5: Paragraph explain actions and page integration

**Files:**
- Modify: `frontend/src/components/learning/bilingual-paragraphs.tsx`
- Modify: `frontend/src/components/learning/bilingual-paragraphs.test.tsx`
- Modify: `frontend/src/app/learning/[id]/page.tsx`

- [ ] Add failing tests proving paragraph rows expose an `Explain` action.
- [ ] Run the focused frontend tests and confirm they fail.
- [ ] Add an optional paragraph action callback to `BilingualParagraphs`.
- [ ] Compose `BilingualParagraphs` and `TutorPanel` together on the learning page.
- [ ] Re-run the focused frontend tests.

## Chunk 3: Verification

### Task 6: Regression checks

**Files:**
- No new files

- [ ] Run focused backend tests for tutor controller, tutor service, and tutor context.
- [ ] Run frontend unit tests.
- [ ] Run frontend build.
- [ ] Review `git diff` and ensure unrelated local config changes remain untouched.
