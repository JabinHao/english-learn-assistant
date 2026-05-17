# Static Learning Article Export Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a local static HTML archive for each learning article after translation and vocabulary extraction finish.

**Architecture:** Add a dedicated filesystem-only export service that receives already processed article data and writes a stable HTML artifact. Invoke it from the existing learning workflow after vocabulary persistence, while keeping export failures non-fatal.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, AssertJ

---

## Chunk 1: Export Service

### Task 1: Add HTML export behavior

**Files:**
- Create: `src/main/java/com/ailearn/service/learning/LearningArticleExportService.java`
- Create: `src/test/java/com/ailearn/service/learning/LearningArticleExportServiceTest.java`

- [ ] Write a failing test proving a stable HTML file is created under `data/exports/articles/`
- [ ] Run the test and confirm failure because the service does not exist yet
- [ ] Implement slug generation, HTML escaping, and file writing
- [ ] Run the export service test and confirm it passes
- [ ] Commit the export service

## Chunk 2: Workflow Integration

### Task 2: Invoke export after workflow completion

**Files:**
- Modify: `src/main/java/com/ailearn/service/learning/LearningWorkflowService.java`
- Modify: `src/test/java/com/ailearn/service/learning/LearningWorkflowServiceTest.java`

- [ ] Write a failing test proving successful workflows call the export service
- [ ] Write a failing test proving export failures do not change the final `VOCAB_READY` result
- [ ] Run the workflow tests and confirm they fail for the missing integration
- [ ] Inject and invoke `LearningArticleExportService` after vocabulary persistence
- [ ] Catch/log export exceptions without failing the workflow
- [ ] Run the workflow tests and confirm they pass
- [ ] Commit workflow integration

## Chunk 3: Verification

### Task 3: Verify the complete change

**Files:**
- No new files

- [ ] Run targeted tests for export and workflow behavior
- [ ] Run the full backend test suite
- [ ] Confirm git status only includes intended changes plus the pre-existing user-local files
