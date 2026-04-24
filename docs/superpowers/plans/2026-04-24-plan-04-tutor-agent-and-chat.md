# Tutor Agent And Chat Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the article-scoped tutor agent and chat flow for summary, paragraph explanation, difficult sentence breakdown, vocabulary explanation, and follow-up Q&A.

**Architecture:** The tutor agent is a bounded service operating on one selected article at a time. It consumes persisted article context from the deterministic workflow instead of orchestrating ingestion or external side effects.

**Tech Stack:** Java 21, Spring Boot 3, LangChain4j, OpenAI, PostgreSQL 16, JUnit 5

---

## Chunk 1: Chat Persistence And Contracts

### Task 1: Add chat schema

**Files:**
- Create: `src/main/resources/db/migration/V3__chat_session.sql`
- Create: `src/main/java/com/ailearn/entity/ChatSessionEntity.java`
- Create: `src/main/java/com/ailearn/entity/ChatMessageEntity.java`
- Create: `src/main/java/com/ailearn/repository/ChatSessionRepository.java`
- Create: `src/main/java/com/ailearn/repository/ChatMessageRepository.java`

- [ ] Step 1: Add chat session and chat message migration.
- [ ] Step 2: Implement entities and repositories.
- [ ] Step 3: Verify migration applies.
- [ ] Step 4: Commit with `feat: add tutor chat persistence`.

### Task 2: Add chat DTOs

**Files:**
- Create: `src/main/java/com/ailearn/api/chat/ChatRequest.java`
- Create: `src/main/java/com/ailearn/api/chat/ChatResponse.java`
- Create: `src/main/java/com/ailearn/api/chat/ChatMessageResponse.java`

- [ ] Step 1: Define request and response contracts for article-scoped chat.
- [ ] Step 2: Add serialization tests if needed.
- [ ] Step 3: Commit with `feat: add tutor chat dto contracts`.

## Chunk 2: Tutor Agent

### Task 3: Add article context assembler

**Files:**
- Create: `src/main/java/com/ailearn/service/tutor/ArticleTutorContextService.java`
- Create: `src/test/java/com/ailearn/service/tutor/ArticleTutorContextServiceTest.java`

- [ ] Step 1: Write failing tests for assembling article summary, paragraphs, and vocabulary into tutor context.
- [ ] Step 2: Implement context service.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add tutor context assembly`.

### Task 4: Add tutor prompt assets and service

**Files:**
- Create: `src/main/resources/prompts/tutor-system-prompt.txt`
- Create: `src/main/java/com/ailearn/service/tutor/TutorAgentService.java`
- Create: `src/test/java/com/ailearn/service/tutor/TutorAgentServiceTest.java`

- [ ] Step 1: Write failing tests for article-scoped tutoring behavior.
- [ ] Step 2: Implement tutor service with constrained tool or helper surface.
- [ ] Step 3: Verify tests pass.
- [ ] Step 4: Commit with `feat: add tutor agent service`.

## Chunk 3: Chat API

### Task 5: Add chat controller

**Files:**
- Create: `src/main/java/com/ailearn/controller/TutorChatController.java`
- Create: `src/test/java/com/ailearn/controller/TutorChatControllerTest.java`

- [ ] Step 1: Write failing tests for `POST /api/learning-articles/{id}/chat`.
- [ ] Step 2: Implement chat endpoint with persistence.
- [ ] Step 3: Verify controller tests pass.
- [ ] Step 4: Commit with `feat: add tutor chat api`.

### Task 6: Add chat history read API

**Files:**
- Modify: `src/main/java/com/ailearn/controller/TutorChatController.java`
- Create: `src/test/java/com/ailearn/controller/TutorChatHistoryControllerTest.java`

- [ ] Step 1: Add `GET` endpoint for session history if needed by UI.
- [ ] Step 2: Verify history tests pass.
- [ ] Step 3: Commit with `feat: add tutor chat history api`.

## Chunk 4: Integration

### Task 7: Add article tutor integration coverage

**Files:**
- Create: `src/test/java/com/ailearn/integration/TutorChatIntegrationTest.java`

- [ ] Step 1: Add integration test for selected article + tutor chat flow.
- [ ] Step 2: Run focused integration coverage.
- [ ] Step 3: Commit with `test: add tutor chat integration coverage`.

## File Ownership Notes

This plan owns:

- chat schema,
- tutor context assembly,
- tutor agent service,
- tutor chat APIs.

It must not implement:

- candidate generation,
- selected-article processing pipeline,
- frontend app shell.
