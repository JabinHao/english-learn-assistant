# Frontend And Learning UI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the frontend app for candidate browsing, article study, and learning history using a stable API contract from the backend plans.

**Architecture:** Next.js provides a focused web UI for the study workflow. The frontend consumes backend APIs for candidates, selected articles, and history, and leaves tutoring logic to the backend chat endpoint.

**Tech Stack:** Next.js, React, TypeScript, Tailwind CSS, shadcn/ui, TanStack Query or SWR, Vitest or Playwright as appropriate

---

## Chunk 1: Frontend Foundation

### Task 1: Scaffold frontend workspace

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/next.config.*`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/app/layout.tsx`
- Create: `frontend/src/app/globals.css`

- [ ] Step 1: Create Next.js workspace with TypeScript and Tailwind.
- [ ] Step 2: Add base design tokens and app shell.
- [ ] Step 3: Run frontend build.
- [ ] Step 4: Commit with `feat: scaffold frontend app`.

### Task 2: Add API client layer

**Files:**
- Create: `frontend/src/lib/api/client.ts`
- Create: `frontend/src/lib/api/types.ts`
- Create: `frontend/src/lib/api/candidates.ts`
- Create: `frontend/src/lib/api/learning.ts`
- Create: `frontend/src/lib/api/history.ts`

- [ ] Step 1: Write typed contracts matching backend DTOs.
- [ ] Step 2: Implement fetch wrappers and error handling.
- [ ] Step 3: Verify typecheck passes.
- [ ] Step 4: Commit with `feat: add frontend api clients`.

## Chunk 2: Core Pages

### Task 3: Build today candidate page

**Files:**
- Create: `frontend/src/app/page.tsx`
- Create: `frontend/src/components/candidate/candidate-list.tsx`
- Create: `frontend/src/components/candidate/candidate-card.tsx`
- Create: `frontend/src/components/candidate/select-button.tsx`

- [ ] Step 1: Implement page for `GET /api/candidates/today`.
- [ ] Step 2: Add candidate card with title, source, summary, and recommendation reason.
- [ ] Step 3: Wire select action to `POST /api/candidates/{id}/select`.
- [ ] Step 4: Verify page renders against mock data.
- [ ] Step 5: Commit with `feat: add candidate selection page`.

### Task 4: Build learning page

**Files:**
- Create: `frontend/src/app/learning/[id]/page.tsx`
- Create: `frontend/src/components/learning/article-summary.tsx`
- Create: `frontend/src/components/learning/bilingual-paragraphs.tsx`
- Create: `frontend/src/components/learning/vocabulary-list.tsx`

- [ ] Step 1: Implement page for `GET /api/learning-articles/{id}`.
- [ ] Step 2: Render summary, bilingual paragraphs, and vocabulary.
- [ ] Step 3: Add loading, empty, and failure states.
- [ ] Step 4: Verify page renders against mock data.
- [ ] Step 5: Commit with `feat: add learning page`.

### Task 5: Build learning history page

**Files:**
- Create: `frontend/src/app/history/page.tsx`
- Create: `frontend/src/components/history/history-list.tsx`

- [ ] Step 1: Implement history page.
- [ ] Step 2: Add cards or rows for past selected articles.
- [ ] Step 3: Verify route renders.
- [ ] Step 4: Commit with `feat: add learning history page`.

## Chunk 3: Quality And Integration

### Task 6: Add UI tests

**Files:**
- Create: `frontend/src/components/candidate/*.test.tsx`
- Create: `frontend/src/components/learning/*.test.tsx`

- [ ] Step 1: Add component tests for candidate list and learning view.
- [ ] Step 2: Run frontend test suite.
- [ ] Step 3: Commit with `test: add frontend component coverage`.

### Task 7: Add end-to-end smoke flow

**Files:**
- Create: `frontend/e2e/candidate-to-learning.spec.ts`

- [ ] Step 1: Add smoke test for candidate selection to learning page transition.
- [ ] Step 2: Run frontend build and smoke test.
- [ ] Step 3: Commit with `test: add frontend study flow smoke test`.

## File Ownership Notes

This plan owns:

- `frontend/` workspace,
- pages and components,
- frontend tests.

It must not implement:

- backend persistence,
- tutor agent logic,
- schema changes unless explicitly requested through protocol.
