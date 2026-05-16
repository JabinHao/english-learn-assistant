import { describe, expect, it } from "vitest";
import { formatDisplayDate, formatDisplayDateTime } from "./date";

describe("date formatting", () => {
  it("formats date-time strings without relying on runtime locale", () => {
    expect(formatDisplayDateTime("2026-04-22T08:00:00Z")).toBe(
      "2026-04-22 08:00:00",
    );
  });

  it("formats date strings without relying on runtime locale", () => {
    expect(formatDisplayDate("2026-04-22T08:00:00Z")).toBe("2026-04-22");
  });
});
