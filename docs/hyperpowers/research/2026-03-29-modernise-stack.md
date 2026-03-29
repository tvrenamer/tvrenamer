# Research: TVRenamer Modernisation (Java/SWT → Tauri)

> Generated: 2026-03-29
> Design Doc: docs/hyperpowers/designs/2026-03-29-modernise-stack-design.md

---

## Original Design Document

# TVRenamer Modernisation Design

**Date:** 2026-03-29
**Status:** Draft

---

### Problem Statement

TVRenamer is a mature, cross-platform desktop TV file renaming utility that suffers from three compounding problems:

1. **Dead UI framework**: SWT 4.3 (2013) — no longer actively maintained, increasingly difficult to package on modern macOS
2. **Discontinued API**: TheTVDB v1 is discontinued; the codebase even includes a `DiscontinuedApiException`
3. **Java 8 runtime dependency**: Conflicts with modern macOS and requires JRE bundling (~80MB installers)

The goal is to modernise the full stack — language, UI framework, and metadata provider — while preserving complete feature parity and cross-platform support on Windows, macOS, and Linux.

---

### Success Criteria

1. All 8 filename parsing patterns from the Java version produce identical results on the same test inputs
2. TMDB lookup correctly matches shows and episodes for the same filenames TheTVDB previously handled
3. File rename and move operations are non-destructive (no data loss on conflict)
4. App ships as a self-contained binary on all 3 platforms with no runtime dependency
5. Preferences round-trip cleanly; XML-to-JSON migration runs silently on first launch
6. App passes a manual smoke test: drag in 10 mixed TV episode files, confirm matches, rename, verify output

---

### Constraints & Out of Scope

**Must not change:**
- Core feature set: filename parsing, show lookup, episode matching, rename format, file moving, batch processing, drag-and-drop, preferences, show overrides
- Cross-platform target: Windows, macOS, Linux must all be supported equally

**Out of scope:**
- New features beyond feature parity
- Server/hosted mode
- Mobile platforms

---

### Approach: Tauri (Rust backend + TypeScript/React frontend)

#### Why Tauri

Tauri produces a self-contained native binary per platform (~10-15MB vs ~80MB+ JRE bundle) with no runtime dependency. The Rust backend handles all OS-level operations; a WebView frontend handles the UI. Tauri v2 is confirmed to support Windows, macOS, and Linux as first-class targets with full drag-and-drop file ingestion support.

#### Architecture

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

#### Metadata Provider: TMDB v3

TheTVDB is replaced with TMDB (The Movie Database). TMDB v3 REST API endpoints used:
- `GET /3/search/tv?query=<show>` — search for shows
- `GET /3/tv/{id}/season/{season}/episode/{episode}` — retrieve episode details

**API key model**: Users must obtain their own free TMDB API key (registration at themoviedb.org) and enter it once in Preferences. The key is stored in `~/.tvrenamer/prefs.json`. Do not bundle an application-level key in the source repository (TMDB policy unclear for open-source; security best practice is per-user keys).

#### Preferences Migration

On first launch, the Rust startup routine checks for `~/.tvrenamer/preferences.xml`. If found, it reads and migrates all settings to `~/.tvrenamer/prefs.json`. The XML file is left in place as a backup.

**Important**: Tauri v2 uses a scoped file system permissions model. Access to `~/.tvrenamer/` must be explicitly declared in `src-tauri/capabilities/default.json` using the `$HOME/.tvrenamer/*` glob pattern. This is a one-time configuration step.

Similarly, `~/.tvrenamer/overrides.xml` (show name mappings) is migrated to a JSON equivalent on first launch.

#### Build & Distribution

| Platform | Format | Size |
|----------|--------|------|
| macOS | `.dmg` with signed `.app` | ~12MB |
| Windows | `.msi` installer | ~12MB |
| Linux | `.deb`, `.rpm`, `.AppImage` | ~12MB |

CI: GitHub Actions using Tauri's official `tauri-action` workflow, which supports parallel cross-platform builds from a single workflow file.

#### Testing Strategy

- Rust unit tests for `parser` module — port existing JUnit FilenameParserTest inputs/outputs verbatim
- Rust unit tests for `renamer` — port MoveTest and ConflictTest logic
- Playwright for UI integration tests (drag-and-drop, table interactions, preferences dialog)

---

### Open Questions

1. **Rename conflict handling**: Spec the full conflict detection and resolution logic before implementing `renamer` (the current Java ConflictTest is the reference)
2. **Show overrides format**: Keep the current string-mapping approach in JSON, or build a more structured lookup with fuzzy matching?
3. **Update checker**: Keep the existing tvrenamer.org version check, or move to GitHub Releases API?
4. **TMDB API key onboarding**: Design a first-launch flow that guides users through obtaining and entering their TMDB key with minimal friction

---

### Assumption Validation Results

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

---

## Resolved Questions

| Question | Resolution | Source |
|----------|------------|--------|
| Rename conflict handling spec | **Automatic index-based versioning**: MoveRunner pre-scans all destinations before any moves execute. Conflicting files are sorted by size (largest = lowest index), assigned indices (2, 3, 4…), and moved to a `versions/` subdirectory with the format `basename (N).ext`. The move-time `Files.exists()` check is a safety net for race conditions only. No user prompt. | Error Handling Analyst + Codebase Analyst (ConflictTest.java:66-131, FileMover.java:301-331, MoveRunner.java:160-216) |
| Show overrides format | **Keep simple JSON exact-match map**. Current `GlobalOverrides` is a `HashMap<String,String>` that is loaded but **never called** during show lookup (orphaned code). Default entries handle year-disambiguation (e.g., "Archer (2009)" → "Archer"). No fuzzy matching needed; TMDB/TheTVDB handle similarity server-side. Port as JSON array `[{"from":"…","to":"…"}]`. Apply immediately after filename parser output, before provider query. | Codebase Analyst + Architecture Analyst + Show-Overrides agent (GlobalOverrides.java:45-51, default-overrides.xml) |
| Update checker | **Move to GitHub Releases + Tauri updater plugin**. tvrenamer.org is still live (returns "0.8") but requires manual maintenance. Tauri v2 has a first-class updater plugin that integrates with GitHub Releases via a `latest.json` manifest auto-generated by `tauri-action`. Cryptographic signing is built in. No custom version comparison code needed. | Framework Docs + Git History + Update-Checker agent (UpdateChecker.java:26-34, Tauri updater docs) |
| TMDB API key onboarding | **3-step non-blocking modal on first launch**: (1) Explain why the key is needed; (2) direct link to `https://www.themoviedb.org/settings/api`; (3) input field + "Test" button that calls `GET /3/authentication`. Validate before saving. Show graceful degradation banner if key is absent. Key stored in `~/.tvrenamer/prefs.json`. Do NOT block app usage if skipped. | Best-Practices + TMDB-Onboarding agent (TMDB reference docs, Tauri Store plugin docs) |

---

## Executive Summary

1. **The Java codebase is a clean MVC port target.** Separation between model (`org.tvrenamer.model`), controller (`org.tvrenamer.controller`), and view (`org.tvrenamer.view`) is genuine. Business logic in the controller layer does not leak into the SWT view, making it extractable.

2. **All 8 filename parsing regex patterns are extractable verbatim.** 95 test cases exist in `FilenameParserTest.java` with inputs and expected outputs. These are the golden test suite for the Rust `parser` module. The patterns use no lookahead/lookbehind, so they translate directly to the Rust `regex` crate without modification.

3. **Conflict handling is fully automatic with a defined algorithm.** MoveRunner pre-scans destinations, sorts conflicting files by size, and assigns sequential version indices before any move executes. The `versions/` subdirectory pattern and `" (N)"` filename suffix are the established behaviour. 4 ConflictTest scenarios define the exact expected outcomes.

4. **Show overrides are orphaned code — never applied during lookup.** `GlobalOverrides.getShowName()` exists and loads the XML file, but is never called anywhere in the production codebase. The Tauri port must wire this into the lookup flow (after parsing, before provider query) as well as migrate the format to JSON.

5. **Tauri v2 requires explicit capability declarations for home directory access.** `$HOME/.tvrenamer/**` must be declared in `src-tauri/capabilities/default.json`. The drag-drop event system also has a counterintuitive configuration: set `dragDropEnabled: false` in window config to enable custom frontend handling.

6. **API key storage should use the OS keychain (`keyring` crate), not plaintext JSON.** Best practices research confirms platform-native secure storage (macOS Keychain, Windows Credential Manager, Linux Secret Service) is the correct approach. The design doc's plan to store in `prefs.json` should be updated.

7. **The update checker should move to GitHub Releases + Tauri updater plugin.** tvrenamer.org is operational but represents manual maintenance overhead. Tauri's built-in updater plugin with tauri-action automation eliminates all custom version-checking code.

8. **TMDB rate limits are generous but require connection pool reuse.** 50 req/sec, 20 concurrent connections. The Rust `reqwest::Client` must be a single static instance (via `once_cell` or `AppHandle` state) — creating a new client per API call eliminates connection pooling and causes significant overhead.

---

## Codebase Analysis

*From Codebase Analyst — FilenameParser.java, FileMover.java, UserPreferences.java, GlobalOverrides.java, ResultsTable.java*

### The 8 Filename Parsing Regex Patterns (Verbatim)

Located at `src/main/java/org/tvrenamer/controller/FilenameParser.java:26-52`:

```java
private static final String[] REGEX = {
    // Pattern 1: SxxExx (e.g., "Show.S01E05")
    "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d*)[eE](\\d\\d*).*",

    // Pattern 2: "Season-XX-Episode-XX"
    "(.+?[^a-zA-Z0-9]\\D*?)Season[- ](\\d\\d*)[- ]?Episode[- ](\\d\\d*).*",

    // Pattern 3: "sXX.eXX" flexible separators
    "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d*)\\D*?[eE](\\d\\d*).*",

    // Pattern 4: "SSxEE" with optional "S"
    "(.+[^a-zA-Z0-9]\\D*?)[Ss](\\d\\d?)x(\\d\\d\\d?).*",

    // Pattern 5: titles with 4-digit year
    "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*",

    // Pattern 6: "SXXYY" (exactly 4 digits: season+episode)
    "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d)(\\d\\d)\\D.*",

    // Pattern 7: Fallback — show name + two 1-2 digit numbers
    "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*",

    // Pattern 8: Last resort — minimal structure
    "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*"
};
```

Each pattern is compiled twice: once with a resolution suffix regex (`\\D(\\d+[pk]).*`) and once without, giving 16 compiled patterns total. Patterns are tried in order; first match wins.

**Rust regex crate compatibility:** All 8 patterns use standard character classes and quantifiers only — no lookahead/lookbehind. They translate directly. Use named capture groups for clarity: `(?P<show>...)`.

### UserPreferences Schema (12 Fields)

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `preloadFolder` | String | null | Auto-load directory on startup |
| `destDir` | String | `~/TV` | Destination for moved files |
| `seasonPrefix` | String | `"Season "` | Season folder prefix |
| `seasonPrefixLeadingZero` | boolean | false | Pad season numbers |
| `moveSelected` | boolean | false | Enable moving files to destDir |
| `renameSelected` | boolean | true | Enable file renaming |
| `removeEmptiedDirectories` | boolean | true | Delete empty source dirs |
| `deleteRowAfterMove` | boolean | false | Auto-remove row after move |
| `renameReplacementMask` | String | `"%S [%sx%0e] %t"` | Filename template |
| `checkForUpdates` | boolean | true | Check for updates at startup |
| `recursivelyAddFolders` | boolean | true | Scan subfolders |
| `ignoreKeywords` | List\<String\> | `["sample"]` | Patterns to ignore |

### Table Columns

4 columns in the main results table: Checkbox (30px), Current File (550px), New Filename/Path (550px — ComboField with dropdown for multiple match options), Status (60px).

### Show Overrides Format

```xml
<!-- etc/default-overrides.xml -->
<overrides>
  <showNames>
    <entry>
      <string>Archer (2009)</string>
      <string>Archer</string>
    </entry>
    <entry>
      <string>House of Cards (2013)</string>
      <string>House of Cards</string>
    </entry>
    <entry>
      <string>The Newsroom (2012)</string>
      <string>The Newsroom</string>
    </entry>
  </showNames>
</overrides>
```

Maps year-disambiguated provider names → simplified names. The `getShowName()` method returns the original if no override exists. **Critical finding**: this method is never called in production code — the feature is implemented but not wired into the lookup flow.

---

## Git History Insights

*From Git History Analyzer — git log analysis, commit archaeology*

### TheTVDB API Deprecation Timeline

- **2017-05-24**: Attempted migration to TheTVDB Swagger/REST API (commits a504583, 1432afd). Vipul Delwadia led this. The Swagger implementation was completed and merged.
- **2017-11-20**: `DiscontinuedApiException` added (commit 0bf81e6) when TheTVDB announced sunset of v1 API December 2017.
- **Later**: Sunset date removed from detection logic (commit 7a82864) when TheTVDB postponed without a new date. The API limped on in an undefined state.
- **Result**: The codebase today has both v1 XML API code and a partial Swagger implementation. TheTVDB's eventual full shutdown means the app has been non-functional for show lookup for some time.

### Update Checker History

- **2010-10-18**: Initial check against `http://r.ac.nz/tvrenamer.version` (commit c712a72)
- **2017-04-21**: Refactored to `TVRENAMER_VERSION_URL = "http://tvrenamer.org/version"` (commit f2a1d39)
- **Status now**: tvrenamer.org is still live and returns plain-text version "0.8". The endpoint requires manual updates after each release — there is no automation.

### Filename Parser Evolution

- Patterns grew incrementally as real-world edge cases were encountered.
- Added 3-digit season support in 2018 (commit 5d664dc) for shows like House Hunters International (S103E02).
- Added numeric-only pattern in 2017 (commit a93031b) for filenames like `dexter.407.mp4`.
- The test suite grew in parallel — this is a well-validated component.

### Conflict Handling History

- Conflict resolution introduced 2017-04-24 (commit 03c5216): strategy is to place the largest file at the primary destination, others in `versions/` subdirectory with index suffix.
- Comprehensive tests added 2018-11-25 (commit 67ce3b0): double, triple, cascading conflicts.
- The "largest file = best quality" assumption is hard-coded and not configurable.

### File Change Hotspots (Most Changed)

1. UIStarter.java (162 changes) — main UI orchestration
2. FileEpisode.java (99 changes) — core domain model
3. Show.java (58 changes) — series metadata
4. Constants.java (59 changes) — config and messages
5. FileMover.java + ShowStore.java (~41 changes each)

**Implication**: UIStarter.java is the most complex to replace. FileEpisode.java carries significant domain logic that must be ported faithfully.

### Primary Contributors

- **John Valente** (552 commits): Deep file operations, conflict resolution, API handling expertise
- **Vipul Delwadia** (167+ commits): API modernisation (Swagger attempt), Docker, recent CI work
- **Dave Harris** (106 commits): Early architecture, SWT framework, original UpdateChecker

---

## Framework & Documentation

*From Framework Docs Researcher — Tauri v2 docs, TMDB API, reqwest, TanStack Table*

### Tauri v2: Critical Configuration

**Drag-and-drop** (⚠️ verify before implementing):
- `dragDropEnabled: false` enables the **HTML5 drag-drop API** in the frontend (disables Tauri's native drag system)
- `dragDropEnabled: true` (default) fires Tauri system events: `tauri://drag-enter`, `tauri://drag-over`, `tauri://drag-drop`, `tauri://drag-leave`
- Payload of `tauri://drag-drop`: `string[]` — array of file paths
- **Decision needed**: choose one approach (Tauri native events vs. HTML5 File API). Tauri native events give OS-level file path access; HTML5 File API works in webview but may have path limitations. Verify current Tauri v2 docs for the correct config for each approach.

**File system capability** for `~/.tvrenamer/` (⚠️ verify format against current Tauri v2 docs — `"fs:scope"` identifier was flagged as incorrect):
```json
{
  "identifier": "default",
  "permissions": [
    "fs:allow-home-read",
    "fs:allow-home-write",
    "fs:allow-home-read-recursive",
    "fs:allow-home-write-recursive"
  ],
  "windows": ["*"]
}
```
Scope path globs (`$HOME/.tvrenamer/**`) are declared separately in plugin scope config — verify the exact format in `src-tauri/capabilities/` against the current Tauri v2 plugin-fs documentation.

**IPC command pattern:**
```rust
#[tauri::command]
async fn search_shows(query: String) -> Result<Vec<Show>, String> { ... }
```
```typescript
const results = await invoke<Show[]>('search_shows', { query: 'Breaking Bad' });
```

**Progress events (Rust → frontend):**
```rust
app.emit("rename-progress", DownloadProgress { ... })?;
```
```typescript
const unlisten = await listen<RenameProgress>('rename-progress', (event) => { ... });
```

### Tauri v2 Breaking Changes from v1

| Component | v1 | v2 |
|-----------|----|----|
| IPC import | `@tauri-apps/api/tauri` | `@tauri-apps/api/core` |
| File system | `readBinaryFile` | `readFile` |
| Allowlist | `tauri.allowlist.*` | Capabilities system |
| Drag-drop event (JS) | `tauri://file-drop` | `tauri://drag-drop` |
| Drag-drop config | `fileDropEnabled` | `dragDropEnabled` |
| Window type (Rust) | `Window` | `WebviewWindow` |

### TMDB v3 API

**Search endpoint:** `GET /3/search/tv?api_key=KEY&query=<show>`

Key response fields: `id` (integer — use for episode lookup), `name`, `first_air_date`, `original_language`

**Episode endpoint:** `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}`

Key response fields: `name` (episode title), `air_date`, `episode_number`, `season_number`, `overview`

**Rate limits:** 50 req/sec, 20 concurrent connections per IP — no difference between free and paid tiers. Returns HTTP 429 on excess.

**Authentication:** Bearer token in `Authorization` header is preferred over `api_key` query parameter.

### TanStack Table v8

- Use `getCoreRowModel()` + `getSortedRowModel()` for sortable table
- Per-row editable cells: implement `table.options.meta?.updateData(rowIndex, columnId, value)` pattern
- Row selection: `enableRowSelection: true` + `onRowSelectionChange`
- Virtualization: needed at >50-100 rows (`@tanstack/react-virtual`)
- Per-row async state: use `useMutationState` (TanStack Query v5) — do NOT create one `useQuery` hook per row

---

## Best Practices

*From Best Practices Researcher — Rust patterns, security, UX, updates*

### API Key Storage: Use OS Keychain (`keyring` crate)

```toml
keyring = "3.6"
```

```rust
use keyring::Entry;
let entry = Entry::new("tvrenamer", "tmdb_api_key")?;
entry.set_password(&api_key)?;
let key = entry.get_password()?;
```

Platform mapping: macOS Keychain → Windows Credential Manager → Linux Secret Service. **Do NOT store API key in plaintext `prefs.json`** — this is the key correction to the design doc.

### Atomic File Renames: `tempfile` crate

```toml
tempfile = "3.10"
```

- Use `NamedTempFile::persist_noclobber()` for conflict detection (returns `Err` if target exists — no race window)
- `std::fs::rename` fails across filesystem boundaries on Windows
- `renamore` crate provides `rename_exclusive()` for platform-agnostic atomic exclusive renames

### Cross-Platform Config Paths: `directories` crate

```toml
directories = "5.0"
```

```rust
let proj_dirs = ProjectDirs::from("", "", "tvrenamer")?;
let config_path = proj_dirs.config_dir(); // ~/.config/tvrenamer/ on Linux, etc.
```

**Note**: The design doc uses `~/.tvrenamer/` directly. The `directories` crate provides platform-correct paths (macOS: `~/Library/Application Support/tvrenamer`, Windows: `%APPDATA%\tvrenamer`). Consider adopting this for Tauri port.

### Config Versioning: `serde_flow` crate

```toml
serde_flow = "0.3"
```

Add a `version` field to `prefs.json` from v1.0. Use `#[flow(variant = 2)]` + `From<PrefsV1>` for automatic migration. Alternatively: `#[serde(default)]` for optional new fields with a manual `version` discriminator.

### reqwest: Static Client

Create ONE `reqwest::Client` at startup and share it via `AppHandle` state. Never create a new client per API call — this destroys connection pooling.

```rust
let client = reqwest::Client::builder()
    .connect_timeout(Duration::from_secs(10))
    .timeout(Duration::from_secs(30))
    .build()?;
app.manage(client);
```

### Update Checker: GitHub Releases + Tauri Updater Plugin

Configure in `tauri.conf.json`:
```json
{
  "plugins": {
    "updater": {
      "active": true,
      "pubkey": "YOUR_PUBLIC_KEY",
      "endpoints": [
        "https://github.com/tvrenamer/tvrenamer/releases/latest/download/latest.json"
      ]
    }
  }
}
```

Generate keys: `npm run tauri signer generate`. The `tauri-action` GitHub Action auto-generates `latest.json` and signs all platform artifacts. Zero custom version comparison code needed.

---

## Test Coverage Analysis

*From Test Coverage Analyst — FilenameParserTest.java (95 cases), ConflictTest.java, MoveTest.java*

### FilenameParserTest — All 95 Test Cases (Selected Representative Set)

The full table is in `src/test/java/org/tvrenamer/controller/FilenameParserTest.java`. Key cases:

| Input Filename | Expected Show | Season | Episode | Resolution |
|----------------|--------------|--------|---------|------------|
| `The.Daily.Show.S22E105.D.L.Hughley.HDTV.x264` | `The.Daily.Show` | 22 | 105 | — |
| `Futurama.S07E14.2-D Blacktop.HDTV.x264` | `Futurama` | 7 | 14 | — |
| `House Hunters International.S103E02.mkv` | `House Hunters International` | 103 | 2 | — |
| `24.s08.e01.720p.hdtv.x264-immerse.mkv` | `24` | 8 | 1 | 720p |
| `dexter.407.720p.hdtv.x264-sys.mkv` | `dexter` | 4 | 7 | 720p |
| `game.of.thrones.5x01.mp4` | `game.of.thrones` | 5 | 1 | — |
| `human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv` | `human.target.2010` | 1 | 2 | 720p |
| `castle.2009.s01e09.720p.hdtv.x264-ctu.mkv` | `castle.2009` | 1 | 9 | 720p |
| `the.100.208.hdtv-lol.mp4` | `the.100` | 2 | 8 | — |
| `Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET` | `Marvels.Agents.of.S.H.I.E.L.D` | 3 | 3 | — |
| `law.and.order.svu.1705.hdtv-lol` | `law.and.order.svu` | 17 | 5 | — |
| `Archer.2009.S01E02.Training.Day.mp4` | `Archer.2009` | 1 | 2 | — |
| `Archer (2009)/S01E02 Training Day.mp4` | `Archer (2009)` | 1 | 2 | — |
| `Quintuplets/versions/S01E02.Quintagious~2.avi` | `Quintuplets` | 1 | 2 | — |

All 95 cases must become Rust `#[test]` functions in `src-tauri/src/parser/tests.rs`.

### ConflictTest — 4 Scenarios (Complete Spec)

| Test | Scenario | Expected Outcome |
|------|----------|-----------------|
| `testFileMoverConflict` | Blocking file pre-exists at destination | Source moved to `versions/` as `basename (2).ext` |
| `testMoveRunnerWithConflict` | Same via async MoveRunner | Same outcome + MoveObserver notified |
| `testMoveRunnerWithTwoConflicts` | Blocking file + second obstacle created | Source moved to `versions/` as `basename (3).ext` |
| `testMoveRunnerWithThreeConflicts` | Three cascading obstacles | Source moved to `versions/` as `basename (4).ext` |

**Algorithm** (for Rust port):
```
PRE-MOVE PHASE (before any moves):
  1. Group all pending moves by destination directory
  2. For each destination dir, find all files targeting the same basename
  3. Check Files.exists() for each desired destination path
  4. If conflicts exist (existing files + pending moves > 1):
     a. Sort pending movers by source file size (descending)
     b. Assign indices: first = existing_count + 1, increment for each additional
     c. Set destination for each to: dest_dir/versions/basename (N).ext

MOVE-TIME SAFETY CHECK:
  5. Just before move, verify destination still doesn't exist
  6. If it does: ALREADY_IN_PLACE (if same file) or FAIL_TO_MOVE (race condition)
```

### MoveTest — 5 Scenarios

| Test | Scenario |
|------|----------|
| `testFileMover` | Basic file move succeeds |
| `testFileMoverCannotMove` | Read-only file → FAIL_TO_MOVE |
| `testMoveRunner` | Async move via MoveRunner succeeds |
| `testMoveRunnerCannotMove` | Read-only + async runner |
| `testMoveRunnerCannotMoveWithTimestamp` | Timestamp preservation |

### Critical Coverage Gaps

These paths have **no tests** in the Java codebase and must be written from scratch for the Rust port:

1. **TMDB API integration** — no mock responses, no error path tests
2. **Preferences migration** (XML → JSON) — no tests for the migration itself
3. **Unicode filenames** — no tests for emoji or non-Latin characters
4. **Disk full / permission denied scenarios** — no tests
5. **Very long path names** — no tests
6. **Drag-and-drop with mixed valid/invalid files** — no tests

---

## Error Handling Analysis

*From Error Handling Analyst — FileMover.java, TheTVDBProvider.java, FileEpisode.java*

### Error Type Hierarchy

| Java Type | Rust Equivalent | When to Use |
|-----------|-----------------|-------------|
| `TVRenamerIOException` | Custom enum variant | Wrap all I/O failures |
| `DiscontinuedApiException` | `ApiError::Discontinued` | API sunset detection |
| `FailedShow` | `LookupResult::Failed` | Show lookup failure with timeout flag |

### Recommended Rust Error Enum

```rust
#[derive(thiserror::Error, Debug, serde::Serialize)]
pub enum AppError {
    #[error("API key invalid or missing")]
    ApiKeyMissing,
    #[error("API discontinued")]
    ApiDiscontinued,
    #[error("Network timeout: {0}")]
    NetworkTimeout(String),
    #[error("File not found: {0}")]
    FileNotFound(String),
    #[error("Permission denied: {0}")]
    PermissionDenied(String),
    #[error("Destination already exists")]
    DestinationExists,
    #[error("Parse failed: no pattern matched")]
    ParseFailed,
    #[error("Preferences corrupted")]
    PreferencesCorrupted,
}
```

### User-Facing Error Messages (Port These Verbatim)

From `Constants.java:191-202`:

| State | Message |
|-------|---------|
| Fetching | `"Downloading ..."` |
| Lookup failed | `"Unable to find show information"` |
| Episode not found | `"Could not get episode for show"` |
| Network timeout | `"Timed out trying to look up"` |
| Parse failure | `"Did not extract show name from filename"` |
| General download failure | `"Downloading show listings failed. Check internet connection"` |

### Key Error Handling Improvements for Rust Port

1. **Rate limiting (429)**: Java treats 429 as generic failure. Rust port must implement exponential backoff.
2. **API key validation**: Validate at input time (`GET /3/authentication`) before saving — catches typos immediately.
3. **Preferences missing vs. corrupted**: Java returns `null` for both. Rust should use distinct enum variants.
4. **Partial copy cleanup**: Already implemented in Java (copies to temp, deletes on failure). Port this pattern.
5. **Disk space check**: Java has none. Add a pre-move disk space check in Rust.

---

## Dependency Analysis

*From Dependency Analyst — build.gradle, ivy.xml*

### Current Java Dependencies

| Dependency | Version | Rust/TS Replacement |
|------------|---------|---------------------|
| SWT (all platforms) | 4.3 | Tauri v2 webview + React |
| XStream | 1.4.9 | `serde` + `serde_json` |
| OkHttp3 | 3.8.0 | `reqwest` |
| OkIO | 1.13.0 | `tokio` |
| commons-codec | 1.4 | `base64` crate |
| xmlpull + xpp3 | legacy | JSON (no XML needed for TMDB) |
| java.util.logging | native | `tracing` + `tracing-subscriber` |

### Recommended Rust Cargo.toml (Core)

```toml
[dependencies]
tauri = { version = "2", features = [] }
serde = { version = "1", features = ["derive"] }
serde_json = "1"
reqwest = { version = "0.13", features = ["json"] }  # v0.13.2 as of early 2026
tokio = { version = "1", features = ["full"] }
regex = "1"                                          # verify no lookahead/lookbehind needed; use fancy_regex if required
thiserror = "1"
tracing = "0.1"
tracing-subscriber = "0.3"
keyring = "3.6"
tempfile = "3.10"
directories = "6.0"                                  # v6.0.1 as of January 2025
# NOTE: once_cell superseded by std::sync::OnceLock (stable since Rust 1.70)
```

### Required Tauri v2 Plugins

| Plugin | Purpose |
|--------|---------|
| `@tauri-apps/plugin-fs` | File system operations |
| `@tauri-apps/plugin-dialog` | Native file picker, message boxes |
| `@tauri-apps/plugin-updater` | Auto-update via GitHub Releases |
| `@tauri-apps/plugin-store` | Persistent key-value storage |

### Frontend npm Dependencies (Core)

```json
{
  "@tauri-apps/api": "^2",
  "@tanstack/react-table": "^8.17",
  "@tanstack/react-virtual": "^3.0",
  "@tanstack/react-query": "^5.0",
  "react": "^18",
  "react-dom": "^18"
}
```

---

## Architecture Boundaries Analysis

*From Architecture Analyst — package structure, interfaces, coupling, module mapping*

### Package Structure

```
org.tvrenamer
├── model/           # Data + domain objects (no UI, no SWT)
│   ├── FileEpisode  # Core state machine (6 parse states, 8 move states)
│   ├── Series/Show  # Provider result containers
│   ├── UserPreferences (singleton Observable)
│   ├── GlobalOverrides (singleton — NEVER CALLED in production)
│   └── ShowStore    # In-memory lookup cache
├── controller/      # Business logic + provider integration
│   ├── FilenameParser
│   ├── TheTVDBProvider
│   ├── FileMover + MoveRunner
│   ├── ListingsLookup (thread pool)
│   └── *Persistence classes (XStream XML)
└── view/            # SWT only — no business logic
    ├── ResultsTable
    ├── UIStarter
    └── PreferencesDialog
```

### Complete Data Flow (Traced)

1. File dropped → `ResultsTable.dropTarget` → creates `FileEpisode` objects
2. `FilenameParser.parseFilename(FileEpisode)` → extracts show/season/episode → `ShowName.mapShowName()`
3. `ShowStore.mapStringToShow()` → spawns async lookup → `TheTVDBProvider.getShowOptions()`
4. `ShowInformationListener.downloadSucceeded(Show)` callback → triggers episode listing fetch
5. `ListingsLookup.downloadListings(Series)` → `TheTVDBProvider` fetches all episodes
6. `FileEpisode.setSeries()` → matches by season/episode number → updates display
7. User confirms → `MoveRunner` constructor → pre-conflict detection
8. `FileMover.call()` → file move/copy → `MoveObserver` progress callbacks
9. `ResultsTable` updates row status from FileEpisode state

### Critical Architectural Issues for Port

1. **GlobalOverrides never wired**: Must be applied at step 2 (after show name extraction, before provider query). Port must fix this.
2. **UserPreferences singleton embedded everywhere**: `FileMover` accesses `UserPreferences.getInstance()` directly at line 20. Rust port must pass preferences explicitly or use `AppHandle` state.
3. **Three independent thread pools**: `ListingsLookup.THREAD_POOL`, `ShowStore.threadPool`, `MoveRunner.EXECUTOR` — Rust/Tokio consolidates these naturally into one async runtime.
4. **FileEpisode mutable shared across threads**: In Rust, use `Arc<RwLock<FileEpisode>>` or immutable updates via Tauri events.

### Recommended Rust Module Structure

```
src-tauri/src/
├── lib.rs               # Tauri plugin setup, register all commands
├── state.rs             # AppState (reqwest::Client, loaded prefs, etc.)
├── metadata/
│   ├── mod.rs
│   ├── provider.rs      # Trait: MetadataProvider
│   ├── tmdb.rs          # TMDB v3 client implementing MetadataProvider
│   └── models.rs        # Series, Episode structs (serde Deserialize)
├── parser/
│   ├── mod.rs
│   ├── patterns.rs      # The 8 compiled Regex patterns (lazy_static/once_cell)
│   └── filename.rs      # parse_filename() → ParseResult
├── renamer/
│   ├── mod.rs
│   ├── mover.rs         # Atomic file move + copy-fallback + conflict detection
│   ├── conflict.rs      # Pre-move conflict resolution (port MoveRunner logic)
│   └── template.rs      # Rename format string evaluation
├── overrides/
│   └── mod.rs           # Load/apply show name overrides (JSON)
├── config/
│   ├── mod.rs
│   ├── prefs.rs         # Load/save UserPreferences (serde_json)
│   └── migration.rs     # XML → JSON migration (XStream format reader)
└── ipc.rs               # All #[tauri::command] functions
```

---

## Related Issues

| Source | Finding |
|--------|---------|
| Git history (2017) | TheTVDB v1 API deprecated; `DiscontinuedApiException` is evidence the app is currently non-functional for show lookup |
| Codebase analysis | GlobalOverrides feature is orphaned — loaded but never applied |
| Architecture analysis | Three independent thread pools with no coordinated shutdown |

**Note:** No issue tracker was detected in the repository configuration. Consider linking GitHub Issues for tracking the items above.

---

## Edge Cases & Gotchas

*Synthesised across all 12 agents*

### Parser Edge Cases

1. **Shows with numbers in name** (`the.100`, `warehouse.13`): Patterns 7 and 8 are the fallback for these; they must be tested explicitly. Pattern priority order matters.
2. **Shows with year disambiguation** (`castle.2009`, `human.target.2010`): Pattern 5 handles these. The year becomes part of the extracted show name — the overrides system is meant to strip it, but currently doesn't.
3. **Three-digit episodes** (`S22E105` for The Daily Show): Pattern 1 handles `\d\d*` (one or more digits) — this is already accounted for.
4. **Three-digit seasons** (`S103E02` for House Hunters International): Added in 2018; all patterns use `\d\d*` or `\d\d?` — verify Rust regex handles this identically.
5. **Filenames inside deeply nested paths** (`Quintuplets/versions/S01E02.Quintagious~2.avi`): The parser must handle `~2` version suffixes as extraneous noise; confirmed by 8 test cases in the parser suite.

### Tauri v2 Gotchas

6. **`dragDropEnabled` semantics were corrected** — `false` enables HTML5 drag-drop APIs; `true` (default) enables Tauri native `tauri://drag-*` events. Determine which approach to use before implementing file drop.
7. **Capabilities are mandatory** — Tauri v2 silently returns 403 errors (not clear error messages) when a file system operation isn't covered by a capability. Test home dir file access explicitly before building any features on top of it.
8. **`dragDropEnabled` vs `fileDropEnabled`**: The v1 config key `fileDropEnabled` doesn't exist in v2 — it's `dragDropEnabled`. A common migration mistake.
9. **Emit target**: `app.emit()` broadcasts globally; `app.emit_to("main", ...)` targets a specific window. Use the latter for progress events to avoid flooding multiple windows.
10. **Capabilities format**: The `"identifier": "fs:scope"` form was flagged as incorrect by the assumption checker. Verify exact format against current plugin-fs documentation before writing capabilities config.

### Conflict Handling Edge Cases

10. **Race condition window**: MoveRunner pre-checks conflicts then moves asynchronously. If another process creates a file between the pre-check and the actual move, the move-time `Files.exists()` check catches it — but the resolution is `FAIL_TO_MOVE`, not automatic re-indexing. The Rust port should match this behaviour.
11. **Symlink as source**: `Files.isSameFile(destFile, actualDest)` detects when source and destination resolve to the same inode. Status becomes `ALREADY_IN_PLACE`. Port this check.
12. **Cross-filesystem rename**: `std::fs::rename` fails across filesystem boundaries on Windows. Rust port must fall back to copy-and-delete (same as Java `FileMover.doActualMove()`).

### API & Network Edge Cases

13. **TMDB 429 rate limit**: Java has no backoff. Rust port must implement exponential backoff for 429 responses.
14. **TMDB no direct episode ID lookup**: Episodes must be queried by `season_number + episode_number`, not by ID. Episodes can be re-added with new IDs by TMDB, so the current approach is correct.
15. **TMDB special episodes (Season 0)**: Not tested in existing suite. Season 0 contains specials, pilots, and unaired episodes. The episode endpoint works identically — but parsers may not extract season 0 from filenames. Document as known gap.

### Preferences Migration Edge Cases

16. **Both `preferences.xml` and `prefs.json` exist**: Migration should be skipped if `prefs.json` already exists (migration already ran). Check for `prefs.json` existence first.
17. **`preferences.xml` corrupted**: Java returns `null` and silently uses defaults. Rust port should use distinct error and log a warning, then proceed with defaults.
18. **First launch on Windows**: Config path is `%APPDATA%\tvrenamer\`, not `~/.tvrenamer/`. The Tauri capabilities glob `$HOME/.tvrenamer/**` may not match on Windows — verify or use `$APPDATA/.tvrenamer/**`.

---

## Validated Assumptions

*From assumption-checker agent — 11 validated, 5 invalid (corrected below), 5 unverified*

### ✅ Validated (11)

- Tauri v2 `app.emit()` broadcasts globally; `emit_to("main", ...)` targets a specific window label
- Tauri v2 IPC import is `@tauri-apps/api/core` (not `@tauri-apps/api/tauri`)
- TMDB v3 `GET /3/search/tv` returns `id`, `name`, `first_air_date` fields
- TMDB rate limits: 50 req/sec, 20 concurrent connections, no difference free vs. paid
- TMDB registration URL: `https://www.themoviedb.org/settings/api`
- `tempfile::NamedTempFile::persist_noclobber()` returns `Err` if target exists
- `keyring = "3.6"` (latest 3.6.3) provides macOS Keychain / Windows Credential Manager / Linux Secret Service
- TanStack Query v5 `useMutationState` is the correct hook for per-row mutation state
- Tauri updater config lives in `tauri.conf.json` → `"plugins": {"updater": {...}}`
- `tauri-action` auto-generates and uploads `latest.json` for the updater
- `directories` `ProjectDirs::from("", "", "tvrenamer")` API is stable

### ❌ Invalid — Corrected

| Claim | Correction |
|-------|------------|
| `dragDropEnabled: false` enables `tauri://drag-drop` Tauri events | `dragDropEnabled: false` enables **HTML5 drag-drop APIs** in the frontend (disables Tauri's internal drag system). The `tauri://drag-*` events fire when `dragDropEnabled` is `true` (default). **Action**: verify in implementation which approach to use for file drops — Tauri native events or HTML5 File API. |
| Tauri capabilities use `"identifier": "fs:scope"` | Correct format uses predefined identifiers like `"fs:allow-home-read"` and `"fs:allow-home-write"`. Scope path globs are specified separately. Verify exact format against current Tauri v2 capabilities docs before configuring. |
| TMDB `/3/authentication` returns `{"success": true, "status_code": 1}` | Response includes a third field: `{"success": true, "status_code": 1, "status_message": "Success."}` (minor — parse all three fields) |
| `reqwest = "0.12"` | Current stable is **`reqwest = "0.13"`** (0.13.2 as of early 2026) |
| `directories = "5.0"` | Current stable is **`directories = "6.0"`** (6.0.1, released January 2025) |

### ⚠️ Unverified (5)

- TMDB v3 episode endpoint exact field names — likely correct but verify against actual API response
- `serde_flow = "0.3"` `#[flow(variant = N)]` exact annotation syntax — test before adopting
- `@tanstack/react-table = "^8.17"` — current latest is 8.21+; `^8.17` will resolve correctly but document the true current version
- Java FilenameParser patterns compatibility with Rust `regex` crate — patterns appear to not use lookahead/lookbehind (the `regex` crate does NOT support them); verify each of the 8 patterns before porting. Use `fancy_regex` crate if any pattern requires lookaround.
- `once_cell = "1"` — superseded by `std::sync::OnceLock` (stable since Rust 1.70). Use `OnceLock` in new code; remove `once_cell` from `Cargo.toml`.

---

## Open Questions

1. **`keyring` vs plaintext on headless Linux systems**: The `keyring` crate requires a Secret Service daemon (e.g., `gnome-keyring`). On headless Linux (CI, servers), this isn't available. Fallback strategy needed: encrypted config file, or skip keychain and use `prefs.json` with a warning.

2. **`directories` crate vs `~/.tvrenamer/`**: The design doc specifies `~/.tvrenamer/` directly. The `directories` crate gives platform-correct paths (`~/Library/Application Support/tvrenamer` on macOS). Decision: adopt platform-correct paths, or stay with the `~/.tvrenamer/` convention users are familiar with?

3. **TMDB for anime / specials (Season 0)**: TMDB Season 0 holds specials. The filename parser doesn't extract Season 0 from typical filenames. This is a known gap — document in the implementation plan.

4. **XStream XML migration**: Java's XStream XML format has unusual structure for nested objects. A simple XML parser in Rust may not handle all edge cases. Consider shipping the migration as a one-time Java utility rather than re-implementing XStream parsing in Rust.

5. **TMDB API key validation during onboarding vs. startup**: Should the app validate the stored key on every startup (network call on launch) or only when the user explicitly re-tests it? Current design says validate at entry only.
