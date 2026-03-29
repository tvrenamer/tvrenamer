# Research: Frontend UI (React + Tauri)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Build the React/TypeScript frontend for TVRenamer using TanStack Table, with drag-and-drop file ingestion, per-row status indicators, preferences dialog, and real-time rename progress. Replaces the SWT-based `ResultsTable`, `UIStarter`, and `PreferencesDialog`.

---

## Table Layout

4 columns in the main results table:

| Column | Width | Type |
|--------|-------|------|
| Checkbox | 30px | Row selection |
| Current File | 550px | Read-only filename |
| New Filename/Path | 550px | ComboField — dropdown for multiple match options |
| Status | 60px | Per-row status indicator |

---

## TanStack Table v8 Patterns

- Use `getCoreRowModel()` + `getSortedRowModel()` for sortable table
- Per-row editable cells: implement `table.options.meta?.updateData(rowIndex, columnId, value)` pattern
- Row selection: `enableRowSelection: true` + `onRowSelectionChange`
- Virtualization: needed at >50-100 rows (`@tanstack/react-virtual`)
- Per-row async state: use `useMutationState` (TanStack Query v5) — do NOT create one `useQuery` hook per row

---

## Drag-and-Drop Configuration

Two approaches in Tauri v2 — choose one:

**Option A: Tauri native events** (`dragDropEnabled: true`, the default)
- Fires `tauri://drag-enter`, `tauri://drag-over`, `tauri://drag-drop`, `tauri://drag-leave`
- Payload of `tauri://drag-drop`: `string[]` — array of OS file paths
- Gives direct file path access (needed for rename operations)

**Option B: HTML5 drag-drop API** (`dragDropEnabled: false`)
- Disables Tauri's internal drag system, enables standard HTML5 File API
- May have path limitations in webview

**Recommendation:** Use Tauri native events (Option A) since the app needs OS-level file paths for rename operations.

```typescript
import { listen } from '@tauri-apps/api/event';

const unlisten = await listen<{ paths: string[] }>('tauri://drag-drop', (event) => {
  const filePaths = event.payload.paths;
  // Send to Rust parser via IPC
});
```

---

## Tauri IPC Pattern

**Invoking Rust commands from frontend:**
```typescript
import { invoke } from '@tauri-apps/api/core';  // NOT @tauri-apps/api/tauri

const results = await invoke<Show[]>('search_shows', { query: 'Breaking Bad' });
```

**Receiving progress events from Rust:**
```typescript
import { listen } from '@tauri-apps/api/event';

const unlisten = await listen<RenameProgress>('rename-progress', (event) => {
  updateRowStatus(event.payload.fileId, event.payload.status);
});
```

Note: `app.emit()` broadcasts globally; `app.emit_to("main", ...)` targets a specific window. Use the latter for progress events.

---

## Data Flow (Frontend Perspective)

1. User drops files → `tauri://drag-drop` event → extract file paths
2. Invoke `parse_files` Rust command → receive `ParseResult[]`
3. For each parsed show, invoke `search_shows` → receive `ShowResult[]`
4. Populate table rows with parsed data + show matches
5. User reviews, optionally selects alternative matches from dropdown
6. User clicks Rename → invoke `rename_files` command
7. Listen for `rename-progress` events → update per-row status in real time
8. On completion, update row status indicators

---

## Preferences Dialog

Mirrors the current Java `PreferencesDialog`. Key features:
- All 12 preference fields (see preferences module research)
- Rename-token drag-and-drop builder for constructing `renameReplacementMask`
- TMDB API key input with "Test" button
- Destination directory picker (via `@tauri-apps/plugin-dialog`)

---

## Status Bar

Per-file rename/move progress via Tauri event emitter. Shows:
- Current operation (parsing, looking up, renaming, moving)
- Success/failure count
- Overall progress

---

## Component Architecture

```
src/
├── App.tsx                    # Main layout, drag-drop listener
├── components/
│   ├── FileTable.tsx          # TanStack Table with selection, sorting
│   ├── FileRow.tsx            # Per-row status indicator + match dropdown
│   ├── StatusBar.tsx          # Progress summary
│   ├── PreferencesDialog.tsx  # Settings modal
│   ├── ApiKeySetup.tsx        # First-launch TMDB key onboarding
│   └── TokenBuilder.tsx       # Drag-and-drop rename template builder
├── hooks/
│   ├── useTauriDrop.ts        # Wrapper for tauri://drag-drop
│   ├── useRenameProgress.ts   # Wrapper for rename-progress events
│   └── usePreferences.ts      # Load/save preferences via IPC
└── types/
    └── index.ts               # Shared TypeScript types
```

---

## Drag-Drop Gotchas

1. **`dragDropEnabled` vs `fileDropEnabled`**: The v1 config key `fileDropEnabled` doesn't exist in v2 — it's `dragDropEnabled`. A common migration mistake.
2. **Semantics are counterintuitive**: `dragDropEnabled: false` enables HTML5 drag-drop APIs; `true` (default) enables Tauri native events. Verify before implementing.

---

## Test Coverage Gaps (Write From Scratch)

- **Drag-and-drop with mixed valid/invalid files** — no tests in Java
- **UI integration tests** — Playwright recommended for drag-drop, table interactions, preferences dialog

---

## File Change Hotspots (Java Reference)

The Java files being replaced:
- `UIStarter.java` (162 changes) — most complex to replace, main UI orchestration
- `ResultsTable.java` — table rendering and interaction

These carry significant UI logic that must be understood before porting.

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| Tauri v2 `app.emit()` broadcasts globally; `emit_to("main", ...)` targets specific window | ✅ Valid |
| Tauri v2 IPC import is `@tauri-apps/api/core` | ✅ Valid |
| TanStack Query v5 `useMutationState` is correct for per-row mutation state | ✅ Valid |
| Tauri v2 OS-level drag-and-drop | ✅ Valid |
| `@tanstack/react-table = "^8.17"` — current latest is 8.21+ | ⚠️ `^8.17` resolves correctly |
