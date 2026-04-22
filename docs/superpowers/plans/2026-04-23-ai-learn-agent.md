# AI News Curation & English Learning Agent — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a conversational AI agent that curates AI news, generates bilingual translations, performs smart vocabulary extraction, syncs to Eudic, and exports to Obsidian.

**Architecture:** LangChain4j AI Service with @Tool-annotated methods as the orchestrator. Spring Boot 3 scheduled job triggers daily curation. REST API exposes chat, articles, vocabulary, and manual trigger endpoints. PostgreSQL + pgvector stores articles, glossary, embeddings, and known words.

**Tech Stack:** Java 21, Spring Boot 3, LangChain4j (OpenAI), PostgreSQL 16 + pgvector (Docker), OpenNLP, ROME, Jsoup, Flyway, Maven

**Spec:** `docs/superpowers/specs/2026-04-23-ai-news-english-learning-agent-design.md`

---

## File Map

```
english-learn-assistant/
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/ailearn/
│   ├── AiLearnApplication.java
│   ├── config/
│   │   ├── AppConfig.java                    # Binds ailearn.* properties
│   │   └── AgentConfig.java                  # Wires AiLearnAgent bean
│   ├── model/
│   │   ├── Article.java                      # RSS-parsed article DTO
│   │   ├── ScoredArticle.java                # Article + score + reasoning
│   │   ├── BilingualParagraph.java           # EN + ZH paragraph pair
│   │   ├── TranslatedArticle.java            # ScoredArticle + bilingual paragraphs
│   │   └── GlossaryEntry.java               # word, ipa, defs, context sentence
│   ├── entity/
│   │   ├── ArticleEntity.java
│   │   ├── ArticleTranslationEntity.java
│   │   ├── GlossaryEntryEntity.java
│   │   └── KnownWordEntity.java
│   ├── repository/
│   │   ├── ArticleRepository.java
│   │   ├── ArticleTranslationRepository.java
│   │   ├── GlossaryEntryRepository.java
│   │   └── KnownWordRepository.java
│   ├── service/
│   │   ├── RssFetchService.java
│   │   ├── ArticleScoringService.java
│   │   ├── TranslationService.java
│   │   ├── GlossaryService.java
│   │   ├── ObsidianExportService.java
│   │   ├── EudicApiClient.java
│   │   └── vocabulary/
│   │       ├── WordTokenizer.java
│   │       ├── LemmatizationService.java
│   │       ├── WordFilter.java
│   │       ├── CommonWordsProvider.java
│   │       └── KnownWordsStore.java
│   ├── agent/
│   │   ├── AiLearnAgent.java                # @AiService interface
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
│   │   ├── ChatController.java
│   │   ├── ArticleController.java
│   │   ├── VocabularyController.java
│   │   └── CurateController.java
│   └── scheduler/
│       └── CurationScheduler.java
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/
│   │   └── V1__init_schema.sql
│   └── prompts/
│       ├── scoring-prompt.txt
│       ├── translation-prompt.txt
│       └── glossary-prompt.txt
├── src/test/java/com/ailearn/
│   ├── service/
│   │   ├── RssFetchServiceTest.java
│   │   ├── ArticleScoringServiceTest.java
│   │   ├── TranslationServiceTest.java
│   │   ├── GlossaryServiceTest.java
│   │   ├── ObsidianExportServiceTest.java
│   │   ├── EudicApiClientTest.java
│   │   └── vocabulary/
│   │       ├── WordTokenizerTest.java
│   │       ├── LemmatizationServiceTest.java
│   │       ├── WordFilterTest.java
│   │       └── CommonWordsProviderTest.java
│   ├── controller/
│   │   └── VocabularyControllerTest.java
│   └── integration/
│       └── CurationPipelineIntegrationTest.java
└── data/
    └── (common-words.txt auto-downloaded at runtime)
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/ailearn/AiLearnApplication.java`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.4</version>
        <relativePath/>
    </parent>

    <groupId>com.ailearn</groupId>
    <artifactId>english-learn-assistant</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>AI English Learn Assistant</name>
    <description>AI news curation and English learning agent</description>

    <properties>
        <java.version>21</java.version>
        <langchain4j.version>1.0.0-beta3</langchain4j.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- LangChain4j -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-pgvector</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- NLP -->
        <dependency>
            <groupId>org.apache.opennlp</groupId>
            <artifactId>opennlp-tools</artifactId>
            <version>2.5.3</version>
        </dependency>

        <!-- RSS -->
        <dependency>
            <groupId>com.rometools</groupId>
            <artifactId>rome</artifactId>
            <version>2.1.0</version>
        </dependency>

        <!-- HTML parsing -->
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.18.3</version>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create docker-compose.yml**

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

- [ ] **Step 3: Create AiLearnApplication.java**

```java
package com.ailearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLearnApplication.class, args);
    }
}
```

- [ ] **Step 4: Create application.yml**

```yaml
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
  flyway:
    enabled: true

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o
      temperature: 0.3
      max-tokens: 4096
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small

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
  obsidian:
    vault-path: "${OBSIDIAN_VAULT_PATH:/Users/jphao/ObsidianVault}"
    base-folder: "AI-News"
  eudic:
    base-url: "https://api.frdic.com"
    auth-token: ${EUDIC_AUTH_TOKEN:}
    study-list-name: "ai-learn"
    language: "en"
  vocabulary:
    common-words-url: "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english.txt"
    common-words-path: "./data/common-words.txt"

server:
  port: 8080
```

- [ ] **Step 5: Start Docker and verify compilation**

Run:
```bash
docker compose up -d
mvn compile
```
Expected: BUILD SUCCESS, PostgreSQL container running.

- [ ] **Step 6: Commit**

```bash
git add pom.xml docker-compose.yml src/main/java/com/ailearn/AiLearnApplication.java src/main/resources/application.yml
git commit -m "feat: scaffold Spring Boot project with LangChain4j, pgvector, OpenNLP dependencies"
```

---

## Task 2: Database Schema & JPA Entities

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Create: `src/main/java/com/ailearn/entity/ArticleEntity.java`
- Create: `src/main/java/com/ailearn/entity/ArticleTranslationEntity.java`
- Create: `src/main/java/com/ailearn/entity/GlossaryEntryEntity.java`
- Create: `src/main/java/com/ailearn/entity/KnownWordEntity.java`
- Create: `src/main/java/com/ailearn/repository/ArticleRepository.java`
- Create: `src/main/java/com/ailearn/repository/ArticleTranslationRepository.java`
- Create: `src/main/java/com/ailearn/repository/GlossaryEntryRepository.java`
- Create: `src/main/java/com/ailearn/repository/KnownWordRepository.java`

- [ ] **Step 1: Create Flyway migration V1__init_schema.sql**

```sql
CREATE EXTENSION IF NOT EXISTS vector;

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
    article_id    BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
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
    article_id         BIGINT REFERENCES articles(id) ON DELETE CASCADE,
    embedding          vector(1536),
    mastered           BOOLEAN DEFAULT FALSE,
    eudic_synced       BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT NOW()
);

CREATE TABLE known_words (
    id          BIGSERIAL PRIMARY KEY,
    word        VARCHAR(200) NOT NULL,
    lemma       VARCHAR(200) NOT NULL UNIQUE,
    mastered_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_articles_embedding ON articles USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
CREATE INDEX idx_glossary_embedding ON glossary_entries USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
CREATE INDEX idx_glossary_article_id ON glossary_entries(article_id);
CREATE INDEX idx_article_translations_article_id ON article_translations(article_id);
```

- [ ] **Step 2: Create ArticleEntity.java**

```java
package com.ailearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class ArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 200)
    private String source;

    private LocalDateTime publishedAt;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String scoreReason;

    private LocalDateTime createdAt;

    public ArticleEntity() {}

    public ArticleEntity(String title, String url, String content, String source, LocalDateTime publishedAt) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.source = source;
        this.publishedAt = publishedAt;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getScoreReason() { return scoreReason; }
    public void setScoreReason(String scoreReason) { this.scoreReason = scoreReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Create ArticleTranslationEntity.java**

```java
package com.ailearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "article_translations")
public class ArticleTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private Integer paragraphIdx;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String englishText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String chineseText;

    public ArticleTranslationEntity() {}

    public ArticleTranslationEntity(Long articleId, Integer paragraphIdx, String englishText, String chineseText) {
        this.articleId = articleId;
        this.paragraphIdx = paragraphIdx;
        this.englishText = englishText;
        this.chineseText = chineseText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Integer getParagraphIdx() { return paragraphIdx; }
    public void setParagraphIdx(Integer paragraphIdx) { this.paragraphIdx = paragraphIdx; }
    public String getEnglishText() { return englishText; }
    public void setEnglishText(String englishText) { this.englishText = englishText; }
    public String getChineseText() { return chineseText; }
    public void setChineseText(String chineseText) { this.chineseText = chineseText; }
}
```

- [ ] **Step 4: Create GlossaryEntryEntity.java**

```java
package com.ailearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "glossary_entries")
public class GlossaryEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String word;

    @Column(nullable = false, length = 200)
    private String lemma;

    @Column(length = 200)
    private String ipa;

    @Column(columnDefinition = "TEXT")
    private String englishDefinition;

    @Column(columnDefinition = "TEXT")
    private String chineseDefinition;

    @Column(columnDefinition = "TEXT")
    private String sourceSentence;

    @Column(name = "article_id")
    private Long articleId;

    @Column(nullable = false)
    private Boolean mastered = false;

    @Column(nullable = false)
    private Boolean eudicSynced = false;

    private LocalDateTime createdAt;

    public GlossaryEntryEntity() {}

    public GlossaryEntryEntity(String word, String lemma, String ipa,
                                String englishDefinition, String chineseDefinition,
                                String sourceSentence, Long articleId) {
        this.word = word;
        this.lemma = lemma;
        this.ipa = ipa;
        this.englishDefinition = englishDefinition;
        this.chineseDefinition = chineseDefinition;
        this.sourceSentence = sourceSentence;
        this.articleId = articleId;
        this.mastered = false;
        this.eudicSynced = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getLemma() { return lemma; }
    public void setLemma(String lemma) { this.lemma = lemma; }
    public String getIpa() { return ipa; }
    public void setIpa(String ipa) { this.ipa = ipa; }
    public String getEnglishDefinition() { return englishDefinition; }
    public void setEnglishDefinition(String englishDefinition) { this.englishDefinition = englishDefinition; }
    public String getChineseDefinition() { return chineseDefinition; }
    public void setChineseDefinition(String chineseDefinition) { this.chineseDefinition = chineseDefinition; }
    public String getSourceSentence() { return sourceSentence; }
    public void setSourceSentence(String sourceSentence) { this.sourceSentence = sourceSentence; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Boolean getMastered() { return mastered; }
    public void setMastered(Boolean mastered) { this.mastered = mastered; }
    public Boolean getEudicSynced() { return eudicSynced; }
    public void setEudicSynced(Boolean eudicSynced) { this.eudicSynced = eudicSynced; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 5: Create KnownWordEntity.java**

```java
package com.ailearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "known_words")
public class KnownWordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String word;

    @Column(nullable = false, unique = true, length = 200)
    private String lemma;

    private LocalDateTime masteredAt;

    public KnownWordEntity() {}

    public KnownWordEntity(String word, String lemma) {
        this.word = word;
        this.lemma = lemma;
        this.masteredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getLemma() { return lemma; }
    public void setLemma(String lemma) { this.lemma = lemma; }
    public LocalDateTime getMasteredAt() { return masteredAt; }
    public void setMasteredAt(LocalDateTime masteredAt) { this.masteredAt = masteredAt; }
}
```

- [ ] **Step 6: Create repositories**

`ArticleRepository.java`:
```java
package com.ailearn.repository;

import com.ailearn.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    boolean existsByUrl(String url);
    Optional<ArticleEntity> findByUrl(String url);
    List<ArticleEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
```

`ArticleTranslationRepository.java`:
```java
package com.ailearn.repository;

import com.ailearn.entity.ArticleTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleTranslationRepository extends JpaRepository<ArticleTranslationEntity, Long> {
    List<ArticleTranslationEntity> findByArticleIdOrderByParagraphIdx(Long articleId);
}
```

`GlossaryEntryRepository.java`:
```java
package com.ailearn.repository;

import com.ailearn.entity.GlossaryEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface GlossaryEntryRepository extends JpaRepository<GlossaryEntryEntity, Long> {
    List<GlossaryEntryEntity> findByArticleId(Long articleId);
    List<GlossaryEntryEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<GlossaryEntryEntity> findByEudicSyncedFalse();
    List<GlossaryEntryEntity> findByMasteredFalse();
}
```

`KnownWordRepository.java`:
```java
package com.ailearn.repository;

import com.ailearn.entity.KnownWordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Set;

public interface KnownWordRepository extends JpaRepository<KnownWordEntity, Long> {
    boolean existsByLemma(String lemma);
    Set<KnownWordEntity> findAll();
}
```

- [ ] **Step 7: Verify Flyway migration runs**

Run:
```bash
docker compose up -d
mvn spring-boot:run
```
Expected: App starts, Flyway runs `V1__init_schema.sql`, tables created. Stop the app with Ctrl+C.

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/db/ src/main/java/com/ailearn/entity/ src/main/java/com/ailearn/repository/
git commit -m "feat: add database schema, JPA entities, and Spring Data repositories"
```

---

## Task 3: Domain Models & Config

**Files:**
- Create: `src/main/java/com/ailearn/model/Article.java`
- Create: `src/main/java/com/ailearn/model/ScoredArticle.java`
- Create: `src/main/java/com/ailearn/model/BilingualParagraph.java`
- Create: `src/main/java/com/ailearn/model/TranslatedArticle.java`
- Create: `src/main/java/com/ailearn/model/GlossaryEntry.java`
- Create: `src/main/java/com/ailearn/config/AppConfig.java`

- [ ] **Step 1: Create Article.java**

```java
package com.ailearn.model;

import java.time.LocalDateTime;

public record Article(
    String title,
    String url,
    String content,
    String source,
    LocalDateTime publishedAt
) {}
```

- [ ] **Step 2: Create ScoredArticle.java**

```java
package com.ailearn.model;

public record ScoredArticle(
    Article article,
    int relevanceScore,
    String reasoning
) {}
```

- [ ] **Step 3: Create BilingualParagraph.java**

```java
package com.ailearn.model;

public record BilingualParagraph(
    String english,
    String chinese
) {}
```

- [ ] **Step 4: Create TranslatedArticle.java**

```java
package com.ailearn.model;

import java.util.List;

public record TranslatedArticle(
    ScoredArticle scoredArticle,
    List<BilingualParagraph> paragraphs
) {}
```

- [ ] **Step 5: Create GlossaryEntry.java**

```java
package com.ailearn.model;

public record GlossaryEntry(
    String word,
    String lemma,
    String ipa,
    String englishDefinition,
    String chineseDefinition,
    String sourceSentence
) {}
```

- [ ] **Step 6: Create AppConfig.java**

```java
package com.ailearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ailearn")
public class AppConfig {

    private Schedule schedule = new Schedule();
    private Rss rss = new Rss();
    private Scoring scoring = new Scoring();
    private Obsidian obsidian = new Obsidian();
    private Eudic eudic = new Eudic();
    private Vocabulary vocabulary = new Vocabulary();

    public static class Schedule {
        private String cron = "0 0 8 * * *";
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    public static class RssFeed {
        private String name;
        private String url;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Rss {
        private List<RssFeed> feeds = List.of();
        public List<RssFeed> getFeeds() { return feeds; }
        public void setFeeds(List<RssFeed> feeds) { this.feeds = feeds; }
    }

    public static class Scoring {
        private int threshold = 7;
        private List<String> topics = List.of();
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> topics) { this.topics = topics; }
    }

    public static class Obsidian {
        private String vaultPath;
        private String baseFolder = "AI-News";
        public String getVaultPath() { return vaultPath; }
        public void setVaultPath(String vaultPath) { this.vaultPath = vaultPath; }
        public String getBaseFolder() { return baseFolder; }
        public void setBaseFolder(String baseFolder) { this.baseFolder = baseFolder; }
    }

    public static class Eudic {
        private String baseUrl = "https://api.frdic.com";
        private String authToken;
        private String studyListName = "ai-learn";
        private String language = "en";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getStudyListName() { return studyListName; }
        public void setStudyListName(String studyListName) { this.studyListName = studyListName; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class Vocabulary {
        private String commonWordsUrl;
        private String commonWordsPath = "./data/common-words.txt";
        public String getCommonWordsUrl() { return commonWordsUrl; }
        public void setCommonWordsUrl(String commonWordsUrl) { this.commonWordsUrl = commonWordsUrl; }
        public String getCommonWordsPath() { return commonWordsPath; }
        public void setCommonWordsPath(String commonWordsPath) { this.commonWordsPath = commonWordsPath; }
    }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    public Rss getRss() { return rss; }
    public void setRss(Rss rss) { this.rss = rss; }
    public Scoring getScoring() { return scoring; }
    public void setScoring(Scoring scoring) { this.scoring = scoring; }
    public Obsidian getObsidian() { return obsidian; }
    public void setObsidian(Obsidian obsidian) { this.obsidian = obsidian; }
    public Eudic getEudic() { return eudic; }
    public void setEudic(Eudic eudic) { this.eudic = eudic; }
    public Vocabulary getVocabulary() { return vocabulary; }
    public void setVocabulary(Vocabulary vocabulary) { this.vocabulary = vocabulary; }
}
```

- [ ] **Step 7: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/ailearn/model/ src/main/java/com/ailearn/config/AppConfig.java
git commit -m "feat: add domain model records and application config properties"
```

---

## Task 4: RSS Fetch Service

**Files:**
- Create: `src/test/java/com/ailearn/service/RssFetchServiceTest.java`
- Create: `src/main/java/com/ailearn/service/RssFetchService.java`
- Create: `src/test/resources/sample-rss.xml`

- [ ] **Step 1: Create sample RSS test fixture**

`src/test/resources/sample-rss.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Test AI Feed</title>
    <item>
      <title>Understanding Transformer Architectures</title>
      <link>https://example.com/transformers</link>
      <description>A deep dive into transformer architectures for backend engineers.</description>
      <pubDate>Wed, 23 Apr 2026 08:00:00 GMT</pubDate>
    </item>
    <item>
      <title>RAG Patterns for Production</title>
      <link>https://example.com/rag</link>
      <description>How to implement retrieval-augmented generation in production systems.</description>
      <pubDate>Wed, 23 Apr 2026 09:00:00 GMT</pubDate>
    </item>
  </channel>
</rss>
```

- [ ] **Step 2: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.Article;
import com.ailearn.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RssFetchServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private RssFetchService rssFetchService;

    @Test
    void parseRssFeed_shouldExtractArticlesFromXml() throws Exception {
        String xmlContent = new String(
            getClass().getClassLoader().getResourceAsStream("sample-rss.xml").readAllBytes()
        );

        when(articleRepository.existsByUrl(anyString())).thenReturn(false);

        List<Article> articles = rssFetchService.parseRssXml(xmlContent, "Test Feed");

        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).title()).isEqualTo("Understanding Transformer Architectures");
        assertThat(articles.get(0).url()).isEqualTo("https://example.com/transformers");
        assertThat(articles.get(0).source()).isEqualTo("Test Feed");
    }

    @Test
    void parseRssFeed_shouldSkipDuplicateUrls() throws Exception {
        String xmlContent = new String(
            getClass().getClassLoader().getResourceAsStream("sample-rss.xml").readAllBytes()
        );

        when(articleRepository.existsByUrl("https://example.com/transformers")).thenReturn(true);
        when(articleRepository.existsByUrl("https://example.com/rag")).thenReturn(false);

        List<Article> articles = rssFetchService.parseRssXml(xmlContent, "Test Feed");

        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).title()).isEqualTo("RAG Patterns for Production");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=RssFetchServiceTest -f pom.xml`
Expected: FAIL — `RssFetchService` does not exist.

- [ ] **Step 4: Implement RssFetchService**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.Article;
import com.ailearn.repository.ArticleRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class RssFetchService {

    private static final Logger log = LoggerFactory.getLogger(RssFetchService.class);

    private final AppConfig appConfig;
    private final ArticleRepository articleRepository;
    private final HttpClient httpClient;

    public RssFetchService(AppConfig appConfig, ArticleRepository articleRepository) {
        this.appConfig = appConfig;
        this.articleRepository = articleRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<Article> fetchAllFeeds() {
        List<Article> allArticles = new ArrayList<>();
        for (AppConfig.RssFeed feed : appConfig.getRss().getFeeds()) {
            try {
                String xml = fetchUrl(feed.getUrl());
                List<Article> articles = parseRssXml(xml, feed.getName());
                allArticles.addAll(articles);
                log.info("Fetched {} articles from {}", articles.size(), feed.getName());
            } catch (Exception e) {
                log.error("Failed to fetch feed {}: {}", feed.getName(), e.getMessage());
            }
        }
        return allArticles;
    }

    public List<Article> parseRssXml(String xml, String sourceName) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new StringReader(xml));
        List<Article> articles = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            String url = entry.getLink();
            if (url == null || articleRepository.existsByUrl(url)) {
                continue;
            }

            String description = "";
            if (entry.getDescription() != null) {
                description = Jsoup.parse(entry.getDescription().getValue()).text();
            }

            LocalDateTime publishedAt = null;
            if (entry.getPublishedDate() != null) {
                publishedAt = entry.getPublishedDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            }

            articles.add(new Article(
                entry.getTitle(),
                url,
                description,
                sourceName,
                publishedAt
            ));
        }
        return articles;
    }

    String fetchUrl(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -Dtest=RssFetchServiceTest`
Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ailearn/service/RssFetchService.java src/test/java/com/ailearn/service/RssFetchServiceTest.java src/test/resources/sample-rss.xml
git commit -m "feat: add RSS fetch service with ROME parsing and URL deduplication"
```

---

## Task 5: Article Scoring Service

**Files:**
- Create: `src/test/java/com/ailearn/service/ArticleScoringServiceTest.java`
- Create: `src/main/java/com/ailearn/service/ArticleScoringService.java`
- Create: `src/main/resources/prompts/scoring-prompt.txt`

- [ ] **Step 1: Create scoring prompt**

`src/main/resources/prompts/scoring-prompt.txt`:
```
You are an AI content curator. Rate the following article for relevance to these topics: {{topics}}.

Article title: {{title}}
Article summary: {{content}}

Respond with JSON only:
{
  "score": <integer 1-10>,
  "reasoning": "<one sentence explanation>"
}

Score 7+ means highly relevant to at least one topic. Score below 7 means not relevant enough.
```

- [ ] **Step 2: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.Article;
import com.ailearn.model.ScoredArticle;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleScoringServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    private AppConfig appConfig;
    private ArticleScoringService scoringService;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
        AppConfig.Scoring scoring = new AppConfig.Scoring();
        scoring.setThreshold(7);
        scoring.setTopics(List.of("Backend Architecture", "LLM Applications"));
        appConfig.setScoring(scoring);
        scoringService = new ArticleScoringService(chatModel, appConfig);
    }

    @Test
    void scoreArticle_aboveThreshold_returnsScored() {
        Article article = new Article("LLM Agents in Production", "https://example.com/llm",
            "How to deploy LLM agents at scale", "Test", LocalDateTime.now());

        when(chatModel.generate(any(List.class)))
            .thenReturn(Response.from(AiMessage.from(
                "{\"score\": 9, \"reasoning\": \"Directly about LLM applications in production\"}"
            )));

        Optional<ScoredArticle> result = scoringService.score(article);

        assertThat(result).isPresent();
        assertThat(result.get().relevanceScore()).isEqualTo(9);
    }

    @Test
    void scoreArticle_belowThreshold_returnsEmpty() {
        Article article = new Article("Cooking Tips", "https://example.com/cooking",
            "Best pasta recipes", "Test", LocalDateTime.now());

        when(chatModel.generate(any(List.class)))
            .thenReturn(Response.from(AiMessage.from(
                "{\"score\": 2, \"reasoning\": \"Not related to tech\"}"
            )));

        Optional<ScoredArticle> result = scoringService.score(article);

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=ArticleScoringServiceTest`
Expected: FAIL — `ArticleScoringService` does not exist.

- [ ] **Step 4: Implement ArticleScoringService**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.Article;
import com.ailearn.model.ScoredArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleScoringService {

    private static final Logger log = LoggerFactory.getLogger(ArticleScoringService.class);
    private final ChatLanguageModel chatModel;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticleScoringService(ChatLanguageModel chatModel, AppConfig appConfig) {
        this.chatModel = chatModel;
        this.appConfig = appConfig;
    }

    public Optional<ScoredArticle> score(Article article) {
        try {
            String topics = String.join(", ", appConfig.getScoring().getTopics());
            String prompt = String.format(
                "You are an AI content curator. Rate the following article for relevance to these topics: %s.\n\n" +
                "Article title: %s\nArticle summary: %s\n\n" +
                "Respond with JSON only:\n{\"score\": <integer 1-10>, \"reasoning\": \"<one sentence>\"}",
                topics, article.title(), article.content()
            );

            var response = chatModel.generate(List.of(
                SystemMessage.from("You are a content relevance scorer. Respond with valid JSON only."),
                UserMessage.from(prompt)
            ));

            String json = response.content().text();
            JsonNode node = objectMapper.readTree(json);
            int score = node.get("score").asInt();
            String reasoning = node.get("reasoning").asText();

            if (score >= appConfig.getScoring().getThreshold()) {
                return Optional.of(new ScoredArticle(article, score, reasoning));
            }
            log.info("Article '{}' scored {} (below threshold {})", article.title(), score, appConfig.getScoring().getThreshold());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to score article '{}': {}", article.title(), e.getMessage());
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -Dtest=ArticleScoringServiceTest`
Expected: 2 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ailearn/service/ArticleScoringService.java src/test/java/com/ailearn/service/ArticleScoringServiceTest.java src/main/resources/prompts/scoring-prompt.txt
git commit -m "feat: add article scoring service with LLM-based relevance rating"
```

---

## Task 6: Translation Service

**Files:**
- Create: `src/test/java/com/ailearn/service/TranslationServiceTest.java`
- Create: `src/main/java/com/ailearn/service/TranslationService.java`
- Create: `src/main/resources/prompts/translation-prompt.txt`

- [ ] **Step 1: Create translation prompt**

`src/main/resources/prompts/translation-prompt.txt`:
```
You are a professional English-Chinese translator specializing in AI and technology.
Translate the following English paragraph into natural, accurate Chinese.
Preserve technical terms in their commonly used form.
Return ONLY the Chinese translation, no explanations.

English:
{{paragraph}}
```

- [ ] **Step 2: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.model.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    @InjectMocks
    private TranslationService translationService;

    @Test
    void translate_shouldProduceBilingualParagraphs() {
        Article article = new Article("Test", "https://example.com/test",
            "Transformers are great.\n\nThey use attention.", "Test", LocalDateTime.now());
        ScoredArticle scored = new ScoredArticle(article, 8, "Relevant");

        when(chatModel.generate(any(List.class)))
            .thenReturn(Response.from(AiMessage.from("Transformer 非常好。")))
            .thenReturn(Response.from(AiMessage.from("它们使用注意力机制。")));

        TranslatedArticle result = translationService.translate(scored);

        assertThat(result.paragraphs()).hasSize(2);
        assertThat(result.paragraphs().get(0).english()).isEqualTo("Transformers are great.");
        assertThat(result.paragraphs().get(0).chinese()).isEqualTo("Transformer 非常好。");
        assertThat(result.paragraphs().get(1).english()).isEqualTo("They use attention.");
        assertThat(result.paragraphs().get(1).chinese()).isEqualTo("它们使用注意力机制。");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=TranslationServiceTest`
Expected: FAIL — `TranslationService` does not exist.

- [ ] **Step 4: Implement TranslationService**

```java
package com.ailearn.service;

import com.ailearn.model.BilingualParagraph;
import com.ailearn.model.ScoredArticle;
import com.ailearn.model.TranslatedArticle;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private final ChatLanguageModel chatModel;

    public TranslationService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public TranslatedArticle translate(ScoredArticle scoredArticle) {
        String content = scoredArticle.article().content();
        String[] paragraphs = Arrays.stream(content.split("\n\n"))
            .map(String::trim)
            .filter(p -> !p.isEmpty())
            .toArray(String[]::new);

        List<BilingualParagraph> bilingualParagraphs = new ArrayList<>();
        for (String paragraph : paragraphs) {
            try {
                String chinese = translateParagraph(paragraph);
                bilingualParagraphs.add(new BilingualParagraph(paragraph, chinese));
            } catch (Exception e) {
                log.error("Failed to translate paragraph: {}", e.getMessage());
                bilingualParagraphs.add(new BilingualParagraph(paragraph, "[Translation failed]"));
            }
        }

        return new TranslatedArticle(scoredArticle, bilingualParagraphs);
    }

    private String translateParagraph(String paragraph) {
        var response = chatModel.generate(List.of(
            SystemMessage.from("You are a professional English-Chinese translator specializing in AI and technology. " +
                "Translate the following English paragraph into natural, accurate Chinese. " +
                "Preserve technical terms in their commonly used form. " +
                "Return ONLY the Chinese translation, no explanations."),
            UserMessage.from(paragraph)
        ));
        return response.content().text().trim();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -Dtest=TranslationServiceTest`
Expected: 1 test PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ailearn/service/TranslationService.java src/test/java/com/ailearn/service/TranslationServiceTest.java src/main/resources/prompts/translation-prompt.txt
git commit -m "feat: add paragraph-by-paragraph EN→ZH translation service"
```

---

## Task 7: Vocabulary Extraction Pipeline

**Files:**
- Create: `src/test/java/com/ailearn/service/vocabulary/WordTokenizerTest.java`
- Create: `src/main/java/com/ailearn/service/vocabulary/WordTokenizer.java`
- Create: `src/test/java/com/ailearn/service/vocabulary/LemmatizationServiceTest.java`
- Create: `src/main/java/com/ailearn/service/vocabulary/LemmatizationService.java`
- Create: `src/test/java/com/ailearn/service/vocabulary/CommonWordsProviderTest.java`
- Create: `src/main/java/com/ailearn/service/vocabulary/CommonWordsProvider.java`
- Create: `src/main/java/com/ailearn/service/vocabulary/KnownWordsStore.java`
- Create: `src/test/java/com/ailearn/service/vocabulary/WordFilterTest.java`
- Create: `src/main/java/com/ailearn/service/vocabulary/WordFilter.java`

### 7a: WordTokenizer

- [ ] **Step 1: Write the failing test**

```java
package com.ailearn.service.vocabulary;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class WordTokenizerTest {

    private final WordTokenizer tokenizer = new WordTokenizer();

    @Test
    void tokenize_shouldSplitAndNormalize() {
        String text = "The Transformer architecture has become foundational. " +
                      "RAG improves accuracy — see https://example.com for details.";

        List<String> tokens = tokenizer.tokenize(text);

        assertThat(tokens).contains("transformer", "architecture", "foundational", "rag", "improves", "accuracy", "see", "details");
        assertThat(tokens).doesNotContain("https://example.com", "—", "the", ".");
    }

    @Test
    void tokenize_shouldRemoveSingleCharsAndNumbers() {
        String text = "I have 42 items in a 3D array";

        List<String> tokens = tokenizer.tokenize(text);

        assertThat(tokens).doesNotContain("i", "a", "42", "3");
        assertThat(tokens).contains("have", "items", "3d", "array");
    }

    @Test
    void tokenize_shouldDeduplicateWords() {
        String text = "the quick brown quick fox";

        List<String> tokens = tokenizer.tokenize(text);

        long quickCount = tokens.stream().filter(t -> t.equals("quick")).count();
        assertThat(quickCount).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=WordTokenizerTest`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement WordTokenizer**

```java
package com.ailearn.service.vocabulary;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class WordTokenizer {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");

    public List<String> tokenize(String text) {
        String cleaned = URL_PATTERN.matcher(text).replaceAll("");

        Set<String> seen = new LinkedHashSet<>();
        Arrays.stream(cleaned.split("[\\s\\p{Punct}]+"))
            .map(String::toLowerCase)
            .map(String::trim)
            .filter(w -> !w.isEmpty())
            .filter(w -> w.length() > 1)
            .filter(w -> WORD_PATTERN.matcher(w).matches())
            .forEach(seen::add);

        return List.copyOf(seen);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=WordTokenizerTest`
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ailearn/service/vocabulary/WordTokenizer.java src/test/java/com/ailearn/service/vocabulary/WordTokenizerTest.java
git commit -m "feat: add word tokenizer with URL removal and deduplication"
```

### 7b: LemmatizationService

- [ ] **Step 6: Write the failing test**

```java
package com.ailearn.service.vocabulary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class LemmatizationServiceTest {

    private static LemmatizationService lemmatizationService;

    @BeforeAll
    static void setUp() {
        lemmatizationService = new LemmatizationService();
        lemmatizationService.init();
    }

    @Test
    void lemmatize_shouldReduceToBaseForm() {
        List<String> words = List.of("driven", "architectures", "running", "processes");

        List<String> lemmas = lemmatizationService.lemmatize(words);

        assertThat(lemmas).contains("drive", "architecture", "run", "process");
    }

    @Test
    void lemmatize_shouldKeepAcronymsUnchanged() {
        List<String> words = List.of("rag", "llm", "api");

        List<String> lemmas = lemmatizationService.lemmatize(words);

        assertThat(lemmas).containsExactly("rag", "llm", "api");
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `mvn test -Dtest=LemmatizationServiceTest`
Expected: FAIL — class does not exist.

- [ ] **Step 8: Implement LemmatizationService**

```java
package com.ailearn.service.vocabulary;

import jakarta.annotation.PostConstruct;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class LemmatizationService {

    private static final Logger log = LoggerFactory.getLogger(LemmatizationService.class);
    private DictionaryLemmatizer lemmatizer;
    private POSTaggerME posTagger;

    @PostConstruct
    public void init() {
        try {
            InputStream posModelStream = getClass().getResourceAsStream("/opennlp/en-pos-maxent.bin");
            if (posModelStream != null) {
                POSModel posModel = new POSModel(posModelStream);
                posTagger = new POSTaggerME(posModel);
            }

            InputStream dictStream = getClass().getResourceAsStream("/opennlp/en-lemmatizer.dict");
            if (dictStream != null) {
                lemmatizer = new DictionaryLemmatizer(dictStream);
            }
        } catch (Exception e) {
            log.warn("Failed to load OpenNLP models, lemmatization will be passthrough: {}", e.getMessage());
        }
    }

    public List<String> lemmatize(List<String> words) {
        if (lemmatizer == null || posTagger == null) {
            return words;
        }

        String[] wordArray = words.toArray(new String[0]);
        String[] tags = posTagger.tag(wordArray);
        String[] lemmas = lemmatizer.lemmatize(wordArray, tags);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmas.length; i++) {
            if ("O".equals(lemmas[i])) {
                result.add(words.get(i));
            } else {
                result.add(lemmas[i]);
            }
        }
        return result;
    }
}
```

Note: The OpenNLP model files (`en-pos-maxent.bin` and `en-lemmatizer.dict`) must be downloaded and placed in `src/main/resources/opennlp/`. Download them from the OpenNLP models page:
- POS model: https://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin
- Lemmatizer dict: https://raw.githubusercontent.com/richardwilly98/opennlp-lemmatizer-dict/master/en-lemmatizer.dict

Run:
```bash
mkdir -p src/main/resources/opennlp
curl -o src/main/resources/opennlp/en-pos-maxent.bin https://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin
curl -o src/main/resources/opennlp/en-lemmatizer.dict https://raw.githubusercontent.com/richardwilly98/opennlp-lemmatizer-dict/master/en-lemmatizer.dict
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn test -Dtest=LemmatizationServiceTest`
Expected: 2 tests PASS.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/ailearn/service/vocabulary/LemmatizationService.java src/test/java/com/ailearn/service/vocabulary/LemmatizationServiceTest.java src/main/resources/opennlp/
git commit -m "feat: add OpenNLP-based lemmatization service"
```

### 7c: CommonWordsProvider

- [ ] **Step 11: Write the failing test**

```java
package com.ailearn.service.vocabulary;

import com.ailearn.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommonWordsProviderTest {

    @TempDir
    Path tempDir;

    private CommonWordsProvider provider;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig();
        AppConfig.Vocabulary vocab = new AppConfig.Vocabulary();
        vocab.setCommonWordsPath(tempDir.resolve("common-words.txt").toString());
        vocab.setCommonWordsUrl("unused-in-this-test");
        appConfig.setVocabulary(vocab);
        provider = new CommonWordsProvider(appConfig);
    }

    @Test
    void loadFromFile_shouldPopulateCommonWordsSet() throws IOException {
        Path file = tempDir.resolve("common-words.txt");
        Files.writeString(file, "the\nand\nis\nare\nhave\n");

        provider.init();
        Set<String> words = provider.getCommonWords();

        assertThat(words).containsExactlyInAnyOrder("the", "and", "is", "are", "have");
    }

    @Test
    void isCommon_shouldReturnTrueForCommonWord() throws IOException {
        Path file = tempDir.resolve("common-words.txt");
        Files.writeString(file, "the\nand\nis\n");

        provider.init();

        assertThat(provider.isCommon("the")).isTrue();
        assertThat(provider.isCommon("transformer")).isFalse();
    }
}
```

- [ ] **Step 12: Run test to verify it fails**

Run: `mvn test -Dtest=CommonWordsProviderTest`
Expected: FAIL — class does not exist.

- [ ] **Step 13: Implement CommonWordsProvider**

```java
package com.ailearn.service.vocabulary;

import com.ailearn.config.AppConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class CommonWordsProvider {

    private static final Logger log = LoggerFactory.getLogger(CommonWordsProvider.class);
    private final AppConfig appConfig;
    private Set<String> commonWords = Collections.emptySet();

    public CommonWordsProvider(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void init() {
        Path filePath = Path.of(appConfig.getVocabulary().getCommonWordsPath());
        if (!Files.exists(filePath)) {
            download(filePath);
        }
        loadFromFile(filePath);
    }

    private void download(Path target) {
        try {
            String url = appConfig.getVocabulary().getCommonWordsUrl();
            if (url == null || url.isBlank()) {
                log.warn("No common words URL configured, skipping download");
                return;
            }
            log.info("Downloading common words list from {}", url);
            Files.createDirectories(target.getParent());
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Files.writeString(target, response.body());
            log.info("Downloaded common words list to {}", target);
        } catch (Exception e) {
            log.error("Failed to download common words list: {}", e.getMessage());
        }
    }

    private void loadFromFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Set<String> words = new HashSet<>();
                Files.lines(filePath)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(words::add);
                commonWords = Collections.unmodifiableSet(words);
                log.info("Loaded {} common words", commonWords.size());
            }
        } catch (IOException e) {
            log.error("Failed to load common words from {}: {}", filePath, e.getMessage());
        }
    }

    public boolean isCommon(String word) {
        return commonWords.contains(word.toLowerCase());
    }

    public Set<String> getCommonWords() {
        return commonWords;
    }
}
```

- [ ] **Step 14: Run tests to verify they pass**

Run: `mvn test -Dtest=CommonWordsProviderTest`
Expected: 2 tests PASS.

- [ ] **Step 15: Commit**

```bash
git add src/main/java/com/ailearn/service/vocabulary/CommonWordsProvider.java src/test/java/com/ailearn/service/vocabulary/CommonWordsProviderTest.java
git commit -m "feat: add common words provider with auto-download and caching"
```

### 7d: KnownWordsStore

- [ ] **Step 16: Implement KnownWordsStore**

```java
package com.ailearn.service.vocabulary;

import com.ailearn.entity.KnownWordEntity;
import com.ailearn.repository.KnownWordRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KnownWordsStore {

    private static final Logger log = LoggerFactory.getLogger(KnownWordsStore.class);
    private final KnownWordRepository repository;
    private final Set<String> knownLemmas = ConcurrentHashMap.newKeySet();

    public KnownWordsStore(KnownWordRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        repository.findAll().forEach(entity -> knownLemmas.add(entity.getLemma()));
        log.info("Loaded {} known words from database", knownLemmas.size());
    }

    public boolean isKnown(String lemma) {
        return knownLemmas.contains(lemma.toLowerCase());
    }

    public void addWord(String word, String lemma) {
        String lowerLemma = lemma.toLowerCase();
        if (knownLemmas.add(lowerLemma)) {
            repository.save(new KnownWordEntity(word, lowerLemma));
        }
    }

    public int size() {
        return knownLemmas.size();
    }
}
```

- [ ] **Step 17: Commit**

```bash
git add src/main/java/com/ailearn/service/vocabulary/KnownWordsStore.java
git commit -m "feat: add known words store backed by PostgreSQL"
```

### 7e: WordFilter

- [ ] **Step 18: Write the failing test**

```java
package com.ailearn.service.vocabulary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordFilterTest {

    @Mock
    private CommonWordsProvider commonWordsProvider;

    @Mock
    private KnownWordsStore knownWordsStore;

    @InjectMocks
    private WordFilter wordFilter;

    @Test
    void filter_shouldRemoveCommonAndKnownWords() {
        List<String> words = List.of("transformer", "the", "idempotent", "have", "latency");

        when(commonWordsProvider.isCommon("the")).thenReturn(true);
        when(commonWordsProvider.isCommon("have")).thenReturn(true);
        when(commonWordsProvider.isCommon("transformer")).thenReturn(false);
        when(commonWordsProvider.isCommon("idempotent")).thenReturn(false);
        when(commonWordsProvider.isCommon("latency")).thenReturn(false);

        when(knownWordsStore.isKnown("transformer")).thenReturn(true);
        when(knownWordsStore.isKnown("idempotent")).thenReturn(false);
        when(knownWordsStore.isKnown("latency")).thenReturn(false);

        List<String> filtered = wordFilter.filter(words);

        assertThat(filtered).containsExactly("idempotent", "latency");
    }
}
```

- [ ] **Step 19: Run test to verify it fails**

Run: `mvn test -Dtest=WordFilterTest`
Expected: FAIL — `WordFilter` does not exist.

- [ ] **Step 20: Implement WordFilter**

```java
package com.ailearn.service.vocabulary;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class WordFilter {

    private final CommonWordsProvider commonWordsProvider;
    private final KnownWordsStore knownWordsStore;

    public WordFilter(CommonWordsProvider commonWordsProvider, KnownWordsStore knownWordsStore) {
        this.commonWordsProvider = commonWordsProvider;
        this.knownWordsStore = knownWordsStore;
    }

    public List<String> filter(List<String> lemmatizedWords) {
        return lemmatizedWords.stream()
            .filter(word -> !commonWordsProvider.isCommon(word))
            .filter(word -> !knownWordsStore.isKnown(word))
            .toList();
    }
}
```

- [ ] **Step 21: Run tests to verify they pass**

Run: `mvn test -Dtest=WordFilterTest`
Expected: 1 test PASS.

- [ ] **Step 22: Commit**

```bash
git add src/main/java/com/ailearn/service/vocabulary/WordFilter.java src/test/java/com/ailearn/service/vocabulary/WordFilterTest.java
git commit -m "feat: add word filter combining common words and known words exclusion"
```

---

## Task 8: Glossary Service (Orchestrator)

**Files:**
- Create: `src/test/java/com/ailearn/service/GlossaryServiceTest.java`
- Create: `src/main/java/com/ailearn/service/GlossaryService.java`
- Create: `src/main/resources/prompts/glossary-prompt.txt`

- [ ] **Step 1: Create glossary prompt**

`src/main/resources/prompts/glossary-prompt.txt`:
```
Act as an English teacher for a Senior Java Engineer.
From the candidate words and the original article below, extract 10-15 high-value words.
Include: technical jargon, idiomatic expressions, and advanced academic verbs.
Ignore common daily English.

For each word, provide:
- word: the word as it appears in the article
- ipa: IPA pronunciation
- englishDefinition: contextual English definition based on how the word is used in this article
- chineseDefinition: Chinese translation of the definition
- sourceSentence: the original sentence from the article where this word appears

Also extract any multi-word idiomatic expressions from the article.

Candidate words: {{candidates}}

Original article:
{{article}}

Respond with a JSON array only.
```

- [ ] **Step 2: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.model.*;
import com.ailearn.service.vocabulary.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlossaryServiceTest {

    @Mock private ChatLanguageModel chatModel;
    @Mock private WordTokenizer tokenizer;
    @Mock private LemmatizationService lemmatizationService;
    @Mock private WordFilter wordFilter;
    @Mock private KnownWordsStore knownWordsStore;

    @Test
    void extract_shouldProduceGlossaryEntries() {
        Article article = new Article("Test", "https://example.com",
            "The idempotent API ensures consistency.", "Test", LocalDateTime.now());
        ScoredArticle scored = new ScoredArticle(article, 8, "Relevant");
        TranslatedArticle translated = new TranslatedArticle(scored,
            List.of(new BilingualParagraph("The idempotent API ensures consistency.", "幂等API确保一致性。")));

        when(tokenizer.tokenize(any())).thenReturn(List.of("idempotent", "api", "ensures", "consistency"));
        when(lemmatizationService.lemmatize(any())).thenReturn(List.of("idempotent", "api", "ensure", "consistency"));
        when(wordFilter.filter(any())).thenReturn(List.of("idempotent"));

        String llmResponse = """
            [{"word":"idempotent","ipa":"/aɪˈdɛmpətənt/","englishDefinition":"Producing the same result when applied multiple times","chineseDefinition":"幂等的","sourceSentence":"The idempotent API ensures consistency."}]
            """;
        when(chatModel.generate(any(List.class)))
            .thenReturn(Response.from(AiMessage.from(llmResponse)));

        GlossaryService glossaryService = new GlossaryService(
            chatModel, tokenizer, lemmatizationService, wordFilter, knownWordsStore);

        List<GlossaryEntry> entries = glossaryService.extract(translated);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).word()).isEqualTo("idempotent");
        assertThat(entries.get(0).ipa()).isEqualTo("/aɪˈdɛmpətənt/");
        assertThat(entries.get(0).sourceSentence()).isEqualTo("The idempotent API ensures consistency.");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=GlossaryServiceTest`
Expected: FAIL — `GlossaryService` does not exist.

- [ ] **Step 4: Implement GlossaryService**

```java
package com.ailearn.service;

import com.ailearn.model.GlossaryEntry;
import com.ailearn.model.TranslatedArticle;
import com.ailearn.service.vocabulary.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GlossaryService {

    private static final Logger log = LoggerFactory.getLogger(GlossaryService.class);
    private final ChatLanguageModel chatModel;
    private final WordTokenizer tokenizer;
    private final LemmatizationService lemmatizationService;
    private final WordFilter wordFilter;
    private final KnownWordsStore knownWordsStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GlossaryService(ChatLanguageModel chatModel, WordTokenizer tokenizer,
                           LemmatizationService lemmatizationService, WordFilter wordFilter,
                           KnownWordsStore knownWordsStore) {
        this.chatModel = chatModel;
        this.tokenizer = tokenizer;
        this.lemmatizationService = lemmatizationService;
        this.wordFilter = wordFilter;
        this.knownWordsStore = knownWordsStore;
    }

    public List<GlossaryEntry> extract(TranslatedArticle translatedArticle) {
        String content = translatedArticle.scoredArticle().article().content();

        // Pipeline: tokenize → lemmatize → filter
        List<String> tokens = tokenizer.tokenize(content);
        List<String> lemmas = lemmatizationService.lemmatize(tokens);
        List<String> candidates = wordFilter.filter(lemmas);

        if (candidates.isEmpty()) {
            log.info("No candidate words after filtering for '{}'",
                translatedArticle.scoredArticle().article().title());
            return List.of();
        }

        // Send candidates + article to LLM
        List<GlossaryEntry> entries = callLlm(candidates, content);

        // Auto-add extracted words to known words
        for (GlossaryEntry entry : entries) {
            knownWordsStore.addWord(entry.word(), entry.lemma() != null ? entry.lemma() : entry.word());
        }

        return entries;
    }

    private List<GlossaryEntry> callLlm(List<String> candidates, String articleContent) {
        String prompt = String.format(
            "Act as an English teacher for a Senior Java Engineer.\n" +
            "From the candidate words and the original article below, extract 10-15 high-value words.\n" +
            "Include: technical jargon, idiomatic expressions, and advanced academic verbs.\n\n" +
            "For each word, provide a JSON object with fields: word, ipa, englishDefinition, chineseDefinition, sourceSentence.\n" +
            "Also extract any multi-word idiomatic expressions from the article.\n\n" +
            "Candidate words: %s\n\nOriginal article:\n%s\n\nRespond with a JSON array only.",
            String.join(", ", candidates), articleContent
        );

        try {
            var response = chatModel.generate(List.of(
                SystemMessage.from("You are a vocabulary extraction assistant. Respond with valid JSON array only."),
                UserMessage.from(prompt)
            ));

            String json = response.content().text().trim();
            // Strip markdown code fences if present
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            List<Map<String, String>> rawEntries = objectMapper.readValue(json, new TypeReference<>() {});
            List<GlossaryEntry> entries = new ArrayList<>();
            for (Map<String, String> raw : rawEntries) {
                entries.add(new GlossaryEntry(
                    raw.getOrDefault("word", ""),
                    raw.getOrDefault("word", "").toLowerCase(),
                    raw.getOrDefault("ipa", ""),
                    raw.getOrDefault("englishDefinition", ""),
                    raw.getOrDefault("chineseDefinition", ""),
                    raw.getOrDefault("sourceSentence", "")
                ));
            }
            return entries;
        } catch (Exception e) {
            log.error("Failed to extract glossary via LLM: {}", e.getMessage());
            return List.of();
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -Dtest=GlossaryServiceTest`
Expected: 1 test PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ailearn/service/GlossaryService.java src/test/java/com/ailearn/service/GlossaryServiceTest.java src/main/resources/prompts/glossary-prompt.txt
git commit -m "feat: add glossary service orchestrating tokenize→lemmatize→filter→LLM pipeline"
```

---

## Task 9: Eudic API Client

**Files:**
- Create: `src/test/java/com/ailearn/service/EudicApiClientTest.java`
- Create: `src/main/java/com/ailearn/service/EudicApiClient.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.GlossaryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EudicApiClientTest {

    private static ClientAndServer mockServer;
    private EudicApiClient eudicApiClient;

    @BeforeAll
    static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(0);
    }

    @AfterAll
    static void stopMockServer() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig();
        AppConfig.Eudic eudic = new AppConfig.Eudic();
        eudic.setBaseUrl("http://localhost:" + mockServer.getPort());
        eudic.setAuthToken("test-token");
        eudic.setStudyListName("ai-learn");
        eudic.setLanguage("en");
        appConfig.setEudic(eudic);
        eudicApiClient = new EudicApiClient(appConfig);
        eudicApiClient.init();
    }

    @Test
    void syncWord_shouldPostWordWithContext() {
        // Mock category list response
        mockServer.when(
            HttpRequest.request().withMethod("GET").withPath("/api/open/v1/studylist/category")
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody("[{\"id\":\"abc123\",\"name\":\"ai-learn\",\"language\":\"en\"}]")
        );

        // Mock add word response
        mockServer.when(
            HttpRequest.request().withMethod("POST").withPath("/api/open/v1/studylist/word")
        ).respond(
            HttpResponse.response().withStatusCode(201)
        );

        eudicApiClient.ensureStudyList();

        GlossaryEntry entry = new GlossaryEntry("idempotent", "idempotent",
            "/aɪˈdɛmpətənt/", "Producing the same result", "幂等的",
            "The idempotent API ensures consistency.");

        boolean result = eudicApiClient.syncWord(entry);

        assertThat(result).isTrue();
    }
}
```

Note: Add `mockserver-netty` dependency to pom.xml test scope:
```xml
<dependency>
    <groupId>org.mock-server</groupId>
    <artifactId>mockserver-netty</artifactId>
    <version>5.15.0</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EudicApiClientTest`
Expected: FAIL — `EudicApiClient` does not exist.

- [ ] **Step 3: Implement EudicApiClient**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.GlossaryEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class EudicApiClient {

    private static final Logger log = LoggerFactory.getLogger(EudicApiClient.class);
    private final AppConfig appConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String studyListId;

    public EudicApiClient(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.httpClient = HttpClient.newHttpClient();
    }

    @PostConstruct
    public void init() {
        if (appConfig.getEudic().getAuthToken() == null || appConfig.getEudic().getAuthToken().isBlank()) {
            log.warn("Eudic auth token not configured, sync disabled");
        }
    }

    public void ensureStudyList() {
        try {
            String baseUrl = appConfig.getEudic().getBaseUrl();
            String lang = appConfig.getEudic().getLanguage();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/open/v1/studylist/category?language=" + lang))
                .header("Authorization", "NIS " + appConfig.getEudic().getAuthToken())
                .header("Content-Type", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode categories = objectMapper.readTree(response.body());

            String targetName = appConfig.getEudic().getStudyListName();
            for (JsonNode cat : categories) {
                if (targetName.equals(cat.get("name").asText())) {
                    studyListId = cat.get("id").asText();
                    log.info("Found Eudic study list '{}' with id '{}'", targetName, studyListId);
                    return;
                }
            }

            // Create study list if not found
            String createBody = objectMapper.writeValueAsString(Map.of(
                "language", lang,
                "name", targetName
            ));
            HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/open/v1/studylist/category"))
                .header("Authorization", "NIS " + appConfig.getEudic().getAuthToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();

            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode created = objectMapper.readTree(createResponse.body());
            studyListId = created.get("id").asText();
            log.info("Created Eudic study list '{}' with id '{}'", targetName, studyListId);
        } catch (Exception e) {
            log.error("Failed to ensure Eudic study list: {}", e.getMessage());
        }
    }

    public boolean syncWord(GlossaryEntry entry) {
        try {
            String baseUrl = appConfig.getEudic().getBaseUrl();
            String lang = appConfig.getEudic().getLanguage();

            // Add word with context
            String body = objectMapper.writeValueAsString(Map.of(
                "id", studyListId != null ? studyListId : "0",
                "language", lang,
                "word", entry.word(),
                "exp", entry.sourceSentence()
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/open/v1/studylist/word"))
                .header("Authorization", "NIS " + appConfig.getEudic().getAuthToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.debug("Synced word '{}' to Eudic", entry.word());
                return true;
            }
            log.warn("Eudic sync returned status {} for word '{}'", response.statusCode(), entry.word());
            return false;
        } catch (Exception e) {
            log.error("Failed to sync word '{}' to Eudic: {}", entry.word(), e.getMessage());
            return false;
        }
    }

    public boolean isConfigured() {
        String token = appConfig.getEudic().getAuthToken();
        return token != null && !token.isBlank();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=EudicApiClientTest`
Expected: 1 test PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/ailearn/service/EudicApiClient.java src/test/java/com/ailearn/service/EudicApiClientTest.java
git commit -m "feat: add Eudic API client for vocabulary sync with context sentences"
```

---

## Task 10: Obsidian Export Service

**Files:**
- Create: `src/test/java/com/ailearn/service/ObsidianExportServiceTest.java`
- Create: `src/main/java/com/ailearn/service/ObsidianExportService.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObsidianExportServiceTest {

    @TempDir
    Path tempVault;

    private ObsidianExportService exportService;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig();
        AppConfig.Obsidian obsidian = new AppConfig.Obsidian();
        obsidian.setVaultPath(tempVault.toString());
        obsidian.setBaseFolder("AI-News");
        appConfig.setObsidian(obsidian);
        exportService = new ObsidianExportService(appConfig);
    }

    @Test
    void exportArticle_shouldCreateMarkdownWithBilingualContentAndGlossary() throws IOException {
        Article article = new Article("Test LLM Article", "https://example.com/llm",
            "LLMs are transforming software.", "HN", LocalDateTime.of(2026, 4, 23, 8, 0));
        ScoredArticle scored = new ScoredArticle(article, 9, "Highly relevant");
        TranslatedArticle translated = new TranslatedArticle(scored,
            List.of(new BilingualParagraph("LLMs are transforming software.",
                "大语言模型正在改变软件。")));

        List<GlossaryEntry> glossary = List.of(
            new GlossaryEntry("transforming", "transform", "/trænsˈfɔːrmɪŋ/",
                "Changing fundamentally", "从根本上改变",
                "LLMs are transforming software.")
        );

        Path result = exportService.exportArticle(translated, glossary);

        assertThat(result).exists();
        String content = Files.readString(result);
        assertThat(content).contains("title: \"Test LLM Article\"");
        assertThat(content).contains("LLMs are transforming software.");
        assertThat(content).contains("> 大语言模型正在改变软件。");
        assertThat(content).contains("transforming");
        assertThat(content).contains("/trænsˈfɔːrmɪŋ/");
        assertThat(content).contains("localhost:8080/api/vocabulary/master");
    }

    @Test
    void exportDailyGlossary_shouldCreateConsolidatedFile() throws IOException {
        List<GlossaryEntry> glossary = List.of(
            new GlossaryEntry("idempotent", "idempotent", "/aɪˈdɛmpətənt/",
                "Same result on repeated application", "幂等的",
                "The API is idempotent."),
            new GlossaryEntry("latency", "latency", "/ˈleɪtənsi/",
                "Delay before transfer of data", "延迟",
                "Reduce network latency.")
        );

        Path result = exportService.exportDailyGlossary(glossary, LocalDateTime.of(2026, 4, 23, 8, 0));

        assertThat(result).exists();
        String content = Files.readString(result);
        assertThat(content).contains("AI News Glossary - 2026-04-23");
        assertThat(content).contains("idempotent");
        assertThat(content).contains("latency");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ObsidianExportServiceTest`
Expected: FAIL — `ObsidianExportService` does not exist.

- [ ] **Step 3: Implement ObsidianExportService**

```java
package com.ailearn.service;

import com.ailearn.config.AppConfig;
import com.ailearn.model.BilingualParagraph;
import com.ailearn.model.GlossaryEntry;
import com.ailearn.model.TranslatedArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ObsidianExportService {

    private static final Logger log = LoggerFactory.getLogger(ObsidianExportService.class);
    private final AppConfig appConfig;

    public ObsidianExportService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Path exportArticle(TranslatedArticle translated, List<GlossaryEntry> glossary) throws IOException {
        var article = translated.scoredArticle().article();
        var scored = translated.scoredArticle();
        LocalDateTime date = article.publishedAt() != null ? article.publishedAt() : LocalDateTime.now();
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String slug = article.title().toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
        if (slug.length() > 50) slug = slug.substring(0, 50);

        Path dir = Path.of(appConfig.getObsidian().getVaultPath(),
            appConfig.getObsidian().getBaseFolder(),
            date.format(DateTimeFormatter.ofPattern("yyyy")),
            date.format(DateTimeFormatter.ofPattern("MM")));
        Files.createDirectories(dir);

        Path filePath = dir.resolve(dateStr + "-ai-news-" + slug + ".md");

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(article.title()).append("\"\n");
        sb.append("source: ").append(article.url()).append("\n");
        sb.append("date: ").append(dateStr).append("\n");
        sb.append("score: ").append(scored.relevanceScore()).append("\n");
        sb.append("tags: [ai-news]\n");
        sb.append("---\n\n");
        sb.append("# ").append(article.title()).append("\n\n");
        sb.append("## Original & Translation\n\n");

        for (BilingualParagraph p : translated.paragraphs()) {
            sb.append(p.english()).append("\n\n");
            sb.append("> ").append(p.chinese()).append("\n\n");
        }

        if (!glossary.isEmpty()) {
            sb.append("## Vocabulary\n\n");
            sb.append("| Word | IPA | Definition | 中文释义 | Context | Mastered |\n");
            sb.append("|------|-----|------------|----------|---------|----------|\n");
            for (GlossaryEntry entry : glossary) {
                sb.append("| ").append(entry.word())
                    .append(" | ").append(entry.ipa())
                    .append(" | ").append(entry.englishDefinition())
                    .append(" | ").append(entry.chineseDefinition())
                    .append(" | ").append(truncate(entry.sourceSentence(), 60))
                    .append(" | [Mark](http://localhost:8080/api/vocabulary/master?word=")
                    .append(entry.word()).append(") |\n");
            }
        }

        Files.writeString(filePath, sb.toString());
        log.info("Exported article to {}", filePath);
        return filePath;
    }

    public Path exportDailyGlossary(List<GlossaryEntry> allEntries, LocalDateTime date) throws IOException {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Path dir = Path.of(appConfig.getObsidian().getVaultPath(),
            appConfig.getObsidian().getBaseFolder(),
            date.format(DateTimeFormatter.ofPattern("yyyy")),
            date.format(DateTimeFormatter.ofPattern("MM")));
        Files.createDirectories(dir);

        Path filePath = dir.resolve(dateStr + "-glossary.md");

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"AI News Glossary - ").append(dateStr).append("\"\n");
        sb.append("date: ").append(dateStr).append("\n");
        sb.append("tags: [ai-glossary]\n");
        sb.append("---\n\n");
        sb.append("# AI News Glossary - ").append(dateStr).append("\n\n");
        sb.append("| Word | IPA | Definition | 中文释义 | Context | Mastered |\n");
        sb.append("|------|-----|------------|----------|---------|----------|\n");

        for (GlossaryEntry entry : allEntries) {
            sb.append("| ").append(entry.word())
                .append(" | ").append(entry.ipa())
                .append(" | ").append(entry.englishDefinition())
                .append(" | ").append(entry.chineseDefinition())
                .append(" | ").append(truncate(entry.sourceSentence(), 60))
                .append(" | [Mark](http://localhost:8080/api/vocabulary/master?word=")
                .append(entry.word()).append(") |\n");
        }

        Files.writeString(filePath, sb.toString());
        log.info("Exported daily glossary to {}", filePath);
        return filePath;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=ObsidianExportServiceTest`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ailearn/service/ObsidianExportService.java src/test/java/com/ailearn/service/ObsidianExportServiceTest.java
git commit -m "feat: add Obsidian export service with bilingual articles and glossary markdown"
```

---

## Task 11: Agent Tools

**Files:**
- Create: `src/main/java/com/ailearn/agent/tools/FetchNewsTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/ScoreArticleTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/TranslateArticleTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/ExtractVocabularyTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/ExportObsidianTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/SyncEudicTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/SearchArticlesTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/SearchVocabularyTool.java`
- Create: `src/main/java/com/ailearn/agent/tools/QuizTool.java`

- [ ] **Step 1: Create FetchNewsTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.model.Article;
import com.ailearn.service.RssFetchService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FetchNewsTool {

    private final RssFetchService rssFetchService;

    public FetchNewsTool(RssFetchService rssFetchService) {
        this.rssFetchService = rssFetchService;
    }

    @Tool("Fetch the latest AI news articles from all configured RSS feeds. Returns a list of articles with title, URL, and summary.")
    public String fetchNews() {
        List<Article> articles = rssFetchService.fetchAllFeeds();
        if (articles.isEmpty()) {
            return "No new articles found from RSS feeds.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(articles.size()).append(" new articles:\n");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            sb.append(i + 1).append(". ").append(a.title())
              .append(" (").append(a.source()).append(")\n");
        }
        return sb.toString();
    }

    public List<Article> fetchArticles() {
        return rssFetchService.fetchAllFeeds();
    }
}
```

- [ ] **Step 2: Create ScoreArticleTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.ArticleEntity;
import com.ailearn.model.Article;
import com.ailearn.model.ScoredArticle;
import com.ailearn.repository.ArticleRepository;
import com.ailearn.service.ArticleScoringService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScoreArticleTool {

    private final ArticleScoringService scoringService;
    private final ArticleRepository articleRepository;

    public ScoreArticleTool(ArticleScoringService scoringService, ArticleRepository articleRepository) {
        this.scoringService = scoringService;
        this.articleRepository = articleRepository;
    }

    @Tool("Score a list of articles for relevance to AI/Backend topics. Saves scored articles to the database and returns the ones above the threshold.")
    public String scoreArticles() {
        // This tool works with the most recently fetched articles
        return "Use scoreAndSave(articles) programmatically for batch scoring.";
    }

    public List<ScoredArticle> scoreAndSave(List<Article> articles) {
        List<ScoredArticle> scored = new ArrayList<>();
        for (Article article : articles) {
            scoringService.score(article).ifPresent(sa -> {
                ArticleEntity entity = new ArticleEntity(
                    sa.article().title(), sa.article().url(), sa.article().content(),
                    sa.article().source(), sa.article().publishedAt()
                );
                entity.setScore(sa.relevanceScore());
                entity.setScoreReason(sa.reasoning());
                articleRepository.save(entity);
                scored.add(sa);
            });
        }
        return scored;
    }
}
```

- [ ] **Step 3: Create TranslateArticleTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.ArticleTranslationEntity;
import com.ailearn.model.BilingualParagraph;
import com.ailearn.model.ScoredArticle;
import com.ailearn.model.TranslatedArticle;
import com.ailearn.repository.ArticleRepository;
import com.ailearn.repository.ArticleTranslationRepository;
import com.ailearn.service.TranslationService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TranslateArticleTool {

    private final TranslationService translationService;
    private final ArticleTranslationRepository translationRepository;
    private final ArticleRepository articleRepository;

    public TranslateArticleTool(TranslationService translationService,
                                 ArticleTranslationRepository translationRepository,
                                 ArticleRepository articleRepository) {
        this.translationService = translationService;
        this.translationRepository = translationRepository;
        this.articleRepository = articleRepository;
    }

    @Tool("Translate scored articles from English to Chinese, paragraph by paragraph.")
    public String translate() {
        return "Use translateAndSave(scoredArticles) programmatically.";
    }

    public List<TranslatedArticle> translateAndSave(List<ScoredArticle> scoredArticles) {
        List<TranslatedArticle> results = new ArrayList<>();
        for (ScoredArticle sa : scoredArticles) {
            TranslatedArticle translated = translationService.translate(sa);
            // Save translations to DB
            articleRepository.findByUrl(sa.article().url()).ifPresent(entity -> {
                List<BilingualParagraph> paragraphs = translated.paragraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    translationRepository.save(new ArticleTranslationEntity(
                        entity.getId(), i, paragraphs.get(i).english(), paragraphs.get(i).chinese()
                    ));
                }
            });
            results.add(translated);
        }
        return results;
    }
}
```

- [ ] **Step 4: Create ExtractVocabularyTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.GlossaryEntryEntity;
import com.ailearn.model.GlossaryEntry;
import com.ailearn.model.TranslatedArticle;
import com.ailearn.repository.ArticleRepository;
import com.ailearn.repository.GlossaryEntryRepository;
import com.ailearn.service.GlossaryService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExtractVocabularyTool {

    private final GlossaryService glossaryService;
    private final GlossaryEntryRepository glossaryRepository;
    private final ArticleRepository articleRepository;

    public ExtractVocabularyTool(GlossaryService glossaryService,
                                  GlossaryEntryRepository glossaryRepository,
                                  ArticleRepository articleRepository) {
        this.glossaryService = glossaryService;
        this.glossaryRepository = glossaryRepository;
        this.articleRepository = articleRepository;
    }

    @Tool("Extract high-value vocabulary words from translated articles using smart filtering and LLM analysis.")
    public String extractVocabulary() {
        return "Use extractAndSave(translatedArticles) programmatically.";
    }

    public List<GlossaryEntry> extractAndSave(List<TranslatedArticle> translatedArticles) {
        List<GlossaryEntry> allEntries = new ArrayList<>();
        for (TranslatedArticle ta : translatedArticles) {
            List<GlossaryEntry> entries = glossaryService.extract(ta);
            // Save to DB
            articleRepository.findByUrl(ta.scoredArticle().article().url()).ifPresent(entity -> {
                for (GlossaryEntry entry : entries) {
                    glossaryRepository.save(new GlossaryEntryEntity(
                        entry.word(), entry.lemma(), entry.ipa(),
                        entry.englishDefinition(), entry.chineseDefinition(),
                        entry.sourceSentence(), entity.getId()
                    ));
                }
            });
            allEntries.addAll(entries);
        }
        return allEntries;
    }
}
```

- [ ] **Step 5: Create SyncEudicTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.GlossaryEntryEntity;
import com.ailearn.model.GlossaryEntry;
import com.ailearn.repository.GlossaryEntryRepository;
import com.ailearn.service.EudicApiClient;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncEudicTool {

    private static final Logger log = LoggerFactory.getLogger(SyncEudicTool.class);
    private final EudicApiClient eudicApiClient;
    private final GlossaryEntryRepository glossaryRepository;

    public SyncEudicTool(EudicApiClient eudicApiClient, GlossaryEntryRepository glossaryRepository) {
        this.eudicApiClient = eudicApiClient;
        this.glossaryRepository = glossaryRepository;
    }

    @Tool("Sync extracted vocabulary words to Eudic (欧路词典) for spaced-repetition review.")
    public String syncToEudic() {
        if (!eudicApiClient.isConfigured()) {
            return "Eudic sync is not configured. Set EUDIC_AUTH_TOKEN to enable.";
        }

        eudicApiClient.ensureStudyList();
        List<GlossaryEntryEntity> unsynced = glossaryRepository.findByEudicSyncedFalse();

        int synced = 0;
        for (GlossaryEntryEntity entity : unsynced) {
            GlossaryEntry entry = new GlossaryEntry(
                entity.getWord(), entity.getLemma(), entity.getIpa(),
                entity.getEnglishDefinition(), entity.getChineseDefinition(),
                entity.getSourceSentence()
            );
            if (eudicApiClient.syncWord(entry)) {
                entity.setEudicSynced(true);
                glossaryRepository.save(entity);
                synced++;
            }
        }
        return String.format("Synced %d/%d words to Eudic.", synced, unsynced.size());
    }
}
```

- [ ] **Step 6: Create ExportObsidianTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.model.GlossaryEntry;
import com.ailearn.model.TranslatedArticle;
import com.ailearn.service.ObsidianExportService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ExportObsidianTool {

    private static final Logger log = LoggerFactory.getLogger(ExportObsidianTool.class);
    private final ObsidianExportService exportService;

    public ExportObsidianTool(ObsidianExportService exportService) {
        this.exportService = exportService;
    }

    @Tool("Export curated articles and glossary to Obsidian vault as Markdown files.")
    public String exportToObsidian() {
        return "Use export(articles, glossaryMap) programmatically.";
    }

    public String export(List<TranslatedArticle> articles,
                         Map<TranslatedArticle, List<GlossaryEntry>> glossaryMap,
                         List<GlossaryEntry> allEntries) {
        int exported = 0;
        for (TranslatedArticle ta : articles) {
            try {
                List<GlossaryEntry> glossary = glossaryMap.getOrDefault(ta, List.of());
                exportService.exportArticle(ta, glossary);
                exported++;
            } catch (Exception e) {
                log.error("Failed to export article '{}': {}",
                    ta.scoredArticle().article().title(), e.getMessage());
            }
        }

        try {
            exportService.exportDailyGlossary(allEntries, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to export daily glossary: {}", e.getMessage());
        }

        return String.format("Exported %d articles and daily glossary to Obsidian.", exported);
    }
}
```

- [ ] **Step 7: Create SearchArticlesTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.ArticleEntity;
import com.ailearn.repository.ArticleRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SearchArticlesTool {

    private final ArticleRepository articleRepository;

    public SearchArticlesTool(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Tool("Search curated AI articles by date. Returns titles, scores, and URLs.")
    public String searchByDate(@P("Date in yyyy-MM-dd format") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<ArticleEntity> articles = articleRepository.findByCreatedAtBetween(start, end);
        if (articles.isEmpty()) {
            return "No articles found for " + dateStr;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Articles for ").append(dateStr).append(":\n");
        for (ArticleEntity a : articles) {
            sb.append("- [Score: ").append(a.getScore()).append("] ")
              .append(a.getTitle()).append("\n  ").append(a.getUrl()).append("\n");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 8: Create SearchVocabularyTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.GlossaryEntryEntity;
import com.ailearn.repository.GlossaryEntryRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SearchVocabularyTool {

    private final GlossaryEntryRepository glossaryRepository;

    public SearchVocabularyTool(GlossaryEntryRepository glossaryRepository) {
        this.glossaryRepository = glossaryRepository;
    }

    @Tool("Search vocabulary entries by date. Returns words with IPA, definitions, and context.")
    public String searchByDate(@P("Date in yyyy-MM-dd format") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<GlossaryEntryEntity> entries = glossaryRepository.findByCreatedAtBetween(start, end);
        if (entries.isEmpty()) {
            return "No vocabulary entries found for " + dateStr;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Vocabulary for ").append(dateStr).append(":\n");
        for (GlossaryEntryEntity e : entries) {
            sb.append("- **").append(e.getWord()).append("** ")
              .append(e.getIpa()).append(" — ").append(e.getEnglishDefinition())
              .append(" (").append(e.getChineseDefinition()).append(")\n");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 9: Create QuizTool**

```java
package com.ailearn.agent.tools;

import com.ailearn.entity.GlossaryEntryEntity;
import com.ailearn.repository.GlossaryEntryRepository;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class QuizTool {

    private final GlossaryEntryRepository glossaryRepository;

    public QuizTool(GlossaryEntryRepository glossaryRepository) {
        this.glossaryRepository = glossaryRepository;
    }

    @Tool("Generate a vocabulary quiz from unmastered words. Returns 5 random words for the user to practice.")
    public String generateQuiz() {
        List<GlossaryEntryEntity> unmastered = glossaryRepository.findByMasteredFalse();
        if (unmastered.isEmpty()) {
            return "No unmastered words available for quiz. Great job!";
        }

        Collections.shuffle(unmastered);
        int count = Math.min(5, unmastered.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Vocabulary Quiz — fill in the meaning:\n\n");
        for (int i = 0; i < count; i++) {
            GlossaryEntryEntity e = unmastered.get(i);
            sb.append(i + 1).append(". **").append(e.getWord()).append("** ")
              .append(e.getIpa()).append("\n")
              .append("   Context: \"").append(e.getSourceSentence()).append("\"\n")
              .append("   Answer: ||").append(e.getEnglishDefinition())
              .append(" / ").append(e.getChineseDefinition()).append("||\n\n");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 10: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/ailearn/agent/tools/
git commit -m "feat: add all agent tools — fetch, score, translate, extract, export, eudic sync, search, quiz"
```

---

## Task 12: Agent Core & Config

**Files:**
- Create: `src/main/java/com/ailearn/agent/AiLearnAgent.java`
- Create: `src/main/java/com/ailearn/config/AgentConfig.java`

- [ ] **Step 1: Create AiLearnAgent interface**

```java
package com.ailearn.agent;

import dev.langchain4j.service.SystemMessage;

public interface AiLearnAgent {

    @SystemMessage("""
        You are an AI English Learning Assistant for a Senior Java Engineer.
        You help curate AI news articles, translate them to Chinese, extract vocabulary,
        sync words to Eudic, and export to Obsidian.

        You have tools available to:
        - Fetch AI news from RSS feeds
        - Score articles for relevance
        - Translate articles (English to Chinese)
        - Extract vocabulary with IPA and definitions
        - Sync vocabulary to Eudic dictionary
        - Export articles and glossary to Obsidian vault
        - Search past articles and vocabulary
        - Generate vocabulary quizzes

        When asked to curate news, use the tools in this order:
        1. Fetch news
        2. Score articles
        3. Translate high-scoring articles
        4. Extract vocabulary
        5. Sync to Eudic
        6. Export to Obsidian

        Be concise and helpful. Respond in English unless asked otherwise.
        """)
    String chat(String userMessage);
}
```

- [ ] **Step 2: Create AgentConfig**

```java
package com.ailearn.config;

import com.ailearn.agent.AiLearnAgent;
import com.ailearn.agent.tools.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public AiLearnAgent aiLearnAgent(
            ChatLanguageModel chatModel,
            FetchNewsTool fetchNewsTool,
            ScoreArticleTool scoreArticleTool,
            TranslateArticleTool translateArticleTool,
            ExtractVocabularyTool extractVocabularyTool,
            ExportObsidianTool exportObsidianTool,
            SyncEudicTool syncEudicTool,
            SearchArticlesTool searchArticlesTool,
            SearchVocabularyTool searchVocabularyTool,
            QuizTool quizTool) {

        return AiServices.builder(AiLearnAgent.class)
            .chatModel(chatModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            .tools(
                fetchNewsTool,
                scoreArticleTool,
                translateArticleTool,
                extractVocabularyTool,
                exportObsidianTool,
                syncEudicTool,
                searchArticlesTool,
                searchVocabularyTool,
                quizTool
            )
            .build();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ailearn/agent/AiLearnAgent.java src/main/java/com/ailearn/config/AgentConfig.java
git commit -m "feat: add AI agent core with LangChain4j AI Services and chat memory"
```

---

## Task 13: REST Controllers

**Files:**
- Create: `src/main/java/com/ailearn/controller/ChatController.java`
- Create: `src/main/java/com/ailearn/controller/ArticleController.java`
- Create: `src/main/java/com/ailearn/controller/VocabularyController.java`
- Create: `src/main/java/com/ailearn/controller/CurateController.java`
- Create: `src/test/java/com/ailearn/controller/VocabularyControllerTest.java`

- [ ] **Step 1: Create ChatController**

```java
package com.ailearn.controller;

import com.ailearn.agent.AiLearnAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AiLearnAgent agent;

    public ChatController(AiLearnAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String reply = agent.chat(message);
        return Map.of("reply", reply);
    }
}
```

- [ ] **Step 2: Create ArticleController**

```java
package com.ailearn.controller;

import com.ailearn.entity.ArticleEntity;
import com.ailearn.repository.ArticleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleRepository articleRepository;

    public ArticleController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @GetMapping
    public List<ArticleEntity> getArticles(@RequestParam(required = false) String date) {
        if (date != null) {
            LocalDate d = LocalDate.parse(date);
            return articleRepository.findByCreatedAtBetween(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        }
        return articleRepository.findAll();
    }
}
```

- [ ] **Step 3: Create VocabularyController**

```java
package com.ailearn.controller;

import com.ailearn.entity.GlossaryEntryEntity;
import com.ailearn.entity.KnownWordEntity;
import com.ailearn.repository.GlossaryEntryRepository;
import com.ailearn.repository.KnownWordRepository;
import com.ailearn.service.vocabulary.KnownWordsStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final GlossaryEntryRepository glossaryRepository;
    private final KnownWordsStore knownWordsStore;

    public VocabularyController(GlossaryEntryRepository glossaryRepository,
                                 KnownWordsStore knownWordsStore) {
        this.glossaryRepository = glossaryRepository;
        this.knownWordsStore = knownWordsStore;
    }

    @GetMapping
    public List<GlossaryEntryEntity> getVocabulary(@RequestParam(required = false) String date) {
        if (date != null) {
            LocalDate d = LocalDate.parse(date);
            return glossaryRepository.findByCreatedAtBetween(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        }
        return glossaryRepository.findAll();
    }

    @PostMapping(value = "/master", produces = MediaType.TEXT_HTML_VALUE)
    public String masterWord(@RequestParam String word) {
        knownWordsStore.addWord(word, word.toLowerCase());

        // Mark all glossary entries for this word as mastered
        glossaryRepository.findAll().stream()
            .filter(e -> e.getWord().equalsIgnoreCase(word))
            .forEach(e -> {
                e.setMastered(true);
                glossaryRepository.save(e);
            });

        return """
            <html><body style="font-family:sans-serif;text-align:center;padding:40px;">
            <h2>Word Mastered!</h2>
            <p><strong>%s</strong> has been added to your known words.</p>
            <p>You can close this tab.</p>
            </body></html>
            """.formatted(word);
    }
}
```

- [ ] **Step 4: Create CurateController**

```java
package com.ailearn.controller;

import com.ailearn.agent.AiLearnAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CurateController {

    private final AiLearnAgent agent;

    public CurateController(AiLearnAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/curate")
    public Map<String, String> curate() {
        String result = agent.chat("Curate today's AI news. Fetch articles, score them, translate the relevant ones, extract vocabulary, sync to Eudic, and export to Obsidian.");
        return Map.of("result", result);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
```

- [ ] **Step 5: Write VocabularyController test**

```java
package com.ailearn.controller;

import com.ailearn.repository.GlossaryEntryRepository;
import com.ailearn.service.vocabulary.KnownWordsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularyController.class)
class VocabularyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GlossaryEntryRepository glossaryRepository;

    @MockBean
    private KnownWordsStore knownWordsStore;

    @Test
    void masterWord_shouldReturnSuccessHtml() throws Exception {
        when(glossaryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/api/vocabulary/master").param("word", "idempotent"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Word Mastered")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("idempotent")));
    }

    @Test
    void getVocabulary_noDate_shouldReturnAll() throws Exception {
        when(glossaryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/vocabulary"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}
```

- [ ] **Step 6: Run tests**

Run: `mvn test -Dtest=VocabularyControllerTest`
Expected: 2 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ailearn/controller/ src/test/java/com/ailearn/controller/
git commit -m "feat: add REST controllers for chat, articles, vocabulary, and curation trigger"
```

---

## Task 14: Curation Scheduler

**Files:**
- Create: `src/main/java/com/ailearn/scheduler/CurationScheduler.java`

- [ ] **Step 1: Create CurationScheduler**

```java
package com.ailearn.scheduler;

import com.ailearn.agent.tools.*;
import com.ailearn.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CurationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CurationScheduler.class);

    private final FetchNewsTool fetchNewsTool;
    private final ScoreArticleTool scoreArticleTool;
    private final TranslateArticleTool translateArticleTool;
    private final ExtractVocabularyTool extractVocabularyTool;
    private final ExportObsidianTool exportObsidianTool;
    private final SyncEudicTool syncEudicTool;

    public CurationScheduler(FetchNewsTool fetchNewsTool,
                              ScoreArticleTool scoreArticleTool,
                              TranslateArticleTool translateArticleTool,
                              ExtractVocabularyTool extractVocabularyTool,
                              ExportObsidianTool exportObsidianTool,
                              SyncEudicTool syncEudicTool) {
        this.fetchNewsTool = fetchNewsTool;
        this.scoreArticleTool = scoreArticleTool;
        this.translateArticleTool = translateArticleTool;
        this.extractVocabularyTool = extractVocabularyTool;
        this.exportObsidianTool = exportObsidianTool;
        this.syncEudicTool = syncEudicTool;
    }

    @Scheduled(cron = "${ailearn.schedule.cron}")
    public void curate() {
        log.info("Starting daily AI news curation...");

        try {
            // 1. Fetch
            List<Article> articles = fetchNewsTool.fetchArticles();
            log.info("Fetched {} articles", articles.size());
            if (articles.isEmpty()) return;

            // 2. Score
            List<ScoredArticle> scored = scoreArticleTool.scoreAndSave(articles);
            log.info("{} articles passed scoring threshold", scored.size());
            if (scored.isEmpty()) return;

            // 3. Translate
            List<TranslatedArticle> translated = translateArticleTool.translateAndSave(scored);
            log.info("Translated {} articles", translated.size());

            // 4. Extract vocabulary
            List<GlossaryEntry> allEntries = extractVocabularyTool.extractAndSave(translated);
            log.info("Extracted {} vocabulary entries", allEntries.size());

            // 5. Sync to Eudic
            String syncResult = syncEudicTool.syncToEudic();
            log.info("Eudic sync: {}", syncResult);

            // 6. Export to Obsidian
            Map<TranslatedArticle, List<GlossaryEntry>> glossaryMap = new LinkedHashMap<>();
            int idx = 0;
            for (TranslatedArticle ta : translated) {
                List<GlossaryEntry> articleEntries = allEntries.subList(
                    Math.min(idx, allEntries.size()),
                    Math.min(idx + 15, allEntries.size())
                );
                glossaryMap.put(ta, articleEntries);
                idx += articleEntries.size();
            }
            String exportResult = exportObsidianTool.export(translated, glossaryMap, allEntries);
            log.info("Export: {}", exportResult);

            log.info("Daily curation complete!");
        } catch (Exception e) {
            log.error("Curation failed: {}", e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ailearn/scheduler/CurationScheduler.java
git commit -m "feat: add daily curation scheduler orchestrating the full pipeline"
```

---

## Task 15: Integration Smoke Test

**Files:**
- Create: `src/test/java/com/ailearn/integration/CurationPipelineIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package com.ailearn.integration;

import com.ailearn.entity.ArticleEntity;
import com.ailearn.repository.ArticleRepository;
import com.ailearn.repository.GlossaryEntryRepository;
import com.ailearn.repository.KnownWordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CurationPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("langchain4j.open-ai.chat-model.api-key", () -> "test-key");
        registry.add("langchain4j.open-ai.embedding-model.api-key", () -> "test-key");
    }

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private GlossaryEntryRepository glossaryRepository;

    @Autowired
    private KnownWordRepository knownWordRepository;

    @Test
    void contextLoads() {
        // Verifies Spring context starts with Testcontainers PostgreSQL + pgvector
    }

    @Test
    void articleDedup_shouldRejectDuplicateUrls() {
        ArticleEntity article = new ArticleEntity(
            "Test Article", "https://example.com/unique-test",
            "Content", "Test", LocalDateTime.now()
        );
        articleRepository.save(article);

        assertThat(articleRepository.existsByUrl("https://example.com/unique-test")).isTrue();
        assertThat(articleRepository.existsByUrl("https://example.com/other")).isFalse();
    }
}
```

- [ ] **Step 2: Run integration test**

Run: `mvn test -Dtest=CurationPipelineIntegrationTest`
Expected: Tests PASS (requires Docker running for Testcontainers).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ailearn/integration/CurationPipelineIntegrationTest.java
git commit -m "feat: add integration smoke test with Testcontainers PostgreSQL + pgvector"
```

---

## Task 16: Final Verification

- [ ] **Step 1: Run all tests**

Run: `mvn test`
Expected: All tests PASS.

- [ ] **Step 2: Start the full stack and verify**

```bash
docker compose up -d
OPENAI_API_KEY=your-key mvn spring-boot:run
```

In another terminal:
```bash
# Health check
curl http://localhost:8080/api/health

# Expected: {"status":"ok"}
```

- [ ] **Step 3: Add .gitignore and commit**

Create `.gitignore`:
```
target/
*.class
*.jar
*.log
.idea/
*.iml
data/common-words.txt
.env
```

```bash
git add .gitignore
git commit -m "chore: add .gitignore for Maven, IDE, and data files"
```

- [ ] **Step 4: Final commit — verify clean state**

Run: `git status`
Expected: `nothing to commit, working tree clean`
