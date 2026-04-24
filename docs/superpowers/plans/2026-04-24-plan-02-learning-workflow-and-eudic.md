# Learning Workflow And Eudic Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the selected-article learning workflow: selection, content fetch, translation, paragraph persistence, vocabulary extraction, and Eudic push.

**Architecture:** This plan extends the deterministic backend after candidate generation exists. It processes only the selected article, persists article learning artifacts, and records Eudic push status without becoming the system of record for long-term vocabulary mastery.

**Tech Stack:** Java 21, Spring Boot 3, PostgreSQL 16, Flyway, Jsoup, LangChain4j, OpenAI, JUnit 5, Testcontainers

---

## Chunk 1: Schema And Selection Flow

### Task 1: Add learning workflow schema

**Files:**
- Modify: `src/main/resources/db/migration/`
- Create: `src/main/resources/db/migration/V2__learning_workflow.sql`
- Create: `src/main/java/com/ailearn/entity/LearningArticleEntity.java`
- Create: `src/main/java/com/ailearn/entity/ArticleParagraphEntity.java`
- Create: `src/main/java/com/ailearn/entity/VocabularyItemEntity.java`
- Create: `src/main/java/com/ailearn/repository/LearningArticleRepository.java`
- Create: `src/main/java/com/ailearn/repository/ArticleParagraphRepository.java`
- Create: `src/main/java/com/ailearn/repository/VocabularyItemRepository.java`

- [ ] Step 1: Write migration for selected article, bilingual paragraph, and vocabulary tables.
- [ ] Step 2: Implement entities and repositories.
- [ ] Step 3: Verify Flyway migration applies.
- [ ] Step 4: Commit with `feat: add learning workflow schema`.

### Task 2: Add candidate selection API

**Files:**
- Create: `src/main/java/com/ailearn/service/learning/CandidateSelectionService.java`
- Create: `src/main/java/com/ailearn/api/learning/SelectCandidateResponse.java`
- Modify: `src/main/java/com/ailearn/controller/CandidateController.java`
- Create: `src/test/java/com/ailearn/controller/CandidateSelectionControllerTest.java`

- [ ] Step 1: Write failing test for `POST /api/candidates/{id}/select`.
- [ ] Step 2: Implement selection validation and `learning_article` creation.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add candidate selection flow`.

## Chunk 2: Article Processing

### Task 3: Add article content fetch and paragraph split

**Files:**
- Create: `src/main/java/com/ailearn/service/learning/ArticleContentService.java`
- Create: `src/main/java/com/ailearn/service/learning/ParagraphSplitService.java`
- Create: `src/test/java/com/ailearn/service/learning/ArticleContentServiceTest.java`
- Create: `src/test/java/com/ailearn/service/learning/ParagraphSplitServiceTest.java`

- [ ] Step 1: Write failing tests for content extraction and paragraph segmentation.
- [ ] Step 2: Implement services.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add article content processing`.

### Task 4: Add translation pipeline

**Files:**
- Create: `src/main/resources/prompts/translation-prompt.txt`
- Create: `src/main/java/com/ailearn/service/learning/TranslationService.java`
- Create: `src/test/java/com/ailearn/service/learning/TranslationServiceTest.java`

- [ ] Step 1: Write failing translation tests.
- [ ] Step 2: Implement paragraph translation service.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add paragraph translation`.

### Task 5: Add vocabulary extraction

**Files:**
- Create: `src/main/resources/prompts/vocabulary-extraction-prompt.txt`
- Create: `src/main/java/com/ailearn/service/learning/VocabularyExtractionService.java`
- Create: `src/test/java/com/ailearn/service/learning/VocabularyExtractionServiceTest.java`

- [ ] Step 1: Write failing tests for structured vocabulary extraction.
- [ ] Step 2: Implement extraction service for words and expressions.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add vocabulary extraction`.

## Chunk 3: Eudic And Workflow Orchestration

### Task 6: Add Eudic client

**Files:**
- Create: `src/main/java/com/ailearn/client/EudicClient.java`
- Create: `src/test/java/com/ailearn/client/EudicClientTest.java`

- [ ] Step 1: Write failing tests for study-list resolution and word push.
- [ ] Step 2: Implement client with mocked integration coverage.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add eudic client`.

### Task 7: Orchestrate selected-article workflow

**Files:**
- Create: `src/main/java/com/ailearn/service/learning/LearningWorkflowService.java`
- Create: `src/test/java/com/ailearn/service/learning/LearningWorkflowServiceTest.java`

- [ ] Step 1: Write failing orchestration tests for select -> content -> translate -> vocab -> Eudic.
- [ ] Step 2: Implement status transitions and persistence.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add selected article learning workflow`.

### Task 8: Add selected-article read API

**Files:**
- Create: `src/main/java/com/ailearn/api/learning/LearningArticleResponse.java`
- Create: `src/main/java/com/ailearn/controller/LearningArticleController.java`
- Create: `src/test/java/com/ailearn/controller/LearningArticleControllerTest.java`

- [ ] Step 1: Write failing tests for `GET /api/learning-articles/{id}`.
- [ ] Step 2: Implement article read model API.
- [ ] Step 3: Verify controller tests pass.
- [ ] Step 4: Commit with `feat: add learning article read api`.

## File Ownership Notes

This plan owns:

- selected-article schema,
- learning workflow services,
- Eudic client,
- selected-article APIs.

It must not implement:

- candidate generation,
- frontend UI,
- tutor chat agent.
