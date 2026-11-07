import { defineConfig, devices } from "@playwright/test";

/**
 * The suite drives the REAL app: the backend must already run on :8080
 * (vite proxies /api and /ws to it). The Vite dev server is started
 * automatically unless one is already listening on 5173.
 *
 * Run everything:    npm run e2e
 * Chromium only:     npm run e2e -- --project=chromium
 */
export default defineConfig({
        testDir: "./e2e",
        fullyParallel: true,
        forbidOnly: !!process.env.CI,
        retries: process.env.CI ? 2 : 0,
        workers: process.env.CI ? 1 : 2,
        reporter: process.env.CI ? "html" : [["list"]],
        timeout: 60_000,
        use: {
                baseURL: process.env.BASE_URL || "http://localhost:5173",
                trace: "on-first-retry",
        },
        webServer: {
                command: "npm run dev -- --port 5173 --strictPort",
                url: "http://localhost:5173",
                reuseExistingServer: !process.env.CI,
                timeout: 60_000,
        },
        projects: [
                {
                        name: "chromium",
                        use: { ...devices["Desktop Chrome"] },
                },
                {
                        name: "firefox",
                        use: { ...devices["Desktop Firefox"] },
                },
                {
                        name: "webkit",
                        use: { ...devices["Desktop Safari"] },
                },
        ],
});
