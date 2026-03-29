# TVRenamer Modernisation Design

**Date:** 2026-03-29
**Status:** Draft

---

## Problem Statement

TVRenamer is a mature, cross-platform desktop TV file renaming utility that suffers from three compounding problems:

1. **Dead UI framework**: SWT 4.3 (2013) — no longer actively maintained, increasingly difficult to package on modern macOS
2. **Discontinued API**: TheTVDB v1 is discontinued; the codebase even includes a `DiscontinuedApiException`
3. **Java 8 runtime dependency**: Conflicts with modern macOS and requires JRE bundling (~80MB installers)

The goal is to modernise the full stack — language, UI framework, and metadata provider — while preserving complete feature parity and cross-platform support on Windows, macOS, and Linux.

---

## Success Criteria

1. All 8 filename parsing patterns from the Java version produce identical results on the same test inputs
2. TMDB lookup correctly matches shows and episodes for the same filenames TheTVDB previously handled
3. File rename and move operations are non-destructive (no data loss on conflict)
4. App ships as a self-contained binary on all 3 platforms with no runtime dependency
5. Preferences round-trip cleanly; XML-to-JSON migration runs silently on first launch
6. App passes a manual smoke test: drag in 10 mixed TV episode files, confirm matches, rename, verify output

---

## Constraints & Out of Scope

**Must not change:**
- Core feature set: filename parsing, show lookup, episode matching, rename format, file moving, batch processing, drag-and-drop, preferences, show overrides
- Cross-platform target: Windows, macOS, Linux must all be supported equally

**Out of scope:**
- New features beyond feature parity
- Server/hosted mode
- Mobile platforms

---

## Approach: Tauri (Rust backend + TypeScript/React frontend)

### Why Tauri

Tauri produces a self-contained native binary per platform (~10-15MB vs ~80MB+ JRE bundle) with no runtime dependency. The Rust backend handles all OS-level operations; a WebView frontend handles the UI. Tauri v2 is confirmed to support Windows, macOS, and Linux as first-class targets with full drag-and-drop file ingestion support.

### Architecture

**Rust core (`src-tauri/`)**

| Module | Responsibility |
|--------|---------------|
| `parser` | Filename regex engine — port all 8 existing patterns to Rust regex crate |
| `tmdb` | Async TMDB v3 client using `reqwest` crate |
| `renamer` | Applies rename format template and executes file moves/renames |
| `prefs` | Serialises/deserialises preferences to `~/.tvrenamer/prefs.json` via `serde_json` |

Tauri commands expose each module as a typed IPC call to the frontend.

**TypeScript/React frontend (`src/`)**

| Component | Responsibility |
|-----------|---------------|
| Main table | Sortable, editable table of files using TanStack Table; per-row status indicators |
| Drag-and-drop | OS file drop via Tauri `drop` event |
| Preferences dialog | Mirrors current PreferencesDialog, including rename-token drag-and-drop builder |
| Status bar | Per-file rename/move progress via Tauri event emitter |

**Data flow**

1. User drops files → Tauri `drop` event fires → Rust `parser` extracts show name, season, episode
2. Parsed show name → Rust `tmdb` calls TMDB v3 API → returns episode match
3. User reviews table, optionally edits matches → confirms rename/move
4. Rust `renamer` executes file operations; progress events pushed to frontend
5. Frontend updates per-row status in real time

### Metadata Provider: TMDB v3

TheTVDB is replaced with TMDB (The Movie Database). TMDB v3 REST API endpoints used:
- `GET /3/search/tv?query=<show>` — search for shows
- `GET /3/tv/{id}/season/{season}/episode/{episode}` — retrieve episode details

**API key model**: Users must obtain their own free TMDB API key (registration at themoviedb.org) and enter it once in Preferences. The key is stored in `~/.tvrenamer/prefs.json`. Do not bundle an application-level key in the source repository (TMDB policy unclear for open-source; security best practice is per-user keys).

### Preferences Migration

On first launch, the Rust startup routine checks for `~/.tvrenamer/preferences.xml`. If found, it reads and migrates all settings to `~/.tvrenamer/prefs.json`. The XML file is left in place as a backup.

**Important**: Tauri v2 uses a scoped file system permissions model. Access to `~/.tvrenamer/` must be explicitly declared in `src-tauri/capabilities/default.json` using the `$HOME/.tvrenamer/*` glob pattern. This is a one-time configuration step.

Similarly, `~/.tvrenamer/overrides.xml` (show name mappings) is migrated to a JSON equivalent on first launch.

### Build & Distribution

| Platform | Format | Size |
|----------|--------|------|
| macOS | `.dmg` with signed `.app` | ~12MB |
| Windows | `.msi` installer | ~12MB |
| Linux | `.deb`, `.rpm`, `.AppImage` | ~12MB |

CI: GitHub Actions using Tauri's official `tauri-action` workflow, which supports parallel cross-platform builds from a single workflow file.

### Testing Strategy

- Rust unit tests for `parser` module — port existing JUnit FilenameParserTest inputs/outputs verbatim
- Rust unit tests for `renamer` — port MoveTest and ConflictTest logic
- Playwright for UI integration tests (drag-and-drop, table interactions, preferences dialog)

---

## Open Questions

1. **Rename conflict handling**: Spec the full conflict detection and resolution logic before implementing `renamer` (the current Java ConflictTest is the reference)
2. **Show overrides format**: Keep the current string-mapping approach in JSON, or build a more structured lookup with fuzzy matching?
3. **Update checker**: Keep the existing tvrenamer.org version check, or move to GitHub Releases API?
4. **TMDB API key onboarding**: Design a first-launch flow that guides users through obtaining and entering their TMDB key with minimal friction

---

## Assumption Validation Results

| Assumption | Status |
|------------|--------|
| Tauri v2 multi-platform (Windows/macOS/Linux) | ✅ Valid |
| Tauri v2 OS-level drag-and-drop | ✅ Valid |
| TMDB v3 TV search endpoint | ✅ Valid |
| TMDB v3 episode lookup endpoint | ✅ Valid |
| TMDB free tier availability | ✅ Valid |
| `reqwest` async HTTP client for Rust | ✅ Valid |
| TanStack Table: large row count + editable + indicators | ✅ Valid |
| `tauri-action` cross-platform CI | ✅ Valid |
| Tauri arbitrary home dir access without scope config | ❌ Invalid — requires explicit `$HOME/.tvrenamer/*` in capabilities |
| TMDB bundled app-level API key permitted | ⚠️ Unverified — require per-user key instead |
