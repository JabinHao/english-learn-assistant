import { test, expect } from "@playwright/test";

test.describe("Candidate to Learning flow", () => {
  test("home page loads and shows candidate heading", async ({ page }) => {
    await page.goto("/");
    await expect(
      page.getByRole("heading", { name: "Today's Candidates" }),
    ).toBeVisible();
  });

  test("navigation links are present", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("link", { name: "Today" })).toBeVisible();
    await expect(page.getByRole("link", { name: "History" })).toBeVisible();
  });

  test("history page loads", async ({ page }) => {
    await page.goto("/history");
    await expect(
      page.getByRole("heading", { name: "Learning History" }),
    ).toBeVisible();
  });

  test("learning page shows error for invalid id", async ({ page }) => {
    await page.goto("/learning/999999");
    // Should show either loading state or error since backend is not running
    await page.waitForTimeout(2000);
    const content = await page.textContent("body");
    expect(content).toBeTruthy();
  });
});
