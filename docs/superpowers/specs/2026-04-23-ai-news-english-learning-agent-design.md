# AI News Curation & English Learning Agent — Design Spec

## Overview

A Java-based AI agent that automatically curates AI news articles, generates bilingual (English-Chinese) translations, extracts technical glossaries, and exports everything to an Obsidian vault as Markdown files.

**Target user:** Java developer who wants to stay current on AI/Backend topics while improving English technical vocabulary.

## Architecture

### Approach: Monolithic Pipeline

A single Spring Boot application with a linear pipeline: **Fetch -> Score -> Translate -> Extract Glossary -> Export**. Each stage is a Spring `@Service`. A `@Scheduled` method triggers the pipeline daily.

### Tech Stack

- Java 21+
- Spring Boot 3
- LangChain4j (LLM orchestration, OpenAI provider)
- ROME or rssreader (RSS parsing)
- Jsoup (HTML content extraction)
- Jackson (JSON serialization)
- Maven (build)

## Project Structure

```
english-learn-assistant/
├── pom.xml
├── src/main/java/com/ailearn/
│   ├── AiLearnApplication.java              # Spring Boot entry point
│   ├── config/
│   │   └── AppConfig.java                    # Binds application.yml properties
│   ├── model/
│   │   ├── Article.java                      # title, url, content, source, publishedAt
│   │   ├── ScoredArticle.java                # Article + relevanceScore + reasoning
│   │   ├── TranslatedArticle.java            # ScoredArticle + bilingual paragraphs
│   │   └── GlossaryEntry.java               # word, ipa, englishDef, chineseDef
│   ├── service/
│   │   ├── RssFetchService.java             # Fetch + parse RSS feeds
│   │   ├── ArticleScoringService.java       # LLM-based relevance scoring
│   │   ├── TranslationService.java          # Paragraph-by-paragraph EN->ZH translation
│   │   ├── GlossaryService.java             # Extract technical terms via LLM
│   │   └── ObsidianExportService.java       # Render Markdown, write to vault
│   └── pipeline/
│       └── CurationPipeline.java            # Orchestrates the full flow, @Scheduled
├── src/main/resources/
│   ├── application.yml                       # All configuration
│   └── prompts/
│       ├── scoring-prompt.txt                # Prompt template for article scoring
│       ├── translation-prompt.txt            # Prompt template for translation
│       └── glossary-prompt.txt               # Prompt template for glossary extraction
└── src/test/java/com/ailearn/
    └── ...                                   # Tests mirror main structure
```

## Data Flow

```
RssFetchService -> ArticleScoringService -> TranslationService -> GlossaryService -> ObsidianExportService
```

1. **RssFetchService** parses configured RSS feeds into `Article` objects.
2. **ArticleScoringService** sends each article's title + summary to GPT-4o, which scores relevance (1-10) to configured topics ("Backend Architecture", "LLM Applications"). Articles scoring below the threshold (default: 7) are dropped.
3. **TranslationService** translates each surviving article paragraph-by-paragraph (English followed by Chinese translation in blockquote).
4. **GlossaryService** extracts technical terms from each article with word, IPA pronunciation, English definition, and Chinese definition.
5. **ObsidianExportService** renders per-article Markdown files and a consolidated daily glossary, writing them to the Obsidian vault.

### Deduplication

`RssFetchService` maintains an in-memory `Set<String>` of seen article URLs, persisted to a local JSON file (`./data/seen-articles.json`). Articles already seen are skipped.

### Error Handling

If an individual article fails at any stage (LLM timeout, parse error), the error is logged and the article is skipped. One bad article does not stop the batch.

## Models

### Article
- `title` (String)
- `url` (String)
- `content` (String) — full text extracted via Jsoup
- `source` (String) — feed name
- `publishedAt` (LocalDateTime)

### ScoredArticle
- Wraps `Article`
- `relevanceScore` (int, 1-10)
- `reasoning` (String) — LLM's explanation for the score

### TranslatedArticle
- Wraps `ScoredArticle`
- `bilingualParagraphs` (List<BilingualParagraph>) — each has `english` and `chinese` fields

### GlossaryEntry
- `word` (String)
- `ipa` (String) — IPA pronunciation
- `englishDefinition` (String)
- `chineseDefinition` (String)
- `sourceArticleTitle` (String)

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

## Glossary

| Word | IPA | English Definition | 中文释义 |
|------|-----|--------------------|----------|
| transformer | /traensˈfɔːrmər/ | A neural network architecture based on self-attention | 一种基于自注意力机制的神经网络架构 |
| RAG | /raeg/ | Retrieval-Augmented Generation, combining search with LLM output | 检索增强生成，将搜索与大模型输出结合 |
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

| Word | IPA | English Definition | 中文释义 | Source Article |
|------|-----|--------------------|----------|---------------|
| transformer | /traensˈfɔːrmər/ | A neural network architecture... | 一种基于自注意力... | Article Title |
| RAG | /raeg/ | Retrieval-Augmented Generation... | 检索增强生成... | Article Title |
```

The daily glossary deduplicates terms — if the same word appears in multiple articles, it keeps the first occurrence and lists all source articles.

## Configuration

All configuration lives in `application.yml`:

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
  obsidian:
    vault-path: "/Users/jphao/ObsidianVault"
    base-folder: "AI-News"
  dedup:
    store-path: "./data/seen-articles.json"
```

- OpenAI API key is provided via environment variable `OPENAI_API_KEY` (never in config files).
- RSS feeds, scoring topics, and vault path are all configurable without code changes.

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter` | Core framework, scheduling, DI |
| `langchain4j-spring-boot-starter` | LangChain4j Spring Boot integration |
| `langchain4j-open-ai-spring-boot-starter` | OpenAI provider for LangChain4j |
| `rome` or `rssreader` | RSS feed parsing |
| `jsoup` | HTML content extraction from article URLs |
| `jackson` | JSON serialization for dedup store |
| `spring-boot-starter-test` | JUnit 5, Mockito |

## Testing Strategy

- **Unit tests** — Each service tested in isolation. Mock `ChatLanguageModel` to avoid real LLM calls. Verify RSS parsing with sample XML, Markdown output with expected strings, etc.
- **Integration test** — One test for `CurationPipeline` with a mock LLM and sample RSS data to verify the full flow end-to-end.
- **No web/controller tests** — No REST layer exists.

## Intentional Omissions

- **No database** — Dedup uses a JSON file. Overkill for a personal tool.
- **No REST API** — Purely a scheduled batch job. Can be added later.
- **No retry/circuit-breaker** — If an LLM call fails, skip the article and move on.
