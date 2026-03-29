# Research: Renamer & Conflict Resolution Module (Rust Port)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Port the file rename/move engine and conflict resolution algorithm from Java to Rust. Handles rename format template evaluation, atomic file moves with copy-fallback, and automatic conflict detection with index-based versioning.

---

## Rename Format Template

The default rename template is `"%S [%sx%0e] %t"` (from UserPreferences). Tokens:

- `%S` — show name
- `%s` — season number
- `%0e` — zero-padded episode number
- `%t` — episode title

The template is user-configurable. The current UI has a drag-and-drop token builder for constructing templates.

---

## Conflict Resolution Algorithm (Complete Spec)

**Automatic index-based versioning.** No user prompt.

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

The "largest file = best quality" assumption is hard-coded and not configurable.

---

## ConflictTest — 4 Scenarios (Complete Spec)

| Test | Scenario | Expected Outcome |
|------|----------|-----------------|
| `testFileMoverConflict` | Blocking file pre-exists at destination | Source moved to `versions/` as `basename (2).ext` |
| `testMoveRunnerWithConflict` | Same via async MoveRunner | Same outcome + MoveObserver notified |
| `testMoveRunnerWithTwoConflicts` | Blocking file + second obstacle created | Source moved to `versions/` as `basename (3).ext` |
| `testMoveRunnerWithThreeConflicts` | Three cascading obstacles | Source moved to `versions/` as `basename (4).ext` |

---

## MoveTest — 5 Scenarios

| Test | Scenario |
|------|----------|
| `testFileMover` | Basic file move succeeds |
| `testFileMoverCannotMove` | Read-only file → FAIL_TO_MOVE |
| `testMoveRunner` | Async move via MoveRunner succeeds |
| `testMoveRunnerCannotMove` | Read-only + async runner |
| `testMoveRunnerCannotMoveWithTimestamp` | Timestamp preservation |

---

## Atomic File Renames

```toml
tempfile = "3.10"
```

- Use `NamedTempFile::persist_noclobber()` for conflict detection (returns `Err` if target exists — no race window)
- `std::fs::rename` fails across filesystem boundaries on Windows
- `renamore` crate provides `rename_exclusive()` for platform-agnostic atomic exclusive renames

---

## Conflict Handling Edge Cases

1. **Race condition window**: MoveRunner pre-checks conflicts then moves asynchronously. If another process creates a file between pre-check and move, the move-time `Files.exists()` check catches it — resolution is `FAIL_TO_MOVE`, not re-indexing. Rust port must match this.
2. **Symlink as source**: `Files.isSameFile(destFile, actualDest)` detects when source and destination resolve to the same inode. Status becomes `ALREADY_IN_PLACE`. Port this check.
3. **Cross-filesystem rename**: `std::fs::rename` fails across filesystem boundaries on Windows. Must fall back to copy-and-delete (same as Java `FileMover.doActualMove()`).
4. **Partial copy cleanup**: Java copies to temp file, deletes on failure. Port this pattern.

---

## Error Handling

| Error | Rust Variant | When |
|-------|-------------|------|
| Permission denied | `AppError::PermissionDenied` | Read-only destination |
| Destination exists | `AppError::DestinationExists` | Race condition at move time |
| File not found | `AppError::FileNotFound` | Source disappeared between scan and move |

**Improvement for Rust port:** Add a pre-move disk space check (Java has none).

---

## Rust Module Design

```rust
// src-tauri/src/renamer/mover.rs
pub enum MoveStatus {
    Success,
    AlreadyInPlace,
    FailToMove(String),
}

pub fn move_file(source: &Path, dest: &Path) -> Result<MoveStatus, AppError> { ... }

// src-tauri/src/renamer/conflict.rs
pub fn resolve_conflicts(pending_moves: &mut [PendingMove]) { ... }

// src-tauri/src/renamer/template.rs
pub fn apply_template(template: &str, show: &str, season: u32, episode: u32, title: &str) -> String { ... }
```

---

## Data Flow (Steps 7-8 of Full Flow)

1. User confirms rename → `MoveRunner` constructor → pre-conflict detection
2. `FileMover.call()` → file move/copy → `MoveObserver` progress callbacks
3. Frontend updates per-row status from event

Progress events from Rust to frontend:
```rust
app.emit_to("main", "rename-progress", progress)?;
```

---

## Git History

- Conflict resolution introduced 2017-04-24 (commit 03c5216): strategy is largest file at primary destination, others in `versions/` with index suffix.
- Comprehensive tests added 2018-11-25 (commit 67ce3b0): double, triple, cascading conflicts.
- `FileMover.java` has ~41 changes — moderately complex file.

---

## Test Coverage Gaps (Write From Scratch)

- **Disk full / permission denied** — no tests
- **Very long path names** — no tests
- **Unicode filenames** — no tests
- **Cross-filesystem moves** — no tests

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| `tempfile::NamedTempFile::persist_noclobber()` returns `Err` if target exists | ✅ Valid |
