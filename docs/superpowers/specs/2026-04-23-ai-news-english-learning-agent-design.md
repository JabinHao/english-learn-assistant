# AI News Curation & English Learning Agent — Design Spec

## Overview

A Java-based conversational AI agent that automatically curates AI news articles, generates bilingual (English-Chinese) translations, performs smart vocabulary extraction with lemmatization and filtering, syncs words to Eudic (欧路词典) for spaced-repetition review, and exports everything to an Obsidian vault as Markdown files. The agent also exposes a REST API for chat-based interaction, article search, vocabulary quizzing, and manual control.

**Target user:** Senior Java developer running the agent on a Mac mini M4, who wants to stay current on AI/Backend topics while building English technical vocabulary.

## Architecture

### Approach: Conversational AI Agent

The LLM is the orchestrator. It has tools it can call to perform tasks. A `@Scheduled` trigger sends a daily message to the agent ("Curate today's AI news"), and the REST API allows interactive conversations.

```
┌─────────────────────────────────────────────────┐
│                  AI Agent Core                   │
│          (LangChain4j AI Service + Tools)        │
│                                                  │
│  Tools:                                          │
│  - FetchNewsTool        (fetch & parse RSS)      │
│  - ScoreArticleTool     (rate relevance)         │
│  - TranslateArticleTool (EN→ZH paragraph-by-para)│
│  - ExtractVocabularyTool(smart word extraction)  │
│  - ExportObsidianTool   (write Markdown to vault)│
│  - SyncEudicTool        (sync words to Eudic)   │
│  - SearchArticlesTool   (RAG search over articles)│
│  - SearchVocabularyTool (RAG search over glossary)│
│  - QuizTool             (vocabulary quiz)        │
└──────────────┬──────────────────┬───────────────┘
               │                  │
     ┌─────────▼──────┐   ┌──────▼───────┐
     │ REST API Layer  │   │  @Scheduled   │
     │ /api/chat       │   │  Daily curation│
     │ /api/articles   │   │  (agent runs   │
     │ /api/vocabulary  │   │   autonomously)│
     │ /api/curate     │   │               │
     └─────────┬──────┘   └──────────────┘
               │
     ┌─────────▼──────────────────┐
     │  PostgreSQL + pgvector      │
     │  (Docker, ARM-native)       │
     │  - articles + embeddings    │
     │  - glossary_entries         │
     │  - known_words              │
     └────────────────────────────┘
```

### Tech Stack

- Java 21+
- Spring Boot 3
- LangChain4j (AI Services, @Tool, chat memory, OpenAI provider)
- OpenAI GPT-4o (chat) + text-embedding-3-small (embeddings)
- PostgreSQL 16 + pgvector (Docker, ARM-native on M4)
- OpenNLP (lemmatization)
- ROME or rssreader (RSS parsing)
- Jsoup (HTML content extraction)
- Maven (build)

## Project Structure

```
english-learn-assistant/
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/ailearn/
│   ├── AiLearnApplication.java
│   ├── config/
│   │   └── AppConfig.java
│   ├── model/
│   │   ├── Article.java
│   │   ├── ScoredArticle.java
│   │   ├── TranslatedArticle.java
│   │   └── GlossaryEntry.java
│   ├── entity/                          # JPA entities
│   │   ├── ArticleEntity.java
│   │   ├── ArticleTranslationEntity.java
│   │   ├── GlossaryEntryEntity.java
│   │   └── KnownWordEntity.java
│   ├── repository/                      # Spring Data JPA
│   │   ├── ArticleRepository.java
│   │   ├── ArticleTranslationRepository.java
│   │   ├── GlossaryEntryRepository.java
│   │   └── KnownWordRepository.java
│   ├── service/
│   │   ├── RssFetchService.java
│   │   ├── ArticleScoringService.java
│   │   ├── TranslationService.java
│   │   ├── GlossaryService.java         # Orchestrates vocabulary extraction pipeline
│   │   ├── ObsidianExportService.java
│   │   ├── EudicApiClient.java          # Eudic REST API client
│   │   └── vocabulary/
│   │       ├── WordTokenizer.java
│   │       ├── LemmatizationService.java
│   │       ├── WordFilter.java
│   │       ├── CommonWordsProvider.java
│   │       └── KnownWordsStore.java
│   ├── agent/
│   │   ├── AiLearnAgent.java            # LangChain4j @AiService definition
│   │   └── tools/
│   │       ├── FetchNewsTool.java
│   │       ├── ScoreArticleTool.java
│   │       ├── TranslateArticleTool.java
│   │       ├── ExtractVocabularyTool.java
│   │       ├── ExportObsidianTool.java
│   │       ├── SyncEudicTool.java
│   │       ├── SearchArticlesTool.java
│   │       ├── SearchVocabularyTool.java
│   │       └── QuizTool.java
│   ├── controller/
│   │   ├── ChatController.java          # POST /api/chat
│   │   ├── ArticleController.java       # GET /api/articles
│   │   ├── VocabularyController.java    # GET/POST /api/vocabulary
│   │   └── CurateController.java        # POST /api/curate
│   └── pipeline/
│       └── CurationScheduler.java       # @Scheduled, sends message to agent
├── src/main/resources/
│   ├── application.yml
│   └── prompts/
│       ├── scoring-prompt.txt
│       ├── translation-prompt.txt
│       └── glossary-prompt.txt
├── src/test/java/com/ailearn/
│   └── ...
└── data/
    └── common-words.txt                 # Auto-downloaded, cached locally
```

## Data Flow: Daily Curation

The `CurationScheduler` sends a message to the agent: "Curate today's AI news." The agent then reasons and calls tools:

```
Agent receives: "Curate today's AI news"
  │
  ├─ Calls FetchNewsTool
  │    → RssFetchService parses RSS feeds
  │    → Dedup via articles.url UNIQUE constraint
  │    → Returns List<Article>
  │
  ├─ Calls ScoreArticleTool (for each article)
  │    → ArticleScoringService sends title+summary to LLM
  │    → Score 1-10, keep >= threshold
  │    → Stores article + embedding in PostgreSQL
  │    → Returns List<ScoredArticle>
  │
  ├─ Calls TranslateArticleTool (for each scored article)
  │    → TranslationService translates paragraph-by-paragraph
  │    → Stores translations in article_translations table
  │    → Returns List<TranslatedArticle>
  │
  ├─ Calls ExtractVocabularyTool (for each translated article)
  │    → GlossaryService runs the smart extraction pipeline
  │    → Stores entries + embeddings in glossary_entries table
  │    → Returns List<GlossaryEntry>
  │
  ├─ Calls SyncEudicTool
  │    → EudicApiClient syncs words with context to Eudic
  │    → Marks eudic_synced = true on success
  │
  └─ Calls ExportObsidianTool
       → ObsidianExportService writes per-article .md + daily glossary .md
```

## Smart Vocabulary Extraction Pipeline

The internal flow within `GlossaryService`:

```
Article Text
     │
     ▼
┌──────────────┐
│ WordTokenizer │  Split text, normalize (lowercase, strip punctuation,
│              │  remove numbers-only, single chars, URLs), deduplicate
└──────┬───────┘
       │ List<String> raw words
       ▼
┌────────────────────┐
│ LemmatizationService│  OpenNLP DictionaryLemmatizer
│                    │  "driven"→"drive", "architectures"→"architecture"
│                    │  Falls back to original word for acronyms/proper nouns
└──────┬─────────────┘
       │ List<String> lemmatized words
       ▼
┌─────────────┐
│  WordFilter  │  Remove words found in:
│              │    1. CommonWordsProvider (downloaded COCA 5000)
│              │    2. KnownWordsStore (from known_words DB table)
└──────┬──────┘
       │ List<String> candidate words (uncommon + unknown)
       ▼
┌─────────────────────┐
│ LLM (via LangChain4j)│  Prompt: "Act as an English teacher for a Senior
│                      │  Java Engineer. From these candidates + the original
│                      │  article, pick 10-15 high-value words including
│                      │  technical jargon, idiomatic expressions, and advanced
│                      │  academic verbs. For each provide: word, IPA,
│                      │  contextual English definition, Chinese definition.
│                      │  Also extract multi-word idiomatic expressions."
│                      │  Returns structured JSON via @AiService POJO mapping.
└──────┬──────────────┘
       │ List<GlossaryEntry>
       ▼
┌────────────────┐
│ KnownWordsStore │  Auto-add all extracted words (lemmatized) to known_words
│                │  table in PostgreSQL
└────────────────┘
```

### CommonWordsProvider

- On first run, downloads a public word frequency list (e.g., COCA 5000) and saves to `data/common-words.txt`.
- On subsequent runs, loads from the cached file.
- Words loaded into `Set<String>` for O(1) lookup.
- Download URL configurable in `application.yml`.

### LemmatizationService

- Uses OpenNLP `DictionaryLemmatizer` with `en-lemmatizer.dict` (~3MB, bundled as classpath resource).
- Falls back to original word if lemmatization returns no result.

### KnownWordsStore

- Backed by `known_words` table in PostgreSQL (replaces JSON file).
- Loaded into `ConcurrentHashMap.newKeySet()` at startup for fast lookup.
- New words appended after each article's extraction.

## Eudic Integration

### EudicApiClient

Built with Spring's `RestClient`. Auth via `Authorization: NIS <token>` header.

**Endpoints used:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/open/v1/studylist/category?language=en` | Fetch study lists, find target list ID |
| `POST` | `/api/open/v1/studylist/category` | Create `"ai-learn"` list if it doesn't exist |
| `POST` | `/api/open/v1/studylist/word` | Add single word WITH context sentence |
| `POST` | `/api/open/v1/studylist/note` | Attach note (IPA + definition) to the word |
| `GET` | `/api/open/v1/studylist/words` | Check if word already exists (avoid duplicates) |

**Sync flow per article:**
1. Ensure `"ai-learn"` study list exists (create if not).
2. For each extracted glossary entry:
   - `POST /studylist/word` — word + source sentence as context.
   - `POST /studylist/note` — IPA + English definition + Chinese definition.
3. Rate-limit aware: 10-15 words per article, ~2 calls each = ~30 calls. Within 30/min limit for a single article. Add delay between articles if batch-processing multiple.
4. Mark `eudic_synced = true` on success. Retry unsynchronized words on next run.

**Configuration:**
- Auth token via environment variable `EUDIC_AUTH_TOKEN`.
- Study list name configurable in `application.yml` (default: `"ai-learn"`).
- Base URL: `https://api.frdic.com`.

## Obsidian Output Format

### Per-Article File

Path: `{vault}/AI-News/{yyyy}/{MM}/{yyyy-MM-dd}-ai-news-{slug}.md`

```markdown
---
title: "Article Title Here"
source: https://original-url.com
date: 2026-04-23
score: 8
tags: [ai-news, backend-architecture]
---

# Article Title Here

## Original & Translation

The transformer architecture has become the foundation of modern NLP systems.

> Transformer 架构已成为现代自然语言处理系统的基础。

Recent advances in retrieval-augmented generation (RAG) have improved factual accuracy.

> 检索增强生成（RAG）的最新进展提高了事实准确性。

## Vocabulary

| Word | IPA | Definition | 中文释义 | Context | Mastered |
|------|-----|------------|----------|---------|----------|
| idempotent | /aɪˈdɛmpətənt/ | Producing the same result when applied multiple times | 幂等的 | "...ensuring the API call is **idempotent** across retries..." | [Mark](http://localhost:8080/api/vocabulary/master?word=idempotent) |
```

### Daily Consolidated Glossary

Path: `{vault}/AI-News/{yyyy}/{MM}/{yyyy-MM-dd}-glossary.md`

```markdown
---
title: "AI News Glossary - 2026-04-23"
date: 2026-04-23
tags: [ai-glossary]
---

# AI News Glossary - 2026-04-23

| Word | IPA | Definition | 中文释义 | Context | Source | Mastered |
|------|-----|------------|----------|---------|--------|----------|
| idempotent | /aɪˈdɛmpətənt/ | Producing the same result... | 幂等的... | "...the API call is **idempotent**..." | Article Title | [Mark](http://localhost:8080/api/vocabulary/master?word=idempotent) |
```

The "Mark" link hits `POST /api/vocabulary/master?word=xxx` on the Spring Boot app, which adds the word to `known_words` in PostgreSQL and returns a simple HTML success page.

## REST API

```
POST /api/chat                          # Conversational agent interface
  Body: { "message": "..." }
  Response: { "reply": "..." }

GET  /api/articles?date=2026-04-23      # Browse curated articles
GET  /api/articles/search?q=transformer # RAG search over articles

GET  /api/vocabulary?date=2026-04-23    # Browse extracted vocabulary
POST /api/vocabulary/master?word=xxx    # Mark word as mastered

POST /api/curate                        # Manually trigger curation

GET  /api/health                        # Health check
```

**Chat endpoint:** Stateful per session via LangChain4j `MessageWindowChatMemory`. Supports natural language queries like "Show me this week's top articles", "Quiz me on recent vocabulary", "Find articles about RAG architecture."

## Database Schema

```sql
CREATE TABLE articles (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(500) NOT NULL,
    url           VARCHAR(1000) NOT NULL UNIQUE,
    content       TEXT NOT NULL,
    source        VARCHAR(200),
    published_at  TIMESTAMP,
    score         INTEGER,
    score_reason  TEXT,
    embedding     vector(1536),
    created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE article_translations (
    id            BIGSERIAL PRIMARY KEY,
    article_id    BIGINT REFERENCES articles(id),
    paragraph_idx INTEGER NOT NULL,
    english_text  TEXT NOT NULL,
    chinese_text  TEXT NOT NULL
);

CREATE TABLE glossary_entries (
    id                 BIGSERIAL PRIMARY KEY,
    word               VARCHAR(200) NOT NULL,
    lemma              VARCHAR(200) NOT NULL,
    ipa                VARCHAR(200),
    english_definition TEXT,
    chinese_definition TEXT,
    source_sentence    TEXT,
    article_id         BIGINT REFERENCES articles(id),
    embedding          vector(1536),
    mastered           BOOLEAN DEFAULT FALSE,
    eudic_synced       BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT NOW()
);

CREATE TABLE known_words (
    id         BIGSERIAL PRIMARY KEY,
    word       VARCHAR(200) NOT NULL,
    lemma      VARCHAR(200) NOT NULL UNIQUE,
    mastered_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_articles_embedding ON articles USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_glossary_embedding ON glossary_entries USING ivfflat (embedding vector_cosine_ops);
```

## Infrastructure

### Docker Compose

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ailearn
      POSTGRES_USER: ailearn
      POSTGRES_PASSWORD: ailearn
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

The `pgvector/pgvector:pg16` image supports ARM natively — runs well on Mac mini M4. The Spring Boot app runs natively via `mvn spring-boot:run`.

## Configuration

```yaml
ailearn:
  schedule:
    cron: "0 0 8 * * *"
  rss:
    feeds:
      - name: "Hacker News - AI"
        url: "https://hnrss.org/newest?q=AI+LLM"
      - name: "ArXiv - AI"
        url: "https://rss.arxiv.org/rss/cs.AI"
      - name: "TechCrunch - AI"
        url: "https://techcrunch.com/category/artificial-intelligence/feed/"
  scoring:
    threshold: 7
    topics:
      - "Backend Architecture"
      - "LLM Applications"
  llm:
    provider: openai
    model: gpt-4o
    temperature: 0.3
    max-tokens: 4096
  embedding:
    model: text-embedding-3-small
  obsidian:
    vault-path: "/Users/jphao/ObsidianVault"
    base-folder: "AI-News"
  eudic:
    base-url: "https://api.frdic.com"
    study-list-name: "ai-learn"
    language: "en"
  vocabulary:
    common-words-url: "https://..."   # COCA 5000 download URL
    common-words-path: "./data/common-words.txt"
    extract-count: 10-15

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ailearn
    username: ailearn
    password: ailearn
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Environment variables (never in config):
- `OPENAI_API_KEY`
- `EUDIC_AUTH_TOKEN`

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter` | Core framework, scheduling, DI |
| `spring-boot-starter-web` | REST API endpoints |
| `spring-boot-starter-data-jpa` | Database access via JPA/Hibernate |
| `langchain4j-spring-boot-starter` | LangChain4j Spring Boot integration |
| `langchain4j-open-ai-spring-boot-starter` | OpenAI chat + embeddings provider |
| `langchain4j-pgvector-spring-boot-starter` | pgvector embedding store for RAG |
| `opennlp-tools` | Lemmatization |
| `rome` or `rssreader` | RSS feed parsing |
| `jsoup` | HTML content extraction |
| `postgresql` | PostgreSQL JDBC driver |
| `flyway-core` | Database migration management |
| `spring-boot-starter-test` | JUnit 5, Mockito |
| `testcontainers-postgresql` | PostgreSQL in tests |

## Testing Strategy

- **Unit tests** — Each service and tool tested in isolation. Mock `ChatLanguageModel` for LLM calls. Verify RSS parsing with sample XML, Markdown output, WordFilter logic, lemmatization, etc.
- **Integration tests** — Testcontainers spins up PostgreSQL + pgvector. Test full curation pipeline, Eudic client (with WireMock for the API), and REST endpoints.
- **No UI tests** — REST API only, tested via MockMvc.

## Intentional Omissions

- **No frontend UI** — REST API only. Client can be added later.
- **No retry/circuit-breaker** — If an LLM call fails, skip the article and move on. Eudic sync retries on next run via `eudic_synced` flag.
- **No user authentication** — Personal tool running on a local network.
