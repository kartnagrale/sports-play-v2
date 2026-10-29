import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  // Certificate rasterization can outlive the default 5s under container load.
  expect: { timeout: 20000 },
  reporter: [["list"]],
  use: {
    baseURL: process.env.PHASE1_WEB_URL || "http://localhost:53001",
    channel: "msedge",
    headless: true,
    trace: "off",
    screenshot: "off",
    video: "off",
  },
});
