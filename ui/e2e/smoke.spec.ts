import { test, expect } from '@playwright/test';

// Note: Tauri native drag-drop events (tauri://drag-drop) cannot be simulated via
// Playwright's page.dispatchEvent because they originate from the OS, not the browser.
// To test the full drag-drop-to-rename flow in E2E, use Tauri's app.emit() from a
// test helper script or the tauri-driver project.

test('app renders TVRenamer heading', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('TVRenamer')).toBeVisible();
});

test('drop zone instruction is shown with no files', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText(/drop files/i)).toBeVisible();
});

test('Preferences button opens preferences dialog', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /preferences/i }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByText('Preferences')).toBeVisible();
});

test('Rename Selected button is present', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('button', { name: /rename selected/i })).toBeVisible();
});
