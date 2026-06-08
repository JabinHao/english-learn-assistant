import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { HistoryList } from "./history-list";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }),
}));

describe("HistoryList", () => {
  it("renders an empty state when there are no learning records", () => {
    const html = renderToStaticMarkup(<HistoryList items={[]} />);

    expect(html).toContain("No learning records yet.");
  });

  it("renders learning records with links back to study sessions", () => {
    const html = renderToStaticMarkup(
      <HistoryList
        items={[
          {
            id: 12,
            title: "Newest article",
            chineseTitle: "最新文章",
            source: "OpenAI",
            publishedAt: "2026-04-28T09:00:00",
            selectedAt: "2026-04-28T20:00:00",
            status: "VOCAB_READY",
            summary: "A strong candidate for study.",
            chineseSummary: "适合精读的中文摘要。",
          },
        ]}
      />,
    );

    expect(html).toContain("最新文章");
    expect(html).toContain("Newest article");
    expect(html).toContain("适合精读的中文摘要。");
    expect(html).toContain("OpenAI");
    expect(html).toContain("VOCAB_READY");
    expect(html).toContain('href="/learning/12"');
  });
});
