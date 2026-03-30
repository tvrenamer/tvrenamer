# Task 13 — Playwright E2E Tests: Implementation Summary

## Status: Complete

## What Was Done

### Playwright installed
- Added `@playwright/test` ^1.58.2 as a devDependency via `npm install --save-dev`
- Installed Chromium browser via `npx playwright install chromium` (Chrome Headless Shell 145.0.7632.6)

### Files created
- `ui/playwright.config.ts` — Playwright config pointing `testDir` at `./e2e`, `baseURL` at `http://localhost:5173`, and a `webServer` block that starts `npm run dev` (skipped if server already running outside CI)
- `ui/e2e/smoke.spec.ts` — Four smoke tests covering:
  - App renders a "TVRenamer" heading
  - Drop zone instruction visible with no files
  - Preferences button opens a dialog
  - Rename Selected button is present

### Files modified
- `ui/package.json` — Added `"test:e2e": "playwright test"` to scripts
- `ui/vitest.config.ts` — Added `exclude: ['**/node_modules/**', '**/e2e/**']` so Vitest does not attempt to run Playwright specs as unit tests (this was needed; without it Vitest failed on the Playwright import)

## Verification

Unit tests confirmed passing after the exclusion fix:

```
Test Files  10 passed (10)
     Tests  45 passed (45)
```

E2E tests were NOT executed (require a running Tauri dev server).

## Note on Drag-Drop Testing

Tauri native drag-drop events (`tauri://drag-drop`) originate from the OS and cannot be simulated via `page.dispatchEvent`. Full E2E coverage of the drag-drop-to-rename flow requires `tauri-driver` or `app.emit()` from a test helper.

## Commit

`bf3e7a1` — `test(ui): add Playwright E2E smoke tests for app shell`

## Files Changed

- `ui/playwright.config.ts` (created)
- `ui/e2e/smoke.spec.ts` (created)
- `ui/package.json` (test:e2e script added)
- `ui/package-lock.json` (updated by npm)
- `ui/vitest.config.ts` (e2e exclude added)
