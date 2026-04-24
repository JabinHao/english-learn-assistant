# English Article Tutor Agent — Design Spec

## Overview

This project is an English intensive-reading learning system with an article-tutor agent.

Its purpose is not to fully automate news consumption. Its purpose is to help the user:

1. get 3-5 curated AI news candidates each day,
2. choose one article to study,
3. read that article with translation and vocabulary support,
4. use an agent to understand the article more deeply.

The system should treat AI news curation as a deterministic content pipeline, and treat the agent as a learning assistant around the selected article.

## Product Goal

The core user flow is:

1. The system fetches AI news daily.
2. It produces 3-5 recommended candidate articles.
3. The user selects one article in the UI.
4. The system processes only that selected article:
   - full translation,
   - paragraph-level bilingual view,
   - vocabulary extraction,
   - push vocabulary to Eudic.
5. The agent helps the user study the selected article by:
   - summarizing the article,
   - explaining paragraphs,
   - breaking down difficult sentences,
   - explaining vocabulary and expressions,
   - answering follow-up questions.

## Non-Goals

These are explicitly out of scope for the first version:

- fully autonomous agent orchestration of the entire pipeline,
- translating every high-scoring article,
- maintaining the canonical vocabulary mastery state locally,
- building a heavy RAG system as the foundation,
- long-term adaptive learning strategy across many articles,
- multi-agent architecture.

## Architecture

### Recommended Architecture

Use a hybrid design:

- deterministic application services for ingestion and article processing,
- a single tutor agent for explanation-oriented interactions.

This keeps the main workflow reliable and easy to reason about while still introducing meaningful agent-development concepts.

### High-Level Structure

```text
Daily Scheduler
  -> CandidateGenerationService
     -> RSS fetch
     -> rule-based coarse filter
     -> LLM rerank
     -> persist 3-5 candidates

User selects one candidate in UI
  -> LearningWorkflowService
     -> load article content
     -> translate article
     -> split paragraphs
     -> extract vocabulary
     -> push vocabulary to Eudic
     -> persist learning artifact set

User opens learning page
  -> LearningQueryService
     -> article summary
     -> bilingual paragraphs
     -> vocabulary list
     -> learning history

User chats about selected article
  -> TutorAgentService
     -> summary / explanation / difficult sentence breakdown /
        vocabulary explanation / follow-up Q&A
```

## Why This Architecture

This split is intentional.

The curation and processing pipeline should be deterministic because:

- it has clear input/output boundaries,
- it affects stored state,
- it should be easy to debug and retry,
- it should not depend on agent hidden reasoning.

The tutor experience should be agent-driven because:

- explanation quality benefits from LLM reasoning,
- the user wants interactive learning help,
- this is the part of the product where agent behavior adds real value,
- it is a good surface for learning agent development without overusing agents.

## Core Capabilities

### 1. Daily Candidate Generation

Every day the system fetches AI-related articles from configured RSS feeds.

Selection is done in two stages:

- Stage 1: rule-based coarse filtering
  - remove duplicates,
  - keep only recent items,
  - filter by allowed feeds or topic keywords.
- Stage 2: LLM reranking
  - score relevance to AI trends,
  - score usefulness for English learning,
  - produce a short recommendation reason,
  - keep the top 3-5 articles.

The output is a candidate list, not a fully processed study package.

### 2. Article Selection

The user chooses one candidate article from the UI.

This selection triggers the learning workflow. The system should not process all candidates in depth. That would waste cost, create noise in vocabulary output, and diverge from the intended product behavior.

### 3. Learning Workflow

After selection, the system processes only the chosen article.

The workflow should:

1. fetch and normalize the article content,
2. split it into paragraphs,
3. translate it into Chinese paragraph by paragraph,
4. extract important vocabulary and expressions,
5. push extracted vocabulary to Eudic,
6. persist the complete learning record for later review.

### 4. Tutor Agent

The agent is scoped to one selected learning article.

The first version should support:

- article main-idea summary,
- paragraph-by-paragraph explanation,
- difficult sentence breakdown,
- explanation of key words and expressions,
- free-form follow-up Q&A based on the article context.

The agent should not be responsible for news fetching, candidate generation, DB workflow orchestration, or external side-effect sequencing.

## Data Model

The first version should keep the schema minimal and aligned to the real user flow.

### candidate_batch

Represents one daily candidate-generation run.

Suggested fields:

- `id`
- `run_date`
- `status`
- `source_count`
- `candidate_count`
- `created_at`

### candidate_article

Represents one curated article candidate from a batch.

Suggested fields:

- `id`
- `batch_id`
- `title`
- `url`
- `source`
- `published_at`
- `summary`
- `coarse_filter_reason`
- `llm_score`
- `llm_reason`
- `rank_order`
- `selected`
- `created_at`

### learning_article

Represents the single article the user chose to study.

Suggested fields:

- `id`
- `candidate_article_id`
- `status`
- `title`
- `url`
- `source`
- `published_at`
- `article_content`
- `summary`
- `selected_at`
- `translated_at`
- `vocabulary_extracted_at`
- `eudic_pushed_at`
- `created_at`

Suggested statuses:

- `SELECTED`
- `CONTENT_READY`
- `TRANSLATED`
- `VOCAB_READY`
- `EUDIC_PUSHED`
- `FAILED`

### article_paragraph

Stores bilingual paragraph data for the selected article.

Suggested fields:

- `id`
- `learning_article_id`
- `paragraph_index`
- `english_text`
- `chinese_text`

### vocabulary_item

Stores extracted vocabulary for the selected article.

Suggested fields:

- `id`
- `learning_article_id`
- `word`
- `lemma`
- `type`
- `ipa`
- `english_definition`
- `chinese_definition`
- `source_sentence`
- `eudic_pushed`
- `eudic_pushed_at`
- `created_at`

`type` may be values like:

- `WORD`
- `PHRASE`
- `EXPRESSION`

### chat_session

Represents one tutor interaction thread tied to a selected learning article.

Suggested fields:

- `id`
- `learning_article_id`
- `title`
- `created_at`

### chat_message

Stores conversation turns for the tutor agent.

Suggested fields:

- `id`
- `session_id`
- `role`
- `content`
- `created_at`

## Source of Truth

The source of truth for the user’s long-term vocabulary notebook is Eudic.

This system should not try to replicate full notebook management in the first version.

That means:

- no local `known_words` table as the primary vocabulary truth source,
- no local `mastered_words` table as the first-version learning-state authority,
- local vocabulary data exists to support the selected article and audit what was pushed to Eudic.

If later we need local learning analytics, that can be added as a second-phase feature.

## Application Services

### CandidateGenerationService

Responsibilities:

- fetch RSS feeds,
- normalize feed items,
- deduplicate,
- apply rule-based filtering,
- call LLM rerank,
- persist 3-5 candidates.

### CandidateSelectionService

Responsibilities:

- validate a candidate can be selected,
- create `learning_article`,
- trigger the article learning workflow.

### LearningWorkflowService

Responsibilities:

- fetch full article content,
- split paragraphs,
- translate text,
- extract vocabulary,
- push to Eudic,
- update workflow status.

This service is the deterministic backbone of the product.

### LearningQueryService

Responsibilities:

- assemble learning page data,
- return article summary,
- return bilingual paragraphs,
- return extracted vocabulary,
- return past learning records.

### TutorAgentService

Responsibilities:

- answer article-specific questions,
- explain difficult content,
- provide guided tutoring responses.

It should operate on persisted article context, not raw global system state.

## Agent Design

### Agent Role

The agent should behave as an English article tutor for a technical reader.

It should help the user understand:

- what the article is saying,
- why certain paragraphs are important,
- how difficult sentences are structured,
- what important words or expressions mean in context.

### Agent Inputs

Every tutor call should receive structured context:

- article title,
- article summary,
- full article text or relevant paragraph slice,
- bilingual paragraphs,
- extracted vocabulary items,
- current chat history.

### Agent Tools

The first version should use a small, controlled tool surface.

Recommended tools:

- `summarize_article`
- `explain_paragraph`
- `explain_sentence`
- `explain_vocabulary`

Potential optional tool:

- `generate_quiz`

Avoid tools like:

- `fetch_news`
- `score_articles`
- `sync_all_vocab`
- broad DB search tools

Those belong to application services, not the tutoring agent.

### Why Small Tool Surface

This project is also for learning agent development. The best learning outcome comes from seeing a clear boundary between:

- what should be deterministic system logic,
- what should be agent reasoning.

A small tool surface will make prompt behavior easier to inspect, debug, and improve.

## API Design

### Candidate APIs

- `GET /api/candidates/today`
  - returns today’s 3-5 candidate articles

- `POST /api/candidates/{id}/select`
  - selects one candidate for study
  - triggers the deterministic learning workflow

### Learning APIs

- `GET /api/learning-articles/{id}`
  - returns article summary, bilingual paragraphs, vocabulary items, and workflow status

- `GET /api/learning-history`
  - returns previously selected study articles

### Tutor APIs

- `POST /api/learning-articles/{id}/chat`
  - sends a user question about the selected article
  - returns the tutor agent response

## UI Design

The first version should have three pages.

### 1. Candidate List Page

Purpose:

- show today’s 3-5 candidate articles,
- help the user choose one article to study.

Each item should show:

- title,
- source,
- publication time,
- short summary,
- recommendation reason.

### 2. Learning Page

Purpose:

- support intensive reading for the selected article.

Suggested sections:

- article title and summary,
- paragraph-by-paragraph bilingual content,
- vocabulary/expression list,
- tutor chat panel.

### 3. Learning History Page

Purpose:

- review past study sessions,
- inspect which article was selected on previous days,
- review previously extracted vocabulary.

## Error Handling

The workflow should be resumable and observable.

Recommended rules:

- candidate generation failure should mark the batch as failed without corrupting prior data,
- article processing failure should mark `learning_article.status = FAILED`,
- Eudic push failure should not discard extracted vocabulary,
- translation failure should be visible at paragraph level when possible,
- tutor failures should not break the learning page itself.

Retries should exist for:

- RSS fetch,
- article content fetch,
- LLM processing calls,
- Eudic API calls.

## Testing Strategy

The system should be tested in layers.

### Unit Tests

Focus on:

- RSS normalization,
- coarse filter rules,
- candidate ranking input shaping,
- paragraph splitting,
- vocabulary extraction transformations,
- Eudic client request building.

### Integration Tests

Focus on:

- daily candidate generation flow,
- candidate selection -> learning workflow,
- persistence of bilingual paragraphs and vocabulary items,
- Eudic sync success/failure behavior.

### Agent-Focused Tests

Focus on:

- tutor prompt contract,
- tool-selection behavior if tools are used,
- output quality constraints for explanation tasks,
- article-scoped context isolation.

## Suggested Tech Stack

Recommended first-version stack:

- Java 21
- Spring Boot 3
- PostgreSQL 16
- Flyway
- Jsoup
- Rome
- LangChain4j
- OpenAI chat model

Not recommended as first-version priorities:

- pgvector as a foundational dependency,
- heavy RAG architecture,
- multi-agent orchestration,
- autonomous agent-first workflow control.

If vector search is later needed for article recall or study-history semantic search, it can be added after the main learning workflow is stable.

## Implementation Principles

Use these principles during implementation:

1. Deterministic workflows own state transitions.
2. The tutor agent is article-scoped.
3. Only the selected article gets full processing.
4. Eudic is the vocabulary notebook authority.
5. Keep the first version small enough to ship and use daily.

## Recommended Phase Breakdown

### Phase 1

- daily candidate generation,
- candidate list UI/API,
- select-one-article workflow,
- translation,
- vocabulary extraction,
- Eudic push,
- article learning page.

### Phase 2

- tutor chat,
- paragraph explanation,
- difficult sentence breakdown,
- vocabulary explanation.

### Phase 3

- quiz generation,
- learning analytics,
- optional semantic search and memory features.

## Final Recommendation

Build this as a study product first and an agent demo second.

The right architecture is:

- deterministic content pipeline,
- single article-tutor agent,
- Eudic integration as vocabulary sink,
- UI-driven article selection,
- focused first-version scope.

That will match the actual learning behavior, keep the system maintainable, and still give strong agent-development learning value.
