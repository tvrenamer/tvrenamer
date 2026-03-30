import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  use: {
    // Tauri dev server URL from tauri.conf.json devUrl
    baseURL: 'http://localhost:5173',
  },
  // Start the Vite dev server before tests
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
