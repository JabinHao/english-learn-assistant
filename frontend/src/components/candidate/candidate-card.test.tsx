import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { CandidateCard } from "./candidate-card";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("CandidateCard", () => {
  it("renders candidate metadata and recommendation reason", () => {
    const html = renderToStaticMarkup(
      <CandidateCard
        candidate={{
          id: 1,
          title: "OpenAI reasoning update",
          chineseTitle: "OpenAI 推理能力更新",
          url: "https://example.com/a",
          source: "OpenAI",
          publishedAt: "2026-04-26T09:00:00",
          summary: "A strong candidate for study.",
          chineseSummary: "适合精读的中文摘要。",
          score: 9.2,
          recommendationReason: "Timely AI product update",
          selected: true,
        }}
      />,
    );

    expect(html).toContain("OpenAI 推理能力更新");
    expect(html).toContain("适合精读的中文摘要。");
    expect(html).toContain("OpenAI reasoning update");
    expect(html).toContain("A strong candidate for study.");
    expect(html).toContain("Timely AI product update");
    expect(html).toContain("Score 9.2");
    expect(html).toContain("Selected");
  });
});
