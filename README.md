# English Learn Assistant

> AI-powered English intensive reading assistant — daily AI news curation, bilingual study, vocabulary extraction with Eudic sync, and an article tutor agent.

**Languages:** [English](README.md) · [简体中文](README.zh-CN.md)

---

## Overview

English Learn Assistant helps you study English by reading curated AI news. Each day, the system fetches and ranks AI articles, lets you pick one, and turns it into a structured study session with bilingual paragraphs, extracted vocabulary, and a tutor agent that can answer questions about the article.

It is **not** an autonomous news-summarization bot. It is a focused tool for the daily flow of *pick one article → read deeply → learn vocabulary → discuss with the tutor*.

## Features

- **Daily candidate pipeline** — RSS ingestion → coarse keyword filter → LLM rerank → 3–5 recommended articles per day.
- **Selected-article processing** — On selection: full article fetch, paragraph splitting, English↔Chinese translation, vocabulary extraction.
- **Bilingual reading view** — Side-by-side paragraphs with optional vocabulary inline.
- **Eudic integration** — Push extracted vocabulary to your Eudic study list (manual control per item).
- **Tutor agent** — Article-scoped chat that can summarize, explain paragraphs, break down sentences, and answer follow-up questions.
- **Learning history** — Browse and reopen past study sessions.

## Architecture

```
┌─────────────────┐         ┌──────────────────────┐         ┌──────────────┐
│  Daily Cron     │ ──────► │  Spring Boot API     │ ──────► │  PostgreSQL  │
│  (8:00 AM)      │         │  (candidate / chat)  │         │  + Flyway    │
└─────────────────┘         └──────────┬───────────┘         └──────────────┘
                                       │
                                       ▼
                            ┌──────────────────────┐
                            │  LangChain4j + LLM   │
                            │  (rerank / translate │
                            │   / vocab / tutor)   │
                            └──────────────────────┘
                                       ▲
                                       │
                            ┌──────────┴───────────┐
                            │  Next.js Frontend    │
                            │  (study UI + chat)   │
                            └──────────────────────┘
```

### Tech stack

| Layer    | Stack                                                                   |
| -------- | ----------------------------------------------------------------------- |
| Backend  | Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, LangChain4j, Rome    |
| Database | PostgreSQL 16                                                           |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4, shadcn/ui             |
| Testing  | JUnit 5, Testcontainers, Vitest, Playwright                             |

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 20+ and npm
- Docker (for PostgreSQL)
- An OpenAI-compatible LLM API key

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Configure secrets

Copy `src/main/resources/application.yml` settings via environment variables (do **not** commit secrets):

```bash
export OPENAI_API_KEY=sk-...
export OPENAI_CHAT_MODEL=gpt-4o-mini
export AILEARN_EUDIC_AUTH_TOKEN=...   # optional, for vocabulary sync
```

### 3. Run the backend

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

## Project Structure

```
english-learn-assistant/
├── src/                        # Spring Boot backend
│   ├── main/java/com/ailearn/
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Candidate / learning / tutor services
│   │   ├── client/             # External integrations (Eudic, RSS)
│   │   ├── entity/             # JPA entities
│   │   └── config/             # App + LLM configuration
│   └── main/resources/
│       ├── db/migration/       # Flyway migrations
│       └── prompts/            # LLM prompt templates
├── frontend/                   # Next.js frontend
│   ├── src/app/                # App Router pages
│   ├── src/components/         # UI components
│   └── e2e/                    # Playwright smoke tests
└── docs/                       # Design specs and execution plans
```

## Key API Endpoints

| Method | Path                                          | Description                          |
| ------ | --------------------------------------------- | ------------------------------------ |
| GET    | `/api/candidates/today`                       | Today's candidate articles           |
| POST   | `/api/candidates/{id}/select`                 | Select an article for study          |
| GET    | `/api/learning-articles/{id}`                 | Full learning article with paragraphs and vocabulary |
| POST   | `/api/learning-articles/{id}/vocabulary/{vid}/eudic` | Push a vocabulary item to Eudic |
| GET    | `/api/learning-history`                       | Past study sessions                  |
| POST   | `/api/learning-articles/{id}/chat`            | Article-scoped tutor chat            |

## Configuration

Key environment variables (see `src/main/resources/application.yml` for defaults):

| Variable                          | Description                                     |
| --------------------------------- | ----------------------------------------------- |
| `OPENAI_API_KEY`                  | LLM API key (OpenAI-compatible)                 |
| `OPENAI_CHAT_MODEL`               | Chat model name                                 |
| `AILEARN_DATASOURCE_URL`          | PostgreSQL JDBC URL                             |
| `AILEARN_CANDIDATE_CRON`          | Daily generation cron (default `0 0 8 * * *`)   |
| `AILEARN_CANDIDATE_MAX`           | Number of daily candidates (default 5)          |
| `AILEARN_EUDIC_AUTH_TOKEN`        | Eudic API token for vocabulary sync             |
| `AILEARN_LLM_TIMEOUT_SECONDS`     | LLM call timeout (default 60s)                  |

## Development

### Backend tests

```bash
mvn test
```

### Frontend tests

```bash
cd frontend
npm test           # Vitest unit tests
npm run test:e2e   # Playwright smoke tests
```

## Documentation

- [Design Spec](docs/superpowers/specs/2026-04-24-english-article-tutor-agent-design.md)
- [Execution Plans](docs/superpowers/plans/)

## License

This project is private and not yet licensed for public use.
