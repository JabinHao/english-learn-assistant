# English Learn Assistant

> AI 驱动的英语精读助手 —— 每日 AI 资讯精选、双语对照学习、词汇提取并同步至欧路词典，配套文章导师智能体。

**Languages:** [English](README.md) · [简体中文](README.zh-CN.md)

---

## 项目简介

English Learn Assistant 通过精选 AI 资讯帮助你学习英语。系统每天抓取并排序 AI 相关文章，由你挑选其中一篇，再将其转化为结构化的学习材料：双语段落对照、词汇提取，以及一个能围绕该文章答疑的导师智能体。

它**不是**一个全自动的新闻摘要机器人，而是聚焦于「每天挑一篇 → 精读 → 学词汇 → 与导师讨论」这个学习流程的工具。

## 主要功能

- **每日候选文章管线** —— RSS 抓取 → 关键词粗筛 → LLM 重排序 → 每日 3–5 篇推荐文章。
- **选定文章处理** —— 选中后：抓取全文、按段落切分、中英互译、词汇提取。
- **双语阅读视图** —— 中英段落并排展示，词汇可逐项查看。
- **欧路词典集成** —— 将提取的词汇逐条手动推送到欧路单词本。
- **导师智能体** —— 围绕当前文章的对话，可总结全文、解释段落、拆解长难句、回答追问。
- **学习历史** —— 浏览并重新打开过去的学习会话。

## 系统架构

```
┌─────────────────┐         ┌──────────────────────┐         ┌──────────────┐
│  每日定时任务    │ ──────► │  Spring Boot API     │ ──────► │  PostgreSQL  │
│  (早上 8 点)     │         │  (候选 / 对话)       │         │  + Flyway    │
└─────────────────┘         └──────────┬───────────┘         └──────────────┘
                                       │
                                       ▼
                            ┌──────────────────────┐
                            │  LangChain4j + LLM   │
                            │  (排序 / 翻译 /      │
                            │   词汇提取 / 导师)   │
                            └──────────────────────┘
                                       ▲
                                       │
                            ┌──────────┴───────────┐
                            │  Next.js 前端        │
                            │  (学习界面 + 对话)   │
                            └──────────────────────┘
```

### 技术栈

| 层      | 技术                                                                    |
| ------- | ----------------------------------------------------------------------- |
| 后端    | Java 21、Spring Boot 3.4、Spring Data JPA、Flyway、LangChain4j、Rome    |
| 数据库  | PostgreSQL 16                                                           |
| 前端    | Next.js 16、React 19、TypeScript、Tailwind CSS 4、shadcn/ui             |
| 测试    | JUnit 5、Testcontainers、Vitest、Playwright                             |

## 快速开始

### 前置条件

- Java 21 及以上
- Maven 3.8+
- Node.js 20+ 与 npm
- Docker（用于运行 PostgreSQL）
- 一个 OpenAI 兼容的 LLM API Key

### 1. 启动 PostgreSQL

```bash
docker compose up -d
```

### 2. 配置密钥

通过环境变量配置 `src/main/resources/application.yml` 中的相关项（**请勿**将密钥提交到仓库）：

```bash
export OPENAI_API_KEY=sk-...
export OPENAI_CHAT_MODEL=gpt-4o-mini
export AILEARN_EUDIC_AUTH_TOKEN=...   # 可选，用于词汇同步
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

API 监听于 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:3000`。

## 项目结构

```
english-learn-assistant/
├── src/                        # Spring Boot 后端
│   ├── main/java/com/ailearn/
│   │   ├── controller/         # REST 控制器
│   │   ├── service/            # 候选 / 学习 / 导师服务
│   │   ├── client/             # 外部集成（欧路、RSS）
│   │   ├── entity/             # JPA 实体
│   │   └── config/             # 应用与 LLM 配置
│   └── main/resources/
│       ├── db/migration/       # Flyway 迁移脚本
│       └── prompts/            # LLM 提示词模板
├── frontend/                   # Next.js 前端
│   ├── src/app/                # App Router 页面
│   ├── src/components/         # UI 组件
│   └── e2e/                    # Playwright 冒烟测试
└── docs/                       # 设计文档与执行计划
```

## 主要 API

| 方法   | 路径                                          | 说明                          |
| ------ | --------------------------------------------- | ----------------------------- |
| GET    | `/api/candidates/today`                       | 今日候选文章                  |
| POST   | `/api/candidates/{id}/select`                 | 选择某篇文章用于学习          |
| GET    | `/api/learning-articles/{id}`                 | 含段落和词汇的完整学习文章    |
| POST   | `/api/learning-articles/{id}/vocabulary/{vid}/eudic` | 将单个词汇推送到欧路词典 |
| GET    | `/api/learning-history`                       | 历史学习记录                  |
| POST   | `/api/learning-articles/{id}/chat`            | 文章级别的导师对话            |

## 配置项

主要环境变量（默认值见 `src/main/resources/application.yml`）：

| 变量名                            | 说明                                            |
| --------------------------------- | ----------------------------------------------- |
| `OPENAI_API_KEY`                  | LLM API Key（OpenAI 兼容）                      |
| `OPENAI_CHAT_MODEL`               | 对话模型名称                                    |
| `AILEARN_DATASOURCE_URL`          | PostgreSQL JDBC URL                             |
| `AILEARN_CANDIDATE_CRON`          | 每日生成 cron 表达式（默认 `0 0 8 * * *`）       |
| `AILEARN_CANDIDATE_MAX`           | 每日候选数量（默认 5）                          |
| `AILEARN_EUDIC_AUTH_TOKEN`        | 用于词汇同步的欧路 API Token                    |
| `AILEARN_LLM_TIMEOUT_SECONDS`     | LLM 调用超时（默认 60 秒）                      |

## 开发

### 后端测试

```bash
mvn test
```

### 前端测试

```bash
cd frontend
npm test           # Vitest 单元测试
npm run test:e2e   # Playwright 冒烟测试
```

## 文档

- [设计文档](docs/superpowers/specs/2026-04-24-english-article-tutor-agent-design.md)
- [执行计划](docs/superpowers/plans/)

## 许可证

本项目目前为私有项目，尚未发布开源许可证。
