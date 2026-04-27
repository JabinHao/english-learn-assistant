import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ArticleSummary } from "./article-summary";
import { BilingualParagraphs } from "./bilingual-paragraphs";
import { VocabularyList } from "./vocabulary-list";

describe("learning view", () => {
  it("renders summary, bilingual paragraphs, and vocabulary", () => {
    const summaryHtml = renderToStaticMarkup(
      <ArticleSummary
        article={{
          id: 88,
          candidateArticleId: 1,
          status: "EUDIC_PUSHED",
          title: "OpenAI reasoning update",
          url: "https://example.com/a",
          source: "OpenAI",
          publishedAt: "2026-04-26T09:00:00",
          articleContent: "Paragraph one.",
          summary: "A strong candidate for study.",
          paragraphs: [],
          vocabularyItems: [],
        }}
      />,
    );

    const paragraphHtml = renderToStaticMarkup(
      <BilingualParagraphs
        paragraphs={[
          {
            paragraphIndex: 1,
            englishText: "Paragraph one.",
            chineseText: "第一段。",
          },
        ]}
      />,
    );

    const vocabularyHtml = renderToStaticMarkup(
      <VocabularyList
        items={[
          {
            word: "reasoning",
            lemma: "reasoning",
            type: "WORD",
            ipa: null,
            englishDefinition: "careful thought",
            chineseDefinition: "推理",
            sourceSentence: "Paragraph one.",
            eudicPushed: true,
          },
        ]}
      />,
    );

    expect(summaryHtml).toContain("OpenAI reasoning update");
    expect(paragraphHtml).toContain("Paragraph one.");
    expect(paragraphHtml).toContain("第一段。");
    expect(vocabularyHtml).toContain("reasoning");
    expect(vocabularyHtml).toContain("Eudic synced");
  });
});
