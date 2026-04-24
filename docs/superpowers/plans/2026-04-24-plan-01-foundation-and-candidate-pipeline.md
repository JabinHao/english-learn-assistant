# Foundation And Candidate Pipeline Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the backend foundation, schema, daily candidate generation pipeline, and candidate list APIs.

**Architecture:** Spring Boot owns deterministic daily ingestion. This plan defines the database baseline, RSS ingestion, coarse filtering, LLM reranking, persistence, and read APIs for 3-5 daily candidates. It deliberately stops before selected-article deep processing.

**Tech Stack:** Java 21, Spring Boot 3, PostgreSQL 16, Flyway, Jsoup, Rome, LangChain4j, JUnit 5, Testcontainers

---

## Chunk 1: Project Foundation

### Task 1: Bootstrap backend workspace

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/ailearn/AiLearnApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `.gitignore`

- [ ] Step 1: Create Maven, Docker, app bootstrap, and config files.
- [ ] Step 2: Add Spring Boot, Flyway, PostgreSQL, Jsoup, Rome, LangChain4j, and test dependencies.
- [ ] Step 3: Run `mvn compile`.
- [ ] Step 4: Start PostgreSQL with `docker compose up -d`.
- [ ] Step 5: Commit with `feat: scaffold backend foundation`.

### Task 2: Create baseline schema

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Create: `src/main/java/com/ailearn/entity/CandidateBatchEntity.java`
- Create: `src/main/java/com/ailearn/entity/CandidateArticleEntity.java`
- Create: `src/main/java/com/ailearn/repository/CandidateBatchRepository.java`
- Create: `src/main/java/com/ailearn/repository/CandidateArticleRepository.java`

- [ ] Step 1: Write schema for `candidate_batch` and `candidate_article`.
- [ ] Step 2: Add indexes and uniqueness constraints for candidate dedupe.
- [ ] Step 3: Implement JPA entities and repositories.
- [ ] Step 4: Run app startup to verify Flyway applies successfully.
- [ ] Step 5: Commit with `feat: add candidate generation schema`.

## Chunk 2: Candidate Generation Pipeline

### Task 3: Fetch and normalize feed items

**Files:**
- Create: `src/main/java/com/ailearn/config/AppConfig.java`
- Create: `src/main/java/com/ailearn/model/FeedArticle.java`
- Create: `src/main/java/com/ailearn/service/rss/RssFetchService.java`
- Create: `src/test/java/com/ailearn/service/rss/RssFetchServiceTest.java`
- Create: `src/test/resources/sample-rss.xml`

- [ ] Step 1: Write failing tests for RSS parsing and normalization.
- [ ] Step 2: Implement feed config binding and RSS fetch service.
- [ ] Step 3: Verify parsing test passes.
- [ ] Step 4: Commit with `feat: add rss fetch service`.

### Task 4: Add coarse filter and dedupe

**Files:**
- Create: `src/main/java/com/ailearn/service/candidate/CandidateCoarseFilter.java`
- Create: `src/test/java/com/ailearn/service/candidate/CandidateCoarseFilterTest.java`

- [ ] Step 1: Write failing tests for dedupe, freshness, and keyword rules.
- [ ] Step 2: Implement coarse filter rules.
- [ ] Step 3: Verify unit tests pass.
- [ ] Step 4: Commit with `feat: add candidate coarse filter`.

### Task 5: Add LLM reranking

**Files:**
- Create: `src/main/resources/prompts/candidate-rerank-prompt.txt`
- Create: `src/main/java/com/ailearn/model/RankedCandidate.java`
- Create: `src/main/java/com/ailearn/service/candidate/CandidateRerankService.java`
- Create: `src/test/java/com/ailearn/service/candidate/CandidateRerankServiceTest.java`

- [ ] Step 1: Write failing tests for rerank parsing and top-N selection.
- [ ] Step 2: Implement rerank service with structured output parsing.
- [ ] Step 3: Verify unit tests pass.
- [ ] Step 4: Commit with `feat: add candidate reranking`.

### Task 6: Orchestrate daily candidate generation

**Files:**
- Create: `src/main/java/com/ailearn/service/candidate/CandidateGenerationService.java`
- Create: `src/main/java/com/ailearn/scheduler/CandidateGenerationScheduler.java`
- Create: `src/test/java/com/ailearn/service/candidate/CandidateGenerationServiceTest.java`

- [ ] Step 1: Write failing tests for full candidate-generation orchestration.
- [ ] Step 2: Implement batch creation, filtering, reranking, and persistence.
- [ ] Step 3: Verify service tests pass.
- [ ] Step 4: Commit with `feat: add daily candidate generation pipeline`.

## Chunk 3: Candidate Read APIs

### Task 7: Add candidate DTOs and controller

**Files:**
- Create: `src/main/java/com/ailearn/api/candidate/CandidateArticleResponse.java`
- Create: `src/main/java/com/ailearn/controller/CandidateController.java`
- Create: `src/test/java/com/ailearn/controller/CandidateControllerTest.java`

- [ ] Step 1: Write failing controller tests for `GET /api/candidates/today`.
- [ ] Step 2: Implement response DTO and controller.
- [ ] Step 3: Verify controller tests pass.
- [ ] Step 4: Commit with `feat: add candidate list api`.

### Task 8: Add integration coverage

**Files:**
- Create: `src/test/java/com/ailearn/integration/CandidatePipelineIntegrationTest.java`

- [ ] Step 1: Add Testcontainers integration test for generation + read path.
- [ ] Step 2: Run focused integration test.
- [ ] Step 3: Run `mvn test`.
- [ ] Step 4: Commit with `test: add candidate pipeline integration coverage`.

## File Ownership Notes

This plan owns:

- schema baseline,
- candidate entities and repositories,
- candidate-generation services,
- candidate scheduler,
- candidate read APIs.

It must not implement:

- selected-article processing,
- Eudic push,
- tutor agent,
- frontend app.
