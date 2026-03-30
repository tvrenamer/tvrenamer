# TVRenamer: React/Tauri Frontend Implementation Plan

> **For Claude:** Run `/execute-plan` to implement this plan (will ask which execution style you prefer). Steps use checkbox (`- [ ]`) syntax for tracking.
> **Related Issues:** None detected — no issue tracker configured in repository.

**Goal:** Build the complete React/TypeScript frontend for TVRenamer — file table with drag-and-drop ingestion, per-row TMDB lookup, rename execution with real-time progress, preferences dialog, and first-launch API key onboarding.

**Architecture:** Vite + React 18 + TypeScript scaffold already exists at `ui/`. Rust backend IPC is fully implemented except for `parse_files` (parser has no IPC bridge). Frontend state management via `useState`/`useReducer`; per-row lookups via TanStack Query mutations; table rendering via TanStack Table v8.

**Tech Stack:**
- React 18, TypeScript, Vite
- `@tauri-apps/api` v2 — IPC (`invoke`) and events (`listen`)
- `@tanstack/react-table` v8 — table rendering
- `@tanstack/react-query` v5 — per-row async mutations
- `@tanstack/react-virtual` v3 — row virtualisation (>100 rows)
- `vitest` + `@testing-library/react` — unit/component tests
- `@tauri-apps/plugin-dialog` — native directory picker
- Playwright — E2E tests (Task 13)

**Context Gathered From:**
- `docs/hyperpowers/research/2026-03-29-modernise-frontend.md` — component architecture, TanStack patterns, drag-drop config, IPC patterns, data flow
- `docs/hyperpowers/research/2026-03-29-modernise-stack.md` — Tauri v2 config details, preference schema (12 fields), validated assumptions, open questions
- `src-tauri/src/ipc.rs` — registered IPC commands and type contracts
- `src-tauri/src/parser/mod.rs` — `ParseResult` struct (missing `Serialize` derive)
- `src-tauri/src/renamer/template.rs` — `apply_template` function (4 tokens only)
- `src-tauri/src/metadata/models.rs` — `Series`, `Episode` structs
- `src-tauri/src/config/prefs.rs` — `UserPreferences` schema (12 fields)
- `src-tauri/tauri.conf.json` — `dragDropEnabled: true` (Tauri native events), frontend at `ui/`
- `ui/package.json` — scaffold with TanStack deps already installed

---

## Validated Assumptions

### ✅ Validated
- `dragDropEnabled: true` in `tauri.conf.json` → Tauri fires `tauri://drag-drop` events with `{ paths: string[] }` payload
- IPC import is `@tauri-apps/api/core` (not `@tauri-apps/api/tauri`)
- Event import is `@tauri-apps/api/event`
- `ParseResult` struct exists in `src-tauri/src/parser/mod.rs` but lacks `#[derive(Serialize)]` — must be added before it can be returned over IPC
- `apply_template` uses exactly 4 tokens: `%S`, `%s`, `%0e`, `%t`
- `UserPreferences` has 12 fields serialised with `snake_case` JSON keys
- `RenameOutcome.status` serialises as `"success"`, `"already_in_place"`, `"fail_to_move"` (snake_case via `serde`)

### ⚠️ Verify Before Implementing
- `@tauri-apps/plugin-dialog` availability: check `src-tauri/Cargo.toml` and `ui/package.json` before using in Task 10
- `@tanstack/react-table` exact version in `node_modules` — `^8.17` resolves to 8.21+; API calls in this plan are correct for 8.x

---

## Task 1: Add `parse_files` IPC command (Rust)

**Why:** The parser module exists in Rust (`src-tauri/src/parser/`) but has no IPC bridge. The frontend cannot parse filenames without this command.

**Files:**
- Modify: `src-tauri/src/parser/mod.rs:5` — add `Serialize` to `ParseResult` derive
- Modify: `src-tauri/src/ipc.rs` — add `parse_files` command
- Modify: `src-tauri/src/lib.rs:47` — register `ipc::parse_files`

---

- [ ] **Step 1: Write the failing test**

Add to the existing `#[cfg(test)]` block in `src-tauri/src/ipc.rs`:

```rust
#[test]
fn parse_files_returns_serializable_result() {
    // ParseResult must implement Serialize for IPC — verify via serde_json
    let result = crate::parser::parse_filename("Fargo.S01E01.HDTV.x264-2HD.mp4");
    let json = serde_json::to_string(&result).expect("ParseResult must be serializable");
    assert!(json.contains("Fargo"), "show_name must be present: {json}");
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
cd src-tauri && cargo test parse_files_returns_serializable_result 2>&1
```

Expected: compile error — `the trait 'Serialize' is not implemented for 'Option<ParseResult>'`

- [ ] **Step 3: Add `Serialize` to `ParseResult`**

In `src-tauri/src/parser/mod.rs`, change line 3:

```rust
// Before:
#[derive(Debug, PartialEq)]
pub struct ParseResult {

// After:
#[derive(Debug, PartialEq, serde::Serialize)]
pub struct ParseResult {
```

- [ ] **Step 4: Add `parse_files` command to `src-tauri/src/ipc.rs`**

Add after the `ping` function (around line 25):

```rust
/// Parse a batch of file paths using the Rust filename parser.
/// Returns None for paths that no pattern could match.
/// Call this after `tauri://drag-drop` to extract show/season/episode from filenames.
#[tauri::command]
pub async fn parse_files(paths: Vec<String>) -> Vec<Option<crate::parser::ParseResult>> {
    paths.iter().map(|p| crate::parser::parse_filename(p)).collect()
}
```

- [ ] **Step 5: Register in `src-tauri/src/lib.rs`**

Change the `invoke_handler` block to add `ipc::parse_files`:

```rust
.invoke_handler(tauri::generate_handler![
    ipc::ping,
    ipc::parse_files,      // ← add this line
    ipc::search_shows,
    ipc::lookup_episode,
    ipc::validate_tmdb_key,
    ipc::save_tmdb_key,
    ipc::perform_renames,
    ipc::get_preferences,
    ipc::save_preferences,
])
```

- [ ] **Step 6: Run tests to verify**

```bash
cd src-tauri && cargo test 2>&1
```

Expected: all tests pass, including `parse_files_returns_serializable_result`

- [ ] **Step 7: Verify the app still compiles**

```bash
cd src-tauri && cargo build 2>&1
```

Expected: `Finished dev [unoptimized + debuginfo] target(s)` with no errors

- [ ] **Step 8: Commit**

```bash
git add src-tauri/src/parser/mod.rs src-tauri/src/ipc.rs src-tauri/src/lib.rs
git commit -m "feat(ipc): add parse_files command; derive Serialize on ParseResult"
```

---

## Task 2: Add Vitest + Testing Library to `ui/`

**Why:** The scaffold has no test runner. All subsequent component and hook tests depend on this setup.

**Files:**
- Modify: `ui/package.json` — add vitest, jsdom, testing-library devDependencies
- Create: `ui/vitest.config.ts`
- Create: `ui/src/test-setup.ts`

---

- [ ] **Step 1: Write a placeholder test that will fail without vitest**

Create `ui/src/smoke.test.ts`:

```typescript
import { expect, test } from 'vitest';

test('vitest is configured', () => {
  expect(1 + 1).toBe(2);
});
```

- [ ] **Step 2: Verify it fails (no test runner yet)**

```bash
cd ui && npx vitest run 2>&1
```

Expected: error — `vitest: command not found` or module resolution failure

- [ ] **Step 3: Install vitest and testing-library**

```bash
cd ui && npm install --save-dev vitest @vitest/coverage-v8 jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event 2>&1
```

Expected: packages added to `package.json` devDependencies

- [ ] **Step 4: Create `ui/vitest.config.ts`**

```typescript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
    globals: true,
  },
});
```

- [ ] **Step 5: Create `ui/src/test-setup.ts`**

```typescript
import '@testing-library/jest-dom';

// Mock the Tauri API — not available in jsdom test environment
vi.mock('@tauri-apps/api/core', () => ({
  invoke: vi.fn(),
}));

vi.mock('@tauri-apps/api/event', () => ({
  listen: vi.fn().mockResolvedValue(() => {}), // returns unlisten fn
}));
```

- [ ] **Step 6: Add test script to `ui/package.json`**

```json
"scripts": {
  "dev": "vite",
  "build": "tsc && vite build",
  "preview": "vite preview",
  "test": "vitest run",
  "test:watch": "vitest"
},
```

- [ ] **Step 7: Run the smoke test to verify it passes**

```bash
cd ui && npm test 2>&1
```

Expected:
```
✓ src/smoke.test.ts > vitest is configured
Test Files  1 passed (1)
```

- [ ] **Step 8: Clean up smoke test**

Delete `ui/src/smoke.test.ts` — it was only used to validate the setup.

- [ ] **Step 9: Commit**

```bash
git add ui/package.json ui/package-lock.json ui/vitest.config.ts ui/src/test-setup.ts
git commit -m "chore(ui): add vitest + testing-library setup"
```

---

## Task 3: TypeScript types (`ui/src/types/index.ts`)

**Why:** All hooks and components share these types. They mirror the Rust IPC contracts exactly.

**Files:**
- Create: `ui/src/types/index.ts`
- Create: `ui/src/types/index.test.ts`

---

- [ ] **Step 1: Write type assertion tests**

Create `ui/src/types/index.test.ts`:

```typescript
import { describe, it, expect } from 'vitest';
import type { Series, Episode, UserPreferences, ParseResult, RenameOutcome, FileRow } from './index';

describe('TypeScript types compile and satisfy contracts', () => {
  it('Series matches IPC contract', () => {
    const s: Series = { id: 1, name: 'Fargo', first_air_date: '2014-04-15' };
    expect(s.id).toBe(1);
  });

  it('Episode matches IPC contract', () => {
    const e: Episode = {
      name: 'The Crocodile',
      season_number: 1,
      episode_number: 2,
      air_date: '2014-04-22',
      overview: null,
    };
    expect(e.season_number).toBe(1);
  });

  it('UserPreferences matches IPC contract (12 fields + version)', () => {
    const p: UserPreferences = {
      version: 1,
      preload_folder: null,
      dest_dir: '~/TV',
      season_prefix: 'Season ',
      season_prefix_leading_zero: false,
      move_selected: false,
      rename_selected: true,
      remove_emptied_directories: true,
      delete_row_after_move: false,
      rename_replacement_mask: '%S [%sx%0e] %t',
      check_for_updates: true,
      recursively_add_folders: true,
      ignore_keywords: ['sample'],
    };
    expect(p.rename_replacement_mask).toBe('%S [%sx%0e] %t');
  });

  it('ParseResult matches IPC contract', () => {
    const r: ParseResult = { show_name: 'Fargo', season: 1, episode: 1, resolution: '720p' };
    expect(r.show_name).toBe('Fargo');
  });

  it('RenameOutcome status union is exhaustive', () => {
    const statuses: RenameOutcome['status'][] = ['success', 'already_in_place', 'fail_to_move'];
    expect(statuses).toHaveLength(3);
  });

  it('FileRow has required shape', () => {
    const row: FileRow = {
      id: 'abc',
      sourcePath: '/tv/Fargo.S01E01.mkv',
      parseResult: null,
      status: 'idle',
      seriesOptions: [],
      selectedSeriesId: null,
      episode: null,
      computedNewName: null,
      renameOutcome: null,
      errorMessage: null,
    };
    expect(row.status).toBe('idle');
  });

  it('applyTemplate produces correct filename', () => {
    const { applyTemplate } = await import('./index');
    expect(applyTemplate('%S [%sx%0e] %t', 'Fargo', 1, 1, 'The Crocodile'))
      .toBe('Fargo [1x01] The Crocodile');
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: TypeScript errors — `Cannot find module './index'`

- [ ] **Step 3: Create `ui/src/types/index.ts`**

```typescript
// TypeScript mirror of Rust IPC types.
// Keep in sync with src-tauri/src/metadata/models.rs, config/prefs.rs, ipc.rs, parser/mod.rs.

export interface Series {
  id: number;
  name: string;
  first_air_date: string | null;
}

export interface Episode {
  name: string;
  season_number: number;
  episode_number: number;
  air_date: string | null;
  overview: string | null;
}

export interface UserPreferences {
  version: number;
  preload_folder: string | null;
  dest_dir: string;
  season_prefix: string;
  season_prefix_leading_zero: boolean;
  move_selected: boolean;
  rename_selected: boolean;
  remove_emptied_directories: boolean;
  delete_row_after_move: boolean;
  rename_replacement_mask: string;
  check_for_updates: boolean;
  recursively_add_folders: boolean;
  ignore_keywords: string[];
}

export interface ParseResult {
  show_name: string;
  season: number;
  episode: number;
  resolution: string | null;
}

export interface RenameRequest {
  source: string;
  dest: string;
}

export type RenameStatus = 'success' | 'already_in_place' | 'fail_to_move';

export interface RenameOutcome {
  source: string;
  dest: string;
  status: RenameStatus;
  error: string | null;
}

export type RowStatus =
  | 'idle'
  | 'searching'
  | 'ready'
  | 'no_match'
  | 'parse_failed'
  | 'renaming'
  | 'success'
  | 'error';

export interface FileRow {
  id: string;
  sourcePath: string;
  parseResult: ParseResult | null;
  status: RowStatus;
  seriesOptions: Series[];
  selectedSeriesId: number | null;
  episode: Episode | null;
  computedNewName: string | null; // result of applyTemplate + optional dest path
  renameOutcome: RenameOutcome | null;
  errorMessage: string | null;
}

/**
 * Replicate apply_template from src-tauri/src/renamer/template.rs.
 * Tokens: %S = show name, %s = season (unpadded), %0e = episode (zero-padded 2 digits), %t = title.
 * Runs entirely in the frontend — avoids a Rust round-trip for a pure string substitution.
 */
export function applyTemplate(
  mask: string,
  show: string,
  season: number,
  episode: number,
  title: string,
): string {
  return mask
    .replaceAll('%S', show)
    .replaceAll('%s', String(season))
    .replaceAll('%0e', String(episode).padStart(2, '0'))
    .replaceAll('%t', title);
}
```

- [ ] **Step 4: Fix the dynamic import in the test**

The `applyTemplate` test used a dynamic `await import` which vitest handles, but it's cleaner as a static import. Update `ui/src/types/index.test.ts` to use a static import:

```typescript
import type { Series, Episode, UserPreferences, ParseResult, RenameOutcome, FileRow } from './index';
import { applyTemplate } from './index';
```

And remove the `await import` from the last test:

```typescript
  it('applyTemplate produces correct filename', () => {
    expect(applyTemplate('%S [%sx%0e] %t', 'Fargo', 1, 1, 'The Crocodile'))
      .toBe('Fargo [1x01] The Crocodile');
  });
```

- [ ] **Step 5: Run tests to verify all pass**

```bash
cd ui && npm test 2>&1
```

Expected:
```
✓ src/types/index.test.ts > TypeScript types compile and satisfy contracts > Series matches IPC contract
✓ ... (8 tests pass)
Test Files  1 passed (1)
```

- [ ] **Step 6: Commit**

```bash
git add ui/src/types/index.ts ui/src/types/index.test.ts
git commit -m "feat(ui): add TypeScript types mirroring Rust IPC contracts + applyTemplate"
```

---

## Task 4: `useTauriDrop` hook (`ui/src/hooks/useTauriDrop.ts`)

**Why:** Encapsulates Tauri native drag-drop events. `dragDropEnabled: true` in tauri.conf.json means the app receives `tauri://drag-drop` events with `{ paths: string[] }`. The hook exposes the dropped paths and a visual `isOver` flag.

**Files:**
- Create: `ui/src/hooks/useTauriDrop.ts`
- Create: `ui/src/hooks/useTauriDrop.test.ts`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/hooks/useTauriDrop.test.ts`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useTauriDrop } from './useTauriDrop';
import { listen } from '@tauri-apps/api/event';

const mockListen = vi.mocked(listen);

describe('useTauriDrop', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('registers listener for tauri://drag-drop on mount', async () => {
    const onDrop = vi.fn();
    mockListen.mockResolvedValue(() => {});

    renderHook(() => useTauriDrop(onDrop));

    // Wait for the effect to run
    await vi.waitFor(() => {
      expect(mockListen).toHaveBeenCalledWith('tauri://drag-drop', expect.any(Function));
    });
  });

  it('calls onDrop with file paths when tauri://drag-drop fires', async () => {
    const onDrop = vi.fn();
    let capturedHandler: ((e: { payload: { paths: string[] } }) => void) | null = null;

    mockListen.mockImplementation(async (_event, handler) => {
      capturedHandler = handler as typeof capturedHandler;
      return () => {};
    });

    renderHook(() => useTauriDrop(onDrop));

    await vi.waitFor(() => capturedHandler !== null);

    act(() => {
      capturedHandler!({ payload: { paths: ['/tv/Fargo.S01E01.mkv', '/tv/Fargo.S01E02.mkv'] } });
    });

    expect(onDrop).toHaveBeenCalledWith(['/tv/Fargo.S01E01.mkv', '/tv/Fargo.S01E02.mkv']);
  });

  it('calls unlisten on unmount', async () => {
    const unlisten = vi.fn();
    mockListen.mockResolvedValue(unlisten);
    const onDrop = vi.fn();

    const { unmount } = renderHook(() => useTauriDrop(onDrop));
    await vi.waitFor(() => expect(mockListen).toHaveBeenCalled());

    unmount();

    expect(unlisten).toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './useTauriDrop'`

- [ ] **Step 3: Create `ui/src/hooks/useTauriDrop.ts`**

```typescript
import { useEffect, useRef } from 'react';
import { listen } from '@tauri-apps/api/event';

interface DragDropPayload {
  paths: string[];
}

/**
 * Listens for Tauri native drag-drop events (dragDropEnabled: true in tauri.conf.json).
 * Calls onDrop with the array of OS-level file paths when files are dropped onto the window.
 * Automatically cleans up the listener on unmount.
 */
export function useTauriDrop(onDrop: (paths: string[]) => void): void {
  // Keep onDrop in a ref so the effect doesn't re-run when the callback identity changes.
  const onDropRef = useRef(onDrop);
  onDropRef.current = onDrop;

  useEffect(() => {
    let unlisten: (() => void) | null = null;

    listen<DragDropPayload>('tauri://drag-drop', (event) => {
      onDropRef.current(event.payload.paths);
    }).then((fn) => {
      unlisten = fn;
    });

    return () => {
      if (unlisten) unlisten();
    };
  }, []); // effect runs once — listener is stable
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: 3 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/hooks/useTauriDrop.ts ui/src/hooks/useTauriDrop.test.ts
git commit -m "feat(ui): add useTauriDrop hook with tauri native drag-drop listener"
```

---

## Task 5: `useRenameProgress` hook (`ui/src/hooks/useRenameProgress.ts`)

**Why:** Listens for `rename-progress` events emitted by the Rust `perform_renames` command after each file is processed. Used to update per-row status in real time during rename execution.

**Files:**
- Create: `ui/src/hooks/useRenameProgress.ts`
- Create: `ui/src/hooks/useRenameProgress.test.ts`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/hooks/useRenameProgress.test.ts`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRenameProgress } from './useRenameProgress';
import { listen } from '@tauri-apps/api/event';
import type { RenameOutcome } from '../types';

const mockListen = vi.mocked(listen);

describe('useRenameProgress', () => {
  beforeEach(() => vi.clearAllMocks());

  it('registers listener for rename-progress on mount', async () => {
    const onProgress = vi.fn();
    mockListen.mockResolvedValue(() => {});

    renderHook(() => useRenameProgress(onProgress));

    await vi.waitFor(() => {
      expect(mockListen).toHaveBeenCalledWith('rename-progress', expect.any(Function));
    });
  });

  it('calls onProgress with RenameOutcome when event fires', async () => {
    const onProgress = vi.fn();
    let capturedHandler: ((e: { payload: RenameOutcome }) => void) | null = null;

    mockListen.mockImplementation(async (_event, handler) => {
      capturedHandler = handler as typeof capturedHandler;
      return () => {};
    });

    renderHook(() => useRenameProgress(onProgress));
    await vi.waitFor(() => capturedHandler !== null);

    const outcome: RenameOutcome = {
      source: '/tv/Fargo.S01E01.mkv',
      dest: '/tv/Fargo/Season 1/Fargo [1x01] The Crocodile.mkv',
      status: 'success',
      error: null,
    };

    act(() => { capturedHandler!({ payload: outcome }); });

    expect(onProgress).toHaveBeenCalledWith(outcome);
  });

  it('cleans up listener on unmount', async () => {
    const unlisten = vi.fn();
    mockListen.mockResolvedValue(unlisten);
    const { unmount } = renderHook(() => useRenameProgress(vi.fn()));
    await vi.waitFor(() => expect(mockListen).toHaveBeenCalled());
    unmount();
    expect(unlisten).toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './useRenameProgress'`

- [ ] **Step 3: Create `ui/src/hooks/useRenameProgress.ts`**

```typescript
import { useEffect, useRef } from 'react';
import { listen } from '@tauri-apps/api/event';
import type { RenameOutcome } from '../types';

/**
 * Listens for `rename-progress` events emitted by the Rust `perform_renames` command.
 * Calls onProgress after each file is processed, with the outcome for that file.
 * Automatically cleans up on unmount.
 */
export function useRenameProgress(onProgress: (outcome: RenameOutcome) => void): void {
  const onProgressRef = useRef(onProgress);
  onProgressRef.current = onProgress;

  useEffect(() => {
    let unlisten: (() => void) | null = null;

    listen<RenameOutcome>('rename-progress', (event) => {
      onProgressRef.current(event.payload);
    }).then((fn) => {
      unlisten = fn;
    });

    return () => {
      if (unlisten) unlisten();
    };
  }, []);
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 3 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/hooks/useRenameProgress.ts ui/src/hooks/useRenameProgress.test.ts
git commit -m "feat(ui): add useRenameProgress hook for real-time rename event listener"
```

---

## Task 6: `usePreferences` hook (`ui/src/hooks/usePreferences.ts`)

**Why:** Wraps `get_preferences` and `save_preferences` IPC calls. Used by `PreferencesDialog` to load and persist settings.

**Files:**
- Create: `ui/src/hooks/usePreferences.ts`
- Create: `ui/src/hooks/usePreferences.test.ts`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/hooks/usePreferences.test.ts`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { usePreferences } from './usePreferences';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

const mockInvoke = vi.mocked(invoke);

const defaultPrefs: UserPreferences = {
  version: 1,
  preload_folder: null,
  dest_dir: '~/TV',
  season_prefix: 'Season ',
  season_prefix_leading_zero: false,
  move_selected: false,
  rename_selected: true,
  remove_emptied_directories: true,
  delete_row_after_move: false,
  rename_replacement_mask: '%S [%sx%0e] %t',
  check_for_updates: true,
  recursively_add_folders: true,
  ignore_keywords: ['sample'],
};

describe('usePreferences', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls get_preferences on mount and returns prefs', async () => {
    mockInvoke.mockResolvedValue(defaultPrefs);

    const { result } = renderHook(() => usePreferences());

    await waitFor(() => expect(result.current.prefs).not.toBeNull());

    expect(mockInvoke).toHaveBeenCalledWith('get_preferences');
    expect(result.current.prefs?.dest_dir).toBe('~/TV');
  });

  it('save calls save_preferences with updated prefs', async () => {
    mockInvoke.mockResolvedValue(defaultPrefs);
    const { result } = renderHook(() => usePreferences());
    await waitFor(() => expect(result.current.prefs).not.toBeNull());

    mockInvoke.mockResolvedValue(undefined);

    const updated = { ...defaultPrefs, dest_dir: '/mnt/tv' };
    await act(async () => { await result.current.save(updated); });

    expect(mockInvoke).toHaveBeenCalledWith('save_preferences', { newPrefs: updated });
  });

  it('exposes loading state initially', async () => {
    let resolve: (v: UserPreferences) => void;
    mockInvoke.mockReturnValue(new Promise<UserPreferences>((r) => { resolve = r; }));

    const { result } = renderHook(() => usePreferences());

    expect(result.current.loading).toBe(true);

    act(() => { resolve!(defaultPrefs); });
    await waitFor(() => expect(result.current.loading).toBe(false));
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './usePreferences'`

- [ ] **Step 3: Create `ui/src/hooks/usePreferences.ts`**

```typescript
import { useState, useEffect, useCallback } from 'react';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

interface UsePreferencesResult {
  prefs: UserPreferences | null;
  loading: boolean;
  error: string | null;
  save: (updated: UserPreferences) => Promise<void>;
}

/**
 * Loads preferences on mount via get_preferences IPC.
 * Provides a save() function that calls save_preferences IPC.
 */
export function usePreferences(): UsePreferencesResult {
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    invoke<UserPreferences>('get_preferences')
      .then(setPrefs)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false));
  }, []);

  const save = useCallback(async (updated: UserPreferences) => {
    await invoke('save_preferences', { newPrefs: updated });
    setPrefs(updated);
  }, []);

  return { prefs, loading, error, save };
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 3 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/hooks/usePreferences.ts ui/src/hooks/usePreferences.test.ts
git commit -m "feat(ui): add usePreferences hook wrapping get_preferences/save_preferences IPC"
```

---

## Task 7: `FileTable` component (`ui/src/components/FileTable.tsx`)

**Why:** The central UI — 4 columns (checkbox, current file, new filename dropdown, status). Uses TanStack Table v8. Per-row dropdown allows selecting from multiple TMDB series matches. Status column shows the row's current lifecycle state.

**Files:**
- Create: `ui/src/components/FileTable.tsx`
- Create: `ui/src/components/FileTable.test.tsx`

**Table column spec:**
| Column | Width | Content |
|--------|-------|---------|
| Checkbox | 30px | Row selection (controlled) |
| Current File | 550px | `row.sourcePath` basename |
| New Filename | 550px | Dropdown of series options; shows computed name |
| Status | 60px | Icon/badge per `row.status` |

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/components/FileTable.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileTable } from './FileTable';
import type { FileRow, Series } from '../types';

const makeRow = (overrides: Partial<FileRow> = {}): FileRow => ({
  id: 'row-1',
  sourcePath: '/tv/Fargo.S01E01.HDTV.mkv',
  parseResult: { show_name: 'Fargo', season: 1, episode: 1, resolution: '720p' },
  status: 'ready',
  seriesOptions: [
    { id: 101, name: 'Fargo', first_air_date: '2014-04-15' },
    { id: 202, name: 'Fargo (2024)', first_air_date: '2024-01-01' },
  ],
  selectedSeriesId: 101,
  episode: { name: 'The Crocodile', season_number: 1, episode_number: 1, air_date: null, overview: null },
  computedNewName: 'Fargo [1x01] The Crocodile',
  renameOutcome: null,
  errorMessage: null,
  ...overrides,
});

describe('FileTable', () => {
  it('renders source filename in current file column', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    // Basename is displayed, not full path
    expect(screen.getByText('Fargo.S01E01.HDTV.mkv')).toBeInTheDocument();
  });

  it('renders computed new filename in new filename column', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('Fargo [1x01] The Crocodile')).toBeInTheDocument();
  });

  it('shows series options in dropdown', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    const select = screen.getByRole('combobox');
    expect(select).toBeInTheDocument();
    expect(screen.getByText('Fargo (2014-04-15)')).toBeInTheDocument();
    expect(screen.getByText('Fargo (2024) (2024-01-01)')).toBeInTheDocument();
  });

  it('calls onSeriesChange when dropdown selection changes', () => {
    const onSeriesChange = vi.fn();
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={onSeriesChange}
      />
    );
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: '202' } });
    expect(onSeriesChange).toHaveBeenCalledWith('row-1', 202);
  });

  it('shows "Searching..." status for searching rows', () => {
    render(
      <FileTable
        rows={[makeRow({ status: 'searching' })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('Searching…')).toBeInTheDocument();
  });

  it('shows "No match" for no_match status', () => {
    render(
      <FileTable
        rows={[makeRow({ status: 'no_match', seriesOptions: [], selectedSeriesId: null, episode: null, computedNewName: null })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('No match')).toBeInTheDocument();
  });

  it('renders checkbox for each row and header checkbox', () => {
    render(
      <FileTable
        rows={[makeRow(), makeRow({ id: 'row-2', sourcePath: '/tv/Fargo.S01E02.mkv' })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    // Header checkbox + 2 row checkboxes
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './FileTable'`

- [ ] **Step 3: Create `ui/src/components/FileTable.tsx`**

```typescript
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table';
import type { FileRow, RowStatus } from '../types';

interface FileTableProps {
  rows: FileRow[];
  selectedIds: Set<string>;
  onSelectionChange: (id: string, checked: boolean) => void;
  onSeriesChange: (rowId: string, seriesId: number) => void;
}

const columnHelper = createColumnHelper<FileRow>();

function basename(path: string): string {
  return path.split('/').pop() ?? path;
}

function formatSeriesOption(s: { id: number; name: string; first_air_date: string | null }): string {
  return s.first_air_date ? `${s.name} (${s.first_air_date})` : s.name;
}

function StatusBadge({ status }: { status: RowStatus }) {
  const labels: Record<RowStatus, string> = {
    idle: '–',
    searching: 'Searching…',
    ready: '✓',
    no_match: 'No match',
    parse_failed: 'Parse failed',
    renaming: 'Renaming…',
    success: 'Done',
    error: 'Error',
  };
  return <span data-status={status}>{labels[status]}</span>;
}

export function FileTable({ rows, selectedIds, onSelectionChange, onSeriesChange }: FileTableProps) {
  const columns = [
    // Column 1: Checkbox (30px)
    columnHelper.display({
      id: 'select',
      size: 30,
      header: ({ table }) => (
        <input
          type="checkbox"
          checked={rows.length > 0 && rows.every((r) => selectedIds.has(r.id))}
          onChange={(e) => rows.forEach((r) => onSelectionChange(r.id, e.target.checked))}
          aria-label="Select all"
        />
      ),
      cell: ({ row }) => (
        <input
          type="checkbox"
          checked={selectedIds.has(row.original.id)}
          onChange={(e) => onSelectionChange(row.original.id, e.target.checked)}
          aria-label={`Select ${basename(row.original.sourcePath)}`}
        />
      ),
    }),

    // Column 2: Current File (550px)
    columnHelper.accessor('sourcePath', {
      id: 'currentFile',
      size: 550,
      header: 'Current File',
      cell: (info) => <span title={info.getValue()}>{basename(info.getValue())}</span>,
    }),

    // Column 3: New Filename (550px) — dropdown if multiple series options
    columnHelper.display({
      id: 'newFilename',
      size: 550,
      header: 'New Filename',
      cell: ({ row }) => {
        const { status, seriesOptions, selectedSeriesId, computedNewName } = row.original;

        if (status === 'searching') return <span>Searching…</span>;
        if (status === 'no_match') return <span>No match</span>;
        if (status === 'parse_failed') return <span>Parse failed</span>;
        if (status === 'idle') return <span>–</span>;

        return (
          <div>
            {seriesOptions.length > 1 ? (
              <select
                value={selectedSeriesId ?? ''}
                onChange={(e) => onSeriesChange(row.original.id, Number(e.target.value))}
              >
                {seriesOptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {formatSeriesOption(s)}
                  </option>
                ))}
              </select>
            ) : null}
            {computedNewName ? <span>{computedNewName}</span> : null}
          </div>
        );
      },
    }),

    // Column 4: Status (60px)
    columnHelper.accessor('status', {
      id: 'status',
      size: 60,
      header: 'Status',
      cell: (info) => <StatusBadge status={info.getValue()} />,
    }),
  ];

  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row.id,
  });

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <thead>
        {table.getHeaderGroups().map((hg) => (
          <tr key={hg.id}>
            {hg.headers.map((h) => (
              <th key={h.id} style={{ width: h.getSize(), textAlign: 'left', padding: '4px' }}>
                {flexRender(h.column.columnDef.header, h.getContext())}
              </th>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {table.getRowModel().rows.map((row) => (
          <tr key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <td key={cell.id} style={{ padding: '4px' }}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 7 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/components/FileTable.tsx ui/src/components/FileTable.test.tsx
git commit -m "feat(ui): add FileTable component with TanStack Table v8 — 4 columns with row selection"
```

---

## Task 8: `StatusBar` component (`ui/src/components/StatusBar.tsx`)

**Why:** Shows overall rename progress — count of pending/success/error rows and the current operation phase.

**Files:**
- Create: `ui/src/components/StatusBar.tsx`
- Create: `ui/src/components/StatusBar.test.tsx`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/components/StatusBar.test.tsx`:

```typescript
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBar } from './StatusBar';

describe('StatusBar', () => {
  it('shows total file count', () => {
    render(<StatusBar total={5} success={0} failed={0} phase="idle" />);
    expect(screen.getByText(/5 files/i)).toBeInTheDocument();
  });

  it('shows success and fail counts during/after rename', () => {
    render(<StatusBar total={5} success={3} failed={1} phase="renaming" />);
    expect(screen.getByText(/3 done/i)).toBeInTheDocument();
    expect(screen.getByText(/1 error/i)).toBeInTheDocument();
  });

  it('shows "Ready to rename" when all rows are ready', () => {
    render(<StatusBar total={3} success={0} failed={0} phase="ready" />);
    expect(screen.getByText(/ready to rename/i)).toBeInTheDocument();
  });

  it('shows "Complete" when rename is done', () => {
    render(<StatusBar total={3} success={3} failed={0} phase="complete" />);
    expect(screen.getByText(/complete/i)).toBeInTheDocument();
  });

  it('renders nothing visible when no files loaded', () => {
    render(<StatusBar total={0} success={0} failed={0} phase="idle" />);
    expect(screen.getByText(/0 files/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './StatusBar'`

- [ ] **Step 3: Create `ui/src/components/StatusBar.tsx`**

```typescript
export type Phase = 'idle' | 'searching' | 'ready' | 'renaming' | 'complete';

interface StatusBarProps {
  total: number;
  success: number;
  failed: number;
  phase: Phase;
}

const phaseLabel: Record<Phase, string> = {
  idle: 'Drop files to begin',
  searching: 'Looking up shows…',
  ready: 'Ready to rename',
  renaming: 'Renaming…',
  complete: 'Complete',
};

export function StatusBar({ total, success, failed, phase }: StatusBarProps) {
  return (
    <div
      style={{
        display: 'flex',
        gap: '16px',
        padding: '4px 8px',
        borderTop: '1px solid #ccc',
        fontSize: '0.85em',
      }}
    >
      <span>{total} files</span>
      {success > 0 && <span>{success} done</span>}
      {failed > 0 && <span>{failed} error{failed !== 1 ? 's' : ''}</span>}
      <span>{phaseLabel[phase]}</span>
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/components/StatusBar.tsx ui/src/components/StatusBar.test.tsx
git commit -m "feat(ui): add StatusBar component with phase + count display"
```

---

## Task 9: `ApiKeySetup` modal (`ui/src/components/ApiKeySetup.tsx`)

**Why:** First-launch onboarding — guides the user through obtaining and saving their TMDB API key. Uses `validate_tmdb_key` to test the key, then `save_tmdb_key` to persist it to the OS keychain. Non-blocking: can be dismissed without entering a key.

**Files:**
- Create: `ui/src/components/ApiKeySetup.tsx`
- Create: `ui/src/components/ApiKeySetup.test.tsx`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/components/ApiKeySetup.test.tsx`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ApiKeySetup } from './ApiKeySetup';
import { invoke } from '@tauri-apps/api/core';

const mockInvoke = vi.mocked(invoke);

describe('ApiKeySetup', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders the modal with explanation and input', () => {
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);
    expect(screen.getByText(/TMDB API key/i)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /test/i })).toBeInTheDocument();
  });

  it('enables Save button only after a successful test', async () => {
    mockInvoke.mockResolvedValue(undefined); // validate_tmdb_key returns Ok(())
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'abc123' } });

    const saveButton = screen.queryByRole('button', { name: /save/i });
    expect(saveButton).toBeNull(); // not shown before test passes

    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('validate_tmdb_key', { key: 'abc123' });
    });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    });
  });

  it('shows error message on failed validation', async () => {
    mockInvoke.mockRejectedValue('API key invalid or missing');
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'badkey' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText(/API key invalid or missing/i)).toBeInTheDocument();
    });
  });

  it('calls save_tmdb_key and then onSaved after saving', async () => {
    mockInvoke
      .mockResolvedValueOnce(undefined) // validate
      .mockResolvedValueOnce(undefined); // save

    const onSaved = vi.fn();
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={onSaved} />);

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'validkey' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => screen.getByRole('button', { name: /save/i }));
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('save_tmdb_key', { key: 'validkey' });
      expect(onSaved).toHaveBeenCalled();
    });
  });

  it('calls onDismiss when dismissed without saving', () => {
    const onDismiss = vi.fn();
    render(<ApiKeySetup onDismiss={onDismiss} onSaved={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /skip/i }));
    expect(onDismiss).toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './ApiKeySetup'`

- [ ] **Step 3: Create `ui/src/components/ApiKeySetup.tsx`**

```typescript
import { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface ApiKeySetupProps {
  onDismiss: () => void;
  onSaved: () => void;
}

type TestState = 'idle' | 'testing' | 'valid' | 'error';

/**
 * First-launch TMDB API key onboarding modal.
 * Step 1: Explain why the key is needed.
 * Step 2: Direct link to https://www.themoviedb.org/settings/api
 * Step 3: Input field + "Test" button → validate_tmdb_key → on success show "Save" button.
 * Non-blocking: "Skip" dismisses without entering a key.
 */
export function ApiKeySetup({ onDismiss, onSaved }: ApiKeySetupProps) {
  const [key, setKey] = useState('');
  const [testState, setTestState] = useState<TestState>('idle');
  const [errorMessage, setErrorMessage] = useState('');

  async function handleTest() {
    setTestState('testing');
    setErrorMessage('');
    try {
      await invoke('validate_tmdb_key', { key });
      setTestState('valid');
    } catch (e) {
      setTestState('error');
      setErrorMessage(String(e));
    }
  }

  async function handleSave() {
    await invoke('save_tmdb_key', { key });
    onSaved();
  }

  return (
    <div role="dialog" aria-modal="true" style={{ padding: '24px', maxWidth: '480px' }}>
      <h2>TMDB API Key Required</h2>
      <p>
        TVRenamer uses the TMDB API to look up show and episode information.
        You need a free API key from{' '}
        <a href="https://www.themoviedb.org/settings/api" target="_blank" rel="noreferrer">
          themoviedb.org
        </a>
        .
      </p>

      <div style={{ marginTop: '16px' }}>
        <label htmlFor="api-key-input">Your TMDB API key:</label>
        <input
          id="api-key-input"
          type="text"
          value={key}
          onChange={(e) => { setKey(e.target.value); setTestState('idle'); }}
          placeholder="Paste your API key here"
          style={{ display: 'block', width: '100%', marginTop: '8px' }}
        />
      </div>

      {errorMessage && (
        <p style={{ color: 'red', marginTop: '8px' }}>{errorMessage}</p>
      )}

      {testState === 'valid' && (
        <p style={{ color: 'green', marginTop: '8px' }}>Key is valid!</p>
      )}

      <div style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
        <button onClick={handleTest} disabled={!key || testState === 'testing'}>
          {testState === 'testing' ? 'Testing…' : 'Test'}
        </button>

        {testState === 'valid' && (
          <button onClick={handleSave}>Save</button>
        )}

        <button onClick={onDismiss}>Skip</button>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/components/ApiKeySetup.tsx ui/src/components/ApiKeySetup.test.tsx
git commit -m "feat(ui): add ApiKeySetup onboarding modal with validate/save TMDB key flow"
```

---

## Task 10: `TokenBuilder` component (`ui/src/components/TokenBuilder.tsx`)

**Why:** Drag-and-drop rename template builder for the Preferences dialog. Allows constructing the `rename_replacement_mask` by clicking/dragging available tokens.

**Token reference:**
| Token | Meaning | Example |
|-------|---------|---------|
| `%S` | Show name | `Fargo` |
| `%s` | Season (unpadded) | `1` |
| `%0e` | Episode (zero-padded) | `01` |
| `%t` | Episode title | `The Crocodile` |

**Files:**
- Create: `ui/src/components/TokenBuilder.tsx`
- Create: `ui/src/components/TokenBuilder.test.tsx`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/components/TokenBuilder.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TokenBuilder } from './TokenBuilder';

describe('TokenBuilder', () => {
  it('renders the current mask in the input field', () => {
    render(<TokenBuilder value="%S [%sx%0e] %t" onChange={vi.fn()} />);
    const input = screen.getByRole('textbox');
    expect((input as HTMLInputElement).value).toBe('%S [%sx%0e] %t');
  });

  it('calls onChange when the mask input is edited directly', () => {
    const onChange = vi.fn();
    render(<TokenBuilder value="%S" onChange={onChange} />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '%S - %t' } });
    expect(onChange).toHaveBeenCalledWith('%S - %t');
  });

  it('renders all 4 available tokens as buttons', () => {
    render(<TokenBuilder value="" onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /%S/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%s/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%0e/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /%t/i })).toBeInTheDocument();
  });

  it('appends token to mask when token button is clicked', () => {
    const onChange = vi.fn();
    render(<TokenBuilder value="%S " onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /%t/i }));
    expect(onChange).toHaveBeenCalledWith('%S %t');
  });

  it('shows a live preview of the mask', () => {
    render(<TokenBuilder value="%S [%sx%0e] %t" onChange={vi.fn()} />);
    // Preview uses hardcoded example values: Show Name, season 1, ep 1, Episode Title
    expect(screen.getByText(/Show Name \[1x01\] Episode Title/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './TokenBuilder'`

- [ ] **Step 3: Create `ui/src/components/TokenBuilder.tsx`**

```typescript
import { applyTemplate } from '../types';

const TOKENS = [
  { token: '%S', label: '%S — Show name' },
  { token: '%s', label: '%s — Season' },
  { token: '%0e', label: '%0e — Episode (padded)' },
  { token: '%t', label: '%t — Episode title' },
] as const;

interface TokenBuilderProps {
  value: string;
  onChange: (newMask: string) => void;
}

/**
 * Rename template builder: editable text input showing the current mask,
 * token buttons that append tokens when clicked, and a live preview.
 */
export function TokenBuilder({ value, onChange }: TokenBuilderProps) {
  const preview = applyTemplate(value, 'Show Name', 1, 1, 'Episode Title');

  return (
    <div>
      <div style={{ marginBottom: '8px' }}>
        {TOKENS.map(({ token, label }) => (
          <button
            key={token}
            type="button"
            aria-label={label}
            onClick={() => onChange(value + token)}
            style={{ marginRight: '4px' }}
          >
            {token}
          </button>
        ))}
      </div>

      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{ width: '100%', fontFamily: 'monospace' }}
        aria-label="Rename mask"
      />

      <p style={{ marginTop: '4px', color: '#666', fontSize: '0.85em' }}>
        Preview: <em>{preview}</em>
      </p>
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/components/TokenBuilder.tsx ui/src/components/TokenBuilder.test.tsx
git commit -m "feat(ui): add TokenBuilder for rename mask editing with token buttons + preview"
```

---

## Task 11: `PreferencesDialog` component (`ui/src/components/PreferencesDialog.tsx`)

**Why:** Settings modal exposing all 12 user preferences. Uses `usePreferences` to load/save. Embeds `TokenBuilder` for `rename_replacement_mask`. Includes a native directory picker for `dest_dir` via `@tauri-apps/plugin-dialog`.

**Before starting:** Verify `@tauri-apps/plugin-dialog` is available:

```bash
cd ui && cat package.json | grep plugin-dialog
```

If not present, install it first:

```bash
cd ui && npm install @tauri-apps/plugin-dialog
```

**Files:**
- Create: `ui/src/components/PreferencesDialog.tsx`
- Create: `ui/src/components/PreferencesDialog.test.tsx`

---

- [ ] **Step 1: Write the failing test**

Create `ui/src/components/PreferencesDialog.test.tsx`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PreferencesDialog } from './PreferencesDialog';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

// Also mock the dialog plugin
vi.mock('@tauri-apps/plugin-dialog', () => ({
  open: vi.fn().mockResolvedValue('/picked/dir'),
}));

const mockInvoke = vi.mocked(invoke);

const defaultPrefs: UserPreferences = {
  version: 1, preload_folder: null, dest_dir: '~/TV', season_prefix: 'Season ',
  season_prefix_leading_zero: false, move_selected: false, rename_selected: true,
  remove_emptied_directories: true, delete_row_after_move: false,
  rename_replacement_mask: '%S [%sx%0e] %t', check_for_updates: true,
  recursively_add_folders: true, ignore_keywords: ['sample'],
};

describe('PreferencesDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockInvoke.mockResolvedValue(defaultPrefs);
  });

  it('renders all 12 preference fields', async () => {
    render(<PreferencesDialog onClose={vi.fn()} />);
    await waitFor(() => expect(screen.getByLabelText(/destination directory/i)).toBeInTheDocument());

    expect(screen.getByLabelText(/rename selected/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/move selected/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/season prefix/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/leading zero/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/remove emptied/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/delete row/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/check for updates/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/recursively/i)).toBeInTheDocument();
    // rename_replacement_mask via TokenBuilder
    expect(screen.getByRole('textbox', { name: /rename mask/i })).toBeInTheDocument();
  });

  it('calls save_preferences on Save', async () => {
    mockInvoke
      .mockResolvedValueOnce(defaultPrefs) // get_preferences
      .mockResolvedValueOnce(undefined);    // save_preferences

    render(<PreferencesDialog onClose={vi.fn()} />);
    await waitFor(() => screen.getByRole('button', { name: /save/i }));

    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('save_preferences', { newPrefs: expect.objectContaining({ version: 1 }) });
    });
  });

  it('calls onClose when Cancel is clicked', async () => {
    const onClose = vi.fn();
    render(<PreferencesDialog onClose={onClose} />);
    await waitFor(() => screen.getByRole('button', { name: /cancel/i }));
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onClose).toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: `Cannot find module './PreferencesDialog'`

- [ ] **Step 3: Create `ui/src/components/PreferencesDialog.tsx`**

```typescript
import { useState } from 'react';
import { open } from '@tauri-apps/plugin-dialog';
import { usePreferences } from '../hooks/usePreferences';
import { TokenBuilder } from './TokenBuilder';
import type { UserPreferences } from '../types';

interface PreferencesDialogProps {
  onClose: () => void;
}

/**
 * Settings modal for all 12 UserPreferences fields.
 * Loads current prefs via usePreferences on mount.
 * Save button persists via save_preferences IPC.
 */
export function PreferencesDialog({ onClose }: PreferencesDialogProps) {
  const { prefs, loading, save } = usePreferences();
  const [draft, setDraft] = useState<UserPreferences | null>(null);

  // Initialise draft from loaded prefs (only once)
  if (prefs && !draft) setDraft({ ...prefs });

  if (loading) return <div>Loading preferences…</div>;
  if (!draft) return null;

  const set = <K extends keyof UserPreferences>(key: K, value: UserPreferences[K]) =>
    setDraft((prev) => prev ? { ...prev, [key]: value } : prev);

  async function handleSave() {
    if (!draft) return;
    await save(draft);
    onClose();
  }

  async function handlePickDir() {
    const selected = await open({ directory: true, multiple: false });
    if (typeof selected === 'string') set('dest_dir', selected);
  }

  return (
    <div role="dialog" aria-modal="true" style={{ padding: '24px', maxWidth: '600px' }}>
      <h2>Preferences</h2>

      {/* dest_dir */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="dest-dir" aria-label="Destination directory">Destination Directory</label>
        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
          <input id="dest-dir" type="text" value={draft.dest_dir}
            onChange={(e) => set('dest_dir', e.target.value)} style={{ flex: 1 }} />
          <button type="button" onClick={handlePickDir}>Browse…</button>
        </div>
      </div>

      {/* season_prefix */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="season-prefix">Season Prefix</label>
        <input id="season-prefix" aria-label="Season prefix" type="text" value={draft.season_prefix}
          onChange={(e) => set('season_prefix', e.target.value)} style={{ display: 'block', marginTop: '4px' }} />
      </div>

      {/* Booleans */}
      {(
        [
          ['rename_selected', 'Rename selected files'] as const,
          ['move_selected', 'Move selected files'] as const,
          ['season_prefix_leading_zero', 'Season prefix leading zero'] as const,
          ['remove_emptied_directories', 'Remove emptied directories'] as const,
          ['delete_row_after_move', 'Delete row after move'] as const,
          ['check_for_updates', 'Check for updates'] as const,
          ['recursively_add_folders', 'Recursively add folders'] as const,
        ] as const
      ).map(([field, labelText]) => (
        <div key={field} style={{ marginBottom: '8px' }}>
          <label aria-label={labelText}>
            <input type="checkbox" checked={draft[field] as boolean}
              onChange={(e) => set(field, e.target.checked as UserPreferences[typeof field])} />
            {' '}{labelText}
          </label>
        </div>
      ))}

      {/* preload_folder */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="preload-folder">Preload Folder (optional)</label>
        <input id="preload-folder" type="text" value={draft.preload_folder ?? ''}
          onChange={(e) => set('preload_folder', e.target.value || null)}
          style={{ display: 'block', marginTop: '4px', width: '100%' }} />
      </div>

      {/* ignore_keywords */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="ignore-keywords">Ignore Keywords (comma-separated)</label>
        <input id="ignore-keywords" type="text"
          value={draft.ignore_keywords.join(', ')}
          onChange={(e) => set('ignore_keywords', e.target.value.split(',').map((s) => s.trim()).filter(Boolean))}
          style={{ display: 'block', marginTop: '4px', width: '100%' }} />
      </div>

      {/* rename_replacement_mask via TokenBuilder */}
      <div style={{ marginBottom: '16px' }}>
        <label>Rename Template</label>
        <TokenBuilder value={draft.rename_replacement_mask}
          onChange={(v) => set('rename_replacement_mask', v)} />
      </div>

      <div style={{ display: 'flex', gap: '8px' }}>
        <button onClick={handleSave}>Save</button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all 3 tests pass

- [ ] **Step 5: Commit**

```bash
git add ui/src/components/PreferencesDialog.tsx ui/src/components/PreferencesDialog.test.tsx
git commit -m "feat(ui): add PreferencesDialog with all 12 fields, TokenBuilder, and native dir picker"
```

---

## Task 12: Wire `App.tsx`

**Why:** Assembles all components into the main layout. Orchestrates the complete data flow: drag-drop → parse → TMDB lookup → table → rename → progress.

**Data flow:**
1. `useTauriDrop` fires with file paths
2. `invoke('parse_files', { paths })` → `ParseResult[]`
3. For each result, `invoke('search_shows', { query: showName })` → `Series[]`
4. Populate `FileRow[]` in state; status = `'ready'` with series options
5. On series dropdown change: `invoke('lookup_episode', { seriesId, season, episode })` → `Episode`; run `applyTemplate` locally
6. "Rename Selected" button → `invoke('perform_renames', { renames })` → outcomes; `useRenameProgress` handles per-row events
7. `ApiKeySetup` shown on first launch if TMDB key not set (detect via `validate_tmdb_key` error on first search)
8. `PreferencesDialog` toggled via a Preferences button

**Files:**
- Modify: `ui/src/App.tsx`
- Create: `ui/src/App.test.tsx`

---

- [ ] **Step 1: Write a minimal failing test for App.tsx**

Create `ui/src/App.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';
import { invoke } from '@tauri-apps/api/core';

vi.mocked(invoke).mockResolvedValue([]);

describe('App', () => {
  it('renders the main heading', () => {
    render(<App />);
    expect(screen.getByText('TVRenamer')).toBeInTheDocument();
  });

  it('renders the Preferences button', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /preferences/i })).toBeInTheDocument();
  });

  it('renders the drop zone instruction text when no files loaded', () => {
    render(<App />);
    expect(screen.getByText(/drop files/i)).toBeInTheDocument();
  });

  it('renders the Rename Selected button', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /rename selected/i })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd ui && npm test 2>&1
```

Expected: tests fail — current App.tsx does not render `TVRenamer`, `Preferences`, or drop zone text in the required format.

- [ ] **Step 3: Replace `ui/src/App.tsx` with full implementation**

```typescript
import { useState, useCallback, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { useTauriDrop } from './hooks/useTauriDrop';
import { useRenameProgress } from './hooks/useRenameProgress';
import { FileTable } from './components/FileTable';
import { StatusBar, type Phase } from './components/StatusBar';
import { ApiKeySetup } from './components/ApiKeySetup';
import { PreferencesDialog } from './components/PreferencesDialog';
import { applyTemplate } from './types';
import type { FileRow, ParseResult, Series, Episode, RenameOutcome, UserPreferences } from './types';

let rowCounter = 0;
function nextId(): string { return `row-${++rowCounter}`; }

function buildInitialRow(path: string, parseResult: ParseResult | null): FileRow {
  return {
    id: nextId(),
    sourcePath: path,
    parseResult,
    status: parseResult ? 'searching' : 'parse_failed',
    seriesOptions: [],
    selectedSeriesId: null,
    episode: null,
    computedNewName: null,
    renameOutcome: null,
    errorMessage: null,
  };
}

export default function App() {
  const [rows, setRows] = useState<FileRow[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [showPrefs, setShowPrefs] = useState(false);
  const [showApiSetup, setShowApiSetup] = useState(false);
  const [phase, setPhase] = useState<Phase>('idle');
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);

  // Load prefs once on mount
  // NOTE: Must use useEffect, NOT useState — useState initializer sets initial state,
  // it does not execute side effects. Using useState(() => {...}) would store a function
  // object as the state value, never invoking it.
  useEffect(() => {
    invoke<UserPreferences>('get_preferences').then(setPrefs).catch(() => {});
  }, []);

  // Update a single row by id
  const updateRow = useCallback((id: string, changes: Partial<FileRow>) => {
    setRows((prev) => prev.map((r) => r.id === id ? { ...r, ...changes } : r));
  }, []);

  // Called when files are dropped
  const handleDrop = useCallback(async (paths: string[]) => {
    // Parse all dropped files in one IPC call
    const parseResults: Array<ParseResult | null> = await invoke('parse_files', { paths });

    const newRows = paths.map((path, i) => buildInitialRow(path, parseResults[i] ?? null));
    setRows((prev) => [...prev, ...newRows]);
    setPhase('searching');

    // Kick off TMDB search for each successfully parsed file
    for (const row of newRows) {
      if (!row.parseResult) continue;
      const { show_name: showName, season, episode } = row.parseResult;

      invoke<Series[]>('search_shows', { query: showName })
        .then(async (seriesOptions) => {
          if (seriesOptions.length === 0) {
            updateRow(row.id, { status: 'no_match', seriesOptions: [] });
            return;
          }

          const selectedSeries = seriesOptions[0];
          let ep: Episode | null = null;
          try {
            ep = await invoke<Episode>('lookup_episode', {
              seriesId: selectedSeries.id,
              season,
              episode,
            });
          } catch {
            // Episode not found — still mark ready with no computed name
          }

          const computedNewName = ep && prefs
            ? applyTemplate(prefs.rename_replacement_mask, selectedSeries.name, season, episode, ep.name)
            : null;

          updateRow(row.id, {
            status: 'ready',
            seriesOptions,
            selectedSeriesId: selectedSeries.id,
            episode: ep,
            computedNewName,
          });
        })
        .catch((e: string) => {
          // Check if error indicates missing API key
          if (String(e).includes('ApiKeyMissing') || String(e).includes('API key invalid or missing')) {
            setShowApiSetup(true);
            updateRow(row.id, { status: 'error', errorMessage: 'TMDB API key not set' });
          } else {
            updateRow(row.id, { status: 'error', errorMessage: String(e) });
          }
        });
    }
  }, [prefs, updateRow]);

  useTauriDrop(handleDrop);

  // Called when rename-progress event fires for a file
  const handleRenameProgress = useCallback((outcome: RenameOutcome) => {
    setRows((prev) =>
      prev.map((r) =>
        r.sourcePath === outcome.source
          ? { ...r, status: outcome.status === 'success' ? 'success' : 'error', renameOutcome: outcome, errorMessage: outcome.error }
          : r
      )
    );
  }, []);

  useRenameProgress(handleRenameProgress);

  // Series dropdown changed for a row
  const handleSeriesChange = useCallback(async (rowId: string, seriesId: number) => {
    const row = rows.find((r) => r.id === rowId);
    if (!row?.parseResult) return;

    const { season, episode } = row.parseResult;
    const selectedSeries = row.seriesOptions.find((s) => s.id === seriesId);
    if (!selectedSeries) return;

    updateRow(rowId, { selectedSeriesId: seriesId, episode: null, computedNewName: null });

    try {
      const ep = await invoke<Episode>('lookup_episode', { seriesId, season, episode });
      const computedNewName = prefs
        ? applyTemplate(prefs.rename_replacement_mask, selectedSeries.name, season, episode, ep.name)
        : null;
      updateRow(rowId, { episode: ep, computedNewName });
    } catch {
      updateRow(rowId, { episode: null, computedNewName: null });
    }
  }, [rows, prefs, updateRow]);

  // Checkbox selection
  const handleSelectionChange = useCallback((id: string, checked: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  }, []);

  // Rename selected rows
  async function handleRename() {
    const toRename = rows.filter(
      (r) => selectedIds.has(r.id) && r.computedNewName && r.status === 'ready'
    );
    if (toRename.length === 0) return;

    setPhase('renaming');

    const renames = toRename.map((r) => ({
      source: r.sourcePath,
      // For now: rename in place (same directory, new filename from template)
      dest: [r.sourcePath.split('/').slice(0, -1).join('/'), r.computedNewName].filter(Boolean).join('/'),
    }));

    try {
      await invoke('perform_renames', { renames });
      setPhase('complete');
    } catch (e) {
      console.error('perform_renames failed:', e);
    }
  }

  const successCount = rows.filter((r) => r.status === 'success').length;
  const errorCount = rows.filter((r) => r.status === 'error').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: 'system-ui' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '8px 12px', borderBottom: '1px solid #ccc' }}>
        <h1 style={{ margin: 0, fontSize: '1.1em' }}>TVRenamer</h1>
        <button onClick={() => setShowPrefs(true)}>Preferences</button>
        <div style={{ flex: 1 }} />
        <button onClick={handleRename} disabled={selectedIds.size === 0}>
          Rename Selected
        </button>
      </div>

      {/* Main content area */}
      <div style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        {rows.length === 0 ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#999' }}>
            Drop files here to begin
          </div>
        ) : (
          <FileTable
            rows={rows}
            selectedIds={selectedIds}
            onSelectionChange={handleSelectionChange}
            onSeriesChange={handleSeriesChange}
          />
        )}
      </div>

      {/* Status bar */}
      <StatusBar total={rows.length} success={successCount} failed={errorCount} phase={phase} />

      {/* Modals */}
      {showApiSetup && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: '8px' }}>
            <ApiKeySetup onDismiss={() => setShowApiSetup(false)} onSaved={() => setShowApiSetup(false)} />
          </div>
        </div>
      )}

      {showPrefs && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: '8px' }}>
            <PreferencesDialog onClose={() => setShowPrefs(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ui && npm test 2>&1
```

Expected: all App tests pass (and all prior tests continue to pass)

- [ ] **Step 5: Run the full test suite**

```bash
cd ui && npm test 2>&1
```

Expected: all test files pass (types, hooks ×3, components ×5, App)

- [ ] **Step 6: Commit**

```bash
git add ui/src/App.tsx ui/src/App.test.tsx
git commit -m "feat(ui): wire App.tsx — full drop→parse→lookup→table→rename data flow"
```

---

## Task 13: Playwright E2E Tests

**Why:** UI integration tests — validates drag-drop with mixed files, table interactions, and preferences dialog in a running Tauri webview. Per the research doc, Playwright is the recommended tool for this.

**Note:** These tests require a running Tauri dev server. They are integration tests, not unit tests, and are run separately from the vitest suite.

**Files:**
- Create: `ui/e2e/smoke.spec.ts`
- Create: `ui/playwright.config.ts`

---

- [ ] **Step 1: Install Playwright**

```bash
cd ui && npm install --save-dev @playwright/test && npx playwright install chromium 2>&1
```

Expected: chromium browser downloaded

- [ ] **Step 2: Create `ui/playwright.config.ts`**

```typescript
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
```

- [ ] **Step 3: Create `ui/e2e/smoke.spec.ts`**

```typescript
import { test, expect } from '@playwright/test';

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
```

**Note on drag-drop E2E testing:** Tauri native drag-drop events (`tauri://drag-drop`) cannot be simulated via Playwright's `page.dispatchEvent` because they originate from the OS, not the browser. To test the full drag-drop-to-rename flow in E2E, use Tauri's `app.emit()` from a test helper script or the `tauri-driver` project. Document this limitation in a comment in the spec file.

- [ ] **Step 4: Add E2E script to `ui/package.json`**

```json
"scripts": {
  "dev": "vite",
  "build": "tsc && vite build",
  "preview": "vite preview",
  "test": "vitest run",
  "test:watch": "vitest",
  "test:e2e": "playwright test"
},
```

- [ ] **Step 5: Run E2E smoke tests** (requires `npm run dev` to be running in another terminal OR Tauri dev to be running)

```bash
cd ui && npm run test:e2e 2>&1
```

Expected:
```
Running 4 tests using 1 worker
✓ smoke.spec.ts > app renders TVRenamer heading
✓ smoke.spec.ts > drop zone instruction is shown with no files
✓ smoke.spec.ts > Preferences button opens preferences dialog
✓ smoke.spec.ts > Rename Selected button is present
4 passed (3s)
```

- [ ] **Step 6: Commit**

```bash
git add ui/playwright.config.ts ui/e2e/ ui/package.json ui/package-lock.json
git commit -m "test(ui): add Playwright E2E smoke tests for app shell"
```

---

## Post-Write Assumption Validation

### ✅ Validated (10)
- `dragDropEnabled: true` fires `tauri://drag-drop` with `{ paths: string[] }` payload (also includes `position` — unused by plan)
- `@tauri-apps/api/core` is correct IPC import for Tauri v2
- `@tauri-apps/api/event` is correct event import for `listen()`
- TanStack Table v8 `createColumnHelper` and `columnHelper.display()` are valid
- TanStack Table v8 `getCoreRowModel()` imported from `@tanstack/react-table`
- `@tanstack/react-virtual` v3 is compatible with React 18
- `@testing-library/react` `renderHook` is available as a named export (merged in v14+)
- `listen()` returns `Promise<UnlistenFn>` — correct usage in hooks
- `save_preferences` parameter `new_prefs` becomes `newPrefs` in JS invoke (Tauri camelCases snake_case params) — confirmed in `ipc.rs`
- `@tauri-apps/plugin-dialog` `open({ directory: true, multiple: false })` returns `string | null`

### ⚠️ Unverified (Low Risk)
- `String.prototype.replaceAll` requires Node.js 18+. Used in `applyTemplate`. Verify project Node target.
- Vitest `vi.mock()` in `test-setup.ts` with `globals: true`: valid, but do NOT import mocked modules inside the setup file itself (confirmed Vitest pattern).

### Bug Fixed During Review
- **CRITICAL FIX**: App.tsx used `useState(() => {...})` to load preferences, which stores a function object as state instead of running a side effect. Fixed to `useEffect(() => {...}, [])`.

---

## Known Gaps & Open Questions

1. **Dest path construction when `move_selected: true`:** `App.tsx` currently computes `dest` as same directory + new filename. When `move_selected` is true, dest should be `dest_dir/season_folder/computedNewName.ext`. Implement after confirming preferences are loaded correctly.

2. **File extension preservation:** The `computedNewName` from `applyTemplate` does not include the file extension. The renamer must append the original extension: `computedNewName + path.extname(sourcePath)`. Add this to `handleDrop` and `handleSeriesChange`.

3. **Season folder construction:** When `move_selected: true`, the destination includes `{dest_dir}/{season_prefix}{season_number}/{computedNewName}.ext`. The `season_prefix_leading_zero` field controls zero-padding the season number.

4. **`keyring` on headless Linux:** The `save_tmdb_key` IPC will fail on headless Linux without a Secret Service daemon. The frontend should catch this error and show a manual fallback (store in prefs.json with a warning).

5. **TMDB Season 0 (specials):** The parser does not extract season 0 from typical filenames. Known gap — document in UI as "Specials (Season 0) are not supported."

6. **Row virtualisation:** `@tanstack/react-virtual` is installed but not yet wired into `FileTable`. Add once table exceeds 100 rows in practice.

7. **`perform_renames` argument name:** The IPC command in Rust uses `renames: Vec<RenameRequest>`. Verify the Tauri invoke argument key name matches exactly — it should be `renames` (snake_case from `#[tauri::command]` parameter name).

---
