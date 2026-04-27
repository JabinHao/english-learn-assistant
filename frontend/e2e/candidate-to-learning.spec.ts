import { expect, test } from "@playwright/test";

test("candidate selection navigates to learning page", async ({ page }) => {
  await page.route("**/api/candidates/today", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        {
          id: 1,
          title: "OpenAI reasoning update",
          url: "https://example.com/a",
          source: "OpenAI",
          publishedAt: "2026-04-26T09:00:00",
          summary: "A strong candidate for study.",
          score: 9.2,
          recommendationReason: "Timely AI product update",
          selected: false,
        },
      ]),
    });
  });

  await page.route("**/api/candidates/1/select", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        learningArticleId: 88,
        candidateArticleId: 1,
        status: "EUDIC_PUSHED",
      }),
    });
  });

  await page.route("**/api/learning-articles/88", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: 88,
        candidateArticleId: 1,
        status: "EUDIC_PUSHED",
        title: "OpenAI reasoning update",
        url: "https://example.com/a",
        source: "OpenAI",
        publishedAt: "2026-04-26T09:00:00",
        articleContent: "Paragraph one.",
        summary: "A strong candidate for study.",
        paragraphs: [
          {
            paragraphIndex: 1,
            englishText: "Paragraph one.",
            chineseText: "第一段。",
          },
        ],
        vocabularyItems: [
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
        ],
      }),
    });
  });

  await page.goto("/");
  await page.getByRole("button", { name: "Study this article" }).click();
  await expect(page).toHaveURL(/\/learning\/88$/);
  await expect(page.getByText("OpenAI reasoning update")).toBeVisible();
});
