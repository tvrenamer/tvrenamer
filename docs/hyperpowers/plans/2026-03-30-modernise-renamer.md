# Renamer Module Implementation Plan

> **For Claude:** Run `/execute-plan` to implement this plan (will ask which execution style you prefer). Steps use checkbox (`- [ ]`) syntax for tracking.
> **Related Issues:** None identified in research document.

**Goal:** Port the file rename/move engine and conflict resolution algorithm from Java to Rust, implementing `template.rs`, `conflict.rs`, and `mover.rs` as fully functional modules with an IPC command to drive renames from the frontend.

**Architecture:** Three pure modules — `template.rs` (format string evaluation), `conflict.rs` (pre-move conflict detection, modifies dest paths before any files move), and `mover.rs` (atomic rename with copy-fallback). A single IPC command (`perform_renames`) wires them together and emits per-file progress events.

**Tech Stack:** Rust, `tempfile` (already in Cargo.toml), `renamore` (add for atomic exclusive renames), Tauri v2 IPC, `#[test]` (sync) + `tempfile::tempdir()` for filesystem tests.

**Context Gathered From:**
- `docs/hyperpowers/research/2026-03-29-modernise-renamer.md`

---

## Validated Assumptions

✅ `tempfile::NamedTempFile::persist_noclobber()` returns `Err` if target exists (confirmed in research)
✅ `AppError` already has `FileNotFound`, `PermissionDenied`, `DestinationExists` variants (`src-tauri/src/errors.rs`)
✅ `tempfile = "3.10"` already in `Cargo.toml`
✅ `renamore = "0.2"` exists on crates.io; `rename_exclusive()` is its primary API returning `std::io::Result<()>`
✅ `tauri::Emitter` trait (import with `use tauri::Emitter;`) is the correct Tauri v2 API for `app.emit()`
✅ `libc` is available as a transitive dependency (via tokio/reqwest) — `libc::EXDEV` usable without explicit dep
⚠️ `renamore` is NOT in `Cargo.toml` — must be added (Task 1)
⚠️ `AppError::DiskFull` does NOT exist yet — must be added (Task 1)
❌ **Fixed:** `io::ErrorKind::CrossesDevices` was NOT stabilized until Rust 1.85 (project requires 1.77). Cross-device detection in Task 4 uses `e.raw_os_error() == Some(libc::EXDEV)` instead.

---

## Task 1: Add `renamore` dependency and `AppError::DiskFull`

**Files:**
- Modify: `src-tauri/Cargo.toml`
- Modify: `src-tauri/src/errors.rs`

- [ ] **Step 1: Write a failing serialization test for `AppError::DiskFull`**

Add to the `tests` mod at the bottom of `src-tauri/src/errors.rs`, inside `fn all_error_variants_serialize()`:

```rust
AppError::DiskFull("no space".into()),
```

- [ ] **Step 2: Run tests to confirm the new variant causes a compile error**

```bash
cargo test -p tvrenamer --lib errors 2>&1 | head -20
```

Expected: compile error — variant `DiskFull` not found.

- [ ] **Step 3: Add `AppError::DiskFull` to errors.rs**

In `src-tauri/src/errors.rs`, add to the `AppError` enum:

```rust
#[error("Disk full: {0}")]
DiskFull(String),
```

- [ ] **Step 4: Add `renamore` to Cargo.toml**

In `src-tauri/Cargo.toml`, under `[dependencies]`, add:

```toml
renamore = "0.2"
```

- [ ] **Step 5: Run tests to confirm everything compiles and serializes**

```bash
cargo test -p tvrenamer --lib errors
```

Expected: all error serialization tests pass.

- [ ] **Step 6: Commit**

```bash
git add src-tauri/Cargo.toml src-tauri/src/errors.rs
git commit -m "feat(renamer): add renamore dep and AppError::DiskFull variant"
```

---

## Task 2: Implement `template.rs`

**Files:**
- Modify: `src-tauri/src/renamer/template.rs`

The default template `"%S [%sx%0e] %t"` uses four tokens. Token replacement is left-to-right, simple string substitution. Unknown tokens pass through unchanged.

- [ ] **Step 1: Write failing tests in `template.rs`**

Replace the current stub content of `src-tauri/src/renamer/template.rs` with:

```rust
// Rename format string evaluation
// Default template: "%S [%sx%0e] %t"  (show name, season, zero-padded episode, title)

/// Apply a rename template to episode metadata.
///
/// Tokens:
///   %S  — show name
///   %s  — season number (unpadded integer)
///   %0e — episode number, zero-padded to 2 digits
///   %t  — episode title
///
/// Unknown tokens are passed through unchanged.
pub fn apply_template(template: &str, show: &str, season: u32, episode: u32, title: &str) -> String {
    todo!()
}

#[cfg(test)]
mod tests {
    use super::apply_template;

    #[test]
    fn default_template_single_digit_episode() {
        assert_eq!(
            apply_template("%S [%sx%0e] %t", "Breaking Bad", 1, 7, "A No-Rough-Stuff-Type Deal"),
            "Breaking Bad [1x07] A No-Rough-Stuff-Type Deal"
        );
    }

    #[test]
    fn default_template_double_digit_episode() {
        assert_eq!(
            apply_template("%S [%sx%0e] %t", "Breaking Bad", 2, 13, "Face Off"),
            "Breaking Bad [2x13] Face Off"
        );
    }

    #[test]
    fn episode_zero_padded_single_digit() {
        assert_eq!(apply_template("%0e", "Ignored", 0, 5, "Ignored"), "05");
    }

    #[test]
    fn episode_not_padded_when_two_digits() {
        assert_eq!(apply_template("%0e", "Ignored", 0, 15, "Ignored"), "15");
    }

    #[test]
    fn season_unpadded() {
        assert_eq!(apply_template("%s", "Ignored", 3, 0, "Ignored"), "3");
    }

    #[test]
    fn unknown_token_passes_through() {
        assert_eq!(apply_template("%X%Y", "Show", 1, 1, "Title"), "%X%Y");
    }

    #[test]
    fn empty_template_returns_empty() {
        assert_eq!(apply_template("", "Show", 1, 1, "Title"), "");
    }

    #[test]
    fn literal_text_preserved() {
        assert_eq!(apply_template("Episode %0e", "Ignored", 0, 3, "Ignored"), "Episode 03");
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (todo! panics)**

```bash
cargo test -p tvrenamer --lib renamer::template
```

Expected: tests fail with `panicked at 'not yet implemented'`.

- [ ] **Step 3: Implement `apply_template`**

Replace `todo!()` with:

```rust
template
    .replace("%S", show)
    .replace("%s", &season.to_string())
    .replace("%0e", &format!("{:02}", episode))
    .replace("%t", title)
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cargo test -p tvrenamer --lib renamer::template
```

Expected: 8 tests pass, 0 failed.

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/renamer/template.rs
git commit -m "feat(renamer): implement template format string evaluation"
```

---

## Task 3: Implement `conflict.rs`

**Files:**
- Modify: `src-tauri/src/renamer/conflict.rs`

The conflict algorithm runs as a pre-move phase — it mutates `dest` paths in the `PendingMove` list BEFORE any file is moved. This guarantees no intermediate state is visible to the filesystem during conflict resolution.

**Algorithm:**
1. Group pending moves by `(dest_parent_dir, dest_stem, dest_extension)`.
2. Count `existing_count` = 1 if a file already exists at the primary dest, else 0.
3. If `existing_count + pending_count > 1` (conflict): route ALL pending moves to `dest_dir/versions/stem (N).ext`. Sort pending moves by `source_size` DESC before assigning; first index = `existing_count + 1`.

**Acceptance criteria (from `ConflictTest` in research):**

| Java Test | Rust equivalent | existing at dest | pending moves | Expected dest for source |
|------|------|------|------|------|
| `testFileMoverConflict` | `blocking_file_preexists_routes_to_versions_2` | 1 | 1 | `versions/basename (2).ext` |
| `testMoveRunnerWithConflict` | Covered by `perform_renames` IPC + above test | 1 | 1 | same outcome, async path |
| `testMoveRunnerWithTwoConflicts` | `one_blocking_two_pending_large_gets_2_small_gets_3` | 1 | 2 | smallest → `versions/basename (3).ext` |
| `testMoveRunnerWithThreeConflicts` | `one_blocking_three_pending_smallest_gets_4` | 1 | 3 | smallest → `versions/basename (4).ext` |

**Note on `testMoveRunnerWithConflict`:** Java had a separate `MoveRunner` class for async moves. In the Rust port there is no `MoveRunner` — that concern is absorbed into `perform_renames` (Task 5), which runs `move_file` in `spawn_blocking`. The conflict logic is identical; the "async + observer" path is the IPC layer itself. No separate Rust test is needed for this scenario beyond what `blocking_file_preexists_routes_to_versions_2` already covers.

- [ ] **Step 1: Write failing tests in `conflict.rs`**

Replace stub content of `src-tauri/src/renamer/conflict.rs` with:

```rust
// Pre-move conflict detection — runs BEFORE any moves execute.
// Ports MoveRunner.java pre-scan: group by dest dir, sort by source size desc, assign versions/ indices.
// 4 ConflictTest scenarios are the acceptance criteria (see research doc).

use std::collections::HashMap;
use std::path::{Path, PathBuf};

/// A pending file rename operation.
#[derive(Debug, Clone)]
pub struct PendingMove {
    pub source: PathBuf,
    pub dest: PathBuf,
    /// Populated from `source` file metadata before calling `resolve_conflicts`.
    pub source_size: u64,
}

/// Resolve conflicts in a list of pending moves **before** any move executes.
///
/// Mutates `dest` on conflicting entries to route them to
/// `dest_dir/versions/stem (N).ext`. Largest-by-size file gets the lowest
/// index (closest to the primary destination quality-wise).
pub fn resolve_conflicts(pending_moves: &mut Vec<PendingMove>) {
    todo!()
}

fn dest_key(dest: &Path) -> (PathBuf, String, String) {
    let dir = dest.parent().unwrap_or(Path::new("")).to_path_buf();
    let stem = dest
        .file_stem()
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let ext = dest
        .extension()
        .map(|s| format!(".{}", s.to_string_lossy()))
        .unwrap_or_default();
    (dir, stem, ext)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    fn pm(source: &str, dest: &str, size: u64) -> PendingMove {
        PendingMove {
            source: PathBuf::from(source),
            dest: PathBuf::from(dest),
            source_size: size,
        }
    }

    // No conflict — single move, dest does not exist: unchanged.
    #[test]
    fn no_conflict_single_move_unchanged() {
        let mut moves = vec![pm("/src/show.s01e01.mkv", "/dest/Show S01E01.mkv", 1000)];
        resolve_conflicts(&mut moves);
        assert_eq!(moves[0].dest, PathBuf::from("/dest/Show S01E01.mkv"));
    }

    // No conflict — two moves targeting DIFFERENT destinations: both unchanged.
    #[test]
    fn no_conflict_different_destinations_unchanged() {
        let mut moves = vec![
            pm("/src/ep1.mkv", "/dest/Show S01E01.mkv", 1000),
            pm("/src/ep2.mkv", "/dest/Show S01E02.mkv", 1000),
        ];
        resolve_conflicts(&mut moves);
        assert_eq!(moves[0].dest, PathBuf::from("/dest/Show S01E01.mkv"));
        assert_eq!(moves[1].dest, PathBuf::from("/dest/Show S01E02.mkv"));
    }

    // testFileMoverConflict: 1 blocking file pre-exists → versions/(2)
    #[test]
    fn blocking_file_preexists_routes_to_versions_2() {
        let dir = tempfile::tempdir().unwrap();
        let primary = dir.path().join("Show S01E01.mkv");
        std::fs::write(&primary, b"existing").unwrap();

        let mut moves = vec![pm("/src/show.mkv", primary.to_str().unwrap(), 500)];
        resolve_conflicts(&mut moves);

        let expected = dir.path().join("versions").join("Show S01E01 (2).mkv");
        assert_eq!(moves[0].dest, expected);
    }

    // testMoveRunnerWithTwoConflicts: 1 blocking + 2 pending → large=(2), small=(3)
    #[test]
    fn one_blocking_two_pending_large_gets_2_small_gets_3() {
        let dir = tempfile::tempdir().unwrap();
        let primary = dir.path().join("Show S01E01.mkv");
        std::fs::write(&primary, b"existing").unwrap();

        let dest_str = primary.to_str().unwrap();
        let mut moves = vec![
            pm("/src/large.mkv", dest_str, 2000),
            pm("/src/small.mkv", dest_str, 500),
        ];
        resolve_conflicts(&mut moves);

        let versions = dir.path().join("versions");
        let large = moves.iter().find(|m| m.source_size == 2000).unwrap();
        let small = moves.iter().find(|m| m.source_size == 500).unwrap();
        assert_eq!(large.dest, versions.join("Show S01E01 (2).mkv"));
        assert_eq!(small.dest, versions.join("Show S01E01 (3).mkv"));
    }

    // testMoveRunnerWithThreeConflicts: 1 blocking + 3 pending → smallest=(4)
    #[test]
    fn one_blocking_three_pending_smallest_gets_4() {
        let dir = tempfile::tempdir().unwrap();
        let primary = dir.path().join("Show S01E01.mkv");
        std::fs::write(&primary, b"existing").unwrap();

        let dest_str = primary.to_str().unwrap();
        let mut moves = vec![
            pm("/src/large.mkv", dest_str, 3000),
            pm("/src/medium.mkv", dest_str, 2000),
            pm("/src/small.mkv", dest_str, 500),
        ];
        resolve_conflicts(&mut moves);

        let versions = dir.path().join("versions");
        let small = moves.iter().find(|m| m.source_size == 500).unwrap();
        assert_eq!(small.dest, versions.join("Show S01E01 (4).mkv"));
    }

    // Two pending moves, no existing file → both go to versions/(1) and versions/(2)
    #[test]
    fn two_pending_no_existing_both_versioned() {
        let mut moves = vec![
            pm("/src/large.mkv", "/dest/Show S01E01.mkv", 2000),
            pm("/src/small.mkv", "/dest/Show S01E01.mkv", 500),
        ];
        resolve_conflicts(&mut moves);

        let large = moves.iter().find(|m| m.source_size == 2000).unwrap();
        let small = moves.iter().find(|m| m.source_size == 500).unwrap();
        assert_eq!(
            large.dest,
            PathBuf::from("/dest/versions/Show S01E01 (1).mkv")
        );
        assert_eq!(
            small.dest,
            PathBuf::from("/dest/versions/Show S01E01 (2).mkv")
        );
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cargo test -p tvrenamer --lib renamer::conflict
```

Expected: compile error or panic on `todo!()`.

- [ ] **Step 3: Implement `resolve_conflicts`**

Replace `todo!()` in `resolve_conflicts` with:

```rust
// Step 1: Group pending move indices by (dest_dir, stem, ext)
let mut groups: HashMap<(PathBuf, String, String), Vec<usize>> = HashMap::new();
for (idx, pm) in pending_moves.iter().enumerate() {
    groups.entry(dest_key(&pm.dest)).or_default().push(idx);
}

for ((dest_dir, stem, ext), indices) in groups {
    // Step 2: Count files already at the primary destination
    let primary = dest_dir.join(format!("{}{}", stem, ext));
    let existing_count = if primary.exists() { 1usize } else { 0 };

    // Step 3: No conflict if at most one total entity at this destination
    if existing_count + indices.len() <= 1 {
        continue;
    }

    // Step 4: Sort pending movers by source_size descending (largest = best quality = first)
    let mut sorted = indices;
    sorted.sort_by(|&a, &b| pending_moves[b].source_size.cmp(&pending_moves[a].source_size));

    // Step 5: Route all pending movers to versions/ with sequential indices
    let versions_dir = dest_dir.join("versions");
    let mut next_index = existing_count + 1;
    for idx in sorted {
        pending_moves[idx].dest =
            versions_dir.join(format!("{} ({}){}", stem, next_index, ext));
        next_index += 1;
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cargo test -p tvrenamer --lib renamer::conflict
```

Expected: 6 tests pass, 0 failed.

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/renamer/conflict.rs
git commit -m "feat(renamer): implement conflict pre-scan with versions/ routing"
```

---

## Task 4: Implement `mover.rs`

**Files:**
- Modify: `src-tauri/src/renamer/mover.rs`

**Move algorithm:**
1. Check source exists → `AppError::FileNotFound`
2. Same-file check (canonical paths) → `MoveStatus::AlreadyInPlace`
3. Create dest parent directory if needed
4. Try `renamore::rename_exclusive(source, dest)`:
   - `Ok(())` → `MoveStatus::Success`
   - `AlreadyExists` → `AppError::DestinationExists` (race condition)
   - `EXDEV` (cross-device) → fall back to copy+persist
   - Other IO error → `AppError::PermissionDenied`
5. Copy fallback (cross-filesystem):
   - `NamedTempFile::new_in(dest_parent)` → write source contents
   - `temp.persist_noclobber(dest)` → `AppError::DestinationExists` if target appeared
   - ENOSPC during copy → `AppError::DiskFull`
   - Delete source

**Note on `renamore::rename_exclusive` on macOS:** On macOS, `rename_exclusive` maps to `renamex_np` with `RENAME_EXCL`. Cross-device errors surface as `io::ErrorKind::CrossesDevices` (Rust 1.75+). Fallback needed for cross-filesystem moves.

- [ ] **Step 1: Write failing tests in `mover.rs`**

Replace stub content of `src-tauri/src/renamer/mover.rs` with:

```rust
// Atomic file move with copy-delete fallback for cross-filesystem moves.
// Ports FileMover.java.
// move_file is synchronous — wrap in tokio::task::spawn_blocking for async callers.

use std::io;
use std::path::Path;

use tempfile::NamedTempFile;

use crate::errors::AppError;

/// Outcome of a single file move attempt.
#[derive(Debug, Clone, PartialEq)]
pub enum MoveStatus {
    /// File moved successfully to destination.
    Success,
    /// Source and destination resolve to the same file (symlink case). No-op.
    AlreadyInPlace,
    /// Move failed. The source file was not deleted.
    FailToMove(String),
}

/// Move `source` to `dest` atomically.
///
/// - Returns `AlreadyInPlace` if source and dest resolve to the same inode.
/// - Returns `Err(DestinationExists)` if dest already exists at move time (race condition).
/// - Falls back to copy+delete for cross-filesystem moves.
/// - Creates dest parent directory if it does not exist.
pub fn move_file(source: &Path, dest: &Path) -> Result<MoveStatus, AppError> {
    todo!()
}

fn same_file(source: &Path, dest: &Path) -> bool {
    if !dest.exists() {
        return false;
    }
    match (source.canonicalize(), dest.canonicalize()) {
        (Ok(s), Ok(d)) => s == d,
        _ => false,
    }
}

fn copy_and_delete(source: &Path, dest: &Path) -> Result<MoveStatus, AppError> {
    let dest_parent = dest.parent().unwrap_or(Path::new("."));
    let tmp = NamedTempFile::new_in(dest_parent).map_err(|e| {
        AppError::PermissionDenied(format!("Cannot create temp file in {:?}: {}", dest_parent, e))
    })?;

    let mut src_file = std::fs::File::open(source)
        .map_err(|_| AppError::FileNotFound(source.display().to_string()))?;
    let mut tmp_file = tmp.as_file();
    std::io::copy(&mut src_file, &mut tmp_file).map_err(|e| {
        if e.raw_os_error() == Some(libc::ENOSPC) {
            AppError::DiskFull(dest.display().to_string())
        } else {
            AppError::PermissionDenied(e.to_string())
        }
    })?;

    tmp.persist_noclobber(dest).map_err(|e| {
        if e.error.kind() == io::ErrorKind::AlreadyExists {
            AppError::DestinationExists
        } else {
            AppError::PermissionDenied(e.error.to_string())
        }
    })?;

    std::fs::remove_file(source)
        .map_err(|e| AppError::PermissionDenied(format!("Cannot delete source: {}", e)))?;

    Ok(MoveStatus::Success)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use tempfile::tempdir;

    // testFileMover: basic move succeeds
    #[test]
    fn basic_move_succeeds() {
        let dir = tempdir().unwrap();
        let src = dir.path().join("source.mkv");
        let dst = dir.path().join("dest.mkv");
        fs::write(&src, b"content").unwrap();

        let result = move_file(&src, &dst).unwrap();

        assert_eq!(result, MoveStatus::Success);
        assert!(!src.exists(), "source should be gone");
        assert!(dst.exists(), "dest should exist");
        assert_eq!(fs::read(&dst).unwrap(), b"content");
    }

    // Dest parent created automatically
    #[test]
    fn creates_dest_parent_directory() {
        let dir = tempdir().unwrap();
        let src = dir.path().join("source.mkv");
        let dst = dir.path().join("subdir").join("nested").join("dest.mkv");
        fs::write(&src, b"hi").unwrap();

        let result = move_file(&src, &dst).unwrap();

        assert_eq!(result, MoveStatus::Success);
        assert!(dst.exists());
    }

    // Source does not exist → FileNotFound
    #[test]
    fn source_missing_returns_file_not_found() {
        let dir = tempdir().unwrap();
        let src = dir.path().join("nonexistent.mkv");
        let dst = dir.path().join("dest.mkv");

        let result = move_file(&src, &dst);
        assert!(matches!(result, Err(AppError::FileNotFound(_))));
    }

    // Source == dest (same canonical path) → AlreadyInPlace
    #[test]
    fn same_source_and_dest_returns_already_in_place() {
        let dir = tempdir().unwrap();
        let src = dir.path().join("file.mkv");
        fs::write(&src, b"data").unwrap();

        let result = move_file(&src, &src).unwrap();
        assert_eq!(result, MoveStatus::AlreadyInPlace);
    }

    // testFileMoverCannotMove: read-only destination directory → FailToMove
    #[test]
    #[cfg(unix)]
    fn read_only_dest_dir_returns_fail_to_move() {
        use std::os::unix::fs::PermissionsExt;

        let dir = tempdir().unwrap();
        let src = dir.path().join("source.mkv");
        let dest_dir = dir.path().join("readonly");
        fs::create_dir(&dest_dir).unwrap();
        fs::write(&src, b"data").unwrap();

        // Make dest dir read-only
        fs::set_permissions(&dest_dir, fs::Permissions::from_mode(0o555)).unwrap();

        let dst = dest_dir.join("dest.mkv");
        let result = move_file(&src, &dst);

        // Restore permissions before assert so tempdir cleanup works
        fs::set_permissions(&dest_dir, fs::Permissions::from_mode(0o755)).unwrap();

        assert!(
            matches!(result, Err(AppError::PermissionDenied(_))),
            "expected PermissionDenied, got {:?}",
            result
        );
    }

    // Destination exists at move time → DestinationExists (race condition)
    #[test]
    fn dest_exists_at_move_time_returns_destination_exists() {
        let dir = tempdir().unwrap();
        let src = dir.path().join("source.mkv");
        let dst = dir.path().join("dest.mkv");
        fs::write(&src, b"source content").unwrap();
        fs::write(&dst, b"pre-existing").unwrap();

        let result = move_file(&src, &dst);
        assert!(matches!(result, Err(AppError::DestinationExists)));
    }
}
```

**Note on `libc::ENOSPC`:** The `libc` crate is a transitive dependency (via `tokio`/`reqwest`), no need to add it explicitly. The import at the top of `copy_and_delete` will resolve.

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cargo test -p tvrenamer --lib renamer::mover
```

Expected: compile error or `todo!()` panics.

- [ ] **Step 3: Implement `move_file`**

Replace `todo!()` in `move_file` with:

```rust
// 1. Source must exist
if !source.exists() {
    return Err(AppError::FileNotFound(source.display().to_string()));
}

// 2. Same-inode check — source and dest are the same file
if same_file(source, dest) {
    return Ok(MoveStatus::AlreadyInPlace);
}

// 3. Create dest parent directory
if let Some(parent) = dest.parent() {
    std::fs::create_dir_all(parent).map_err(|e| {
        AppError::PermissionDenied(format!("Cannot create {:?}: {}", parent, e))
    })?;
}

// 4. Attempt atomic exclusive rename (fails if dest exists — no race window)
match renamore::rename_exclusive(source, dest) {
    Ok(()) => return Ok(MoveStatus::Success),
    Err(e) if e.kind() == io::ErrorKind::AlreadyExists => {
        return Err(AppError::DestinationExists);
    }
    Err(e) if e.raw_os_error() == Some(libc::EXDEV) => {
        // Cross-filesystem move — fall through to copy+delete below.
        // Note: io::ErrorKind::CrossesDevices not used because it was stabilised
        // in Rust 1.85, but this project targets 1.77.
    }
    Err(e) => {
        return Err(AppError::PermissionDenied(e.to_string()));
    }
}

// 5. Cross-filesystem fallback: copy + atomic persist + delete source
copy_and_delete(source, dest)
```

Also add at the top of the file alongside the other `use` statements:

```rust
use renamore;  // `rename_exclusive` is a free function: renamore::rename_exclusive(src, dst)
```

`libc` needs no `use` import — reference it directly as `libc::EXDEV`.

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cargo test -p tvrenamer --lib renamer::mover
```

Expected: 5 tests pass (the `read_only_dest_dir_returns_fail_to_move` test is `#[cfg(unix)]` and may behave differently if run as root — skip that case in CI if needed).

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/renamer/mover.rs
git commit -m "feat(renamer): implement atomic file mover with cross-fs copy-delete fallback"
```

---

## Task 5: Implement IPC command `perform_renames`

**Files:**
- Modify: `src-tauri/src/ipc.rs`
- Modify: `src-tauri/src/lib.rs`

The frontend sends a list of `(source, dest)` pairs. The backend:
1. Reads `source_size` from each source file's metadata
2. Builds `Vec<PendingMove>` and runs `resolve_conflicts`
3. For each (possibly conflict-resolved) move, calls `move_file` in a blocking task
4. Emits a `rename-progress` event after each file
5. Returns all outcomes

**Note on `move_file` in async context:** `move_file` is synchronous (blocking filesystem calls). Wrap it in `tokio::task::spawn_blocking` to avoid blocking the async Tauri executor.

- [ ] **Step 1: Write failing test for the IPC types**

Add to the bottom of `src-tauri/src/ipc.rs`:

```rust
#[cfg(test)]
mod tests {
    use super::{RenameOutcomeStatus, RenameRequest};

    #[test]
    fn rename_request_deserializes() {
        let json = r#"{"source":"/a/b.mkv","dest":"/c/d.mkv"}"#;
        let req: RenameRequest = serde_json::from_str(json).unwrap();
        assert_eq!(req.source, "/a/b.mkv");
        assert_eq!(req.dest, "/c/d.mkv");
    }

    #[test]
    fn rename_outcome_status_serializes() {
        assert_eq!(
            serde_json::to_string(&RenameOutcomeStatus::Success).unwrap(),
            r#""success""#
        );
        assert_eq!(
            serde_json::to_string(&RenameOutcomeStatus::AlreadyInPlace).unwrap(),
            r#""already_in_place""#
        );
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (types don't exist yet)**

```bash
cargo test -p tvrenamer --lib ipc
```

Expected: compile error — `RenameRequest`, `RenameOutcomeStatus` not found.

- [ ] **Step 3: Add types and `perform_renames` command to `ipc.rs`**

Add the following to `src-tauri/src/ipc.rs`, after the existing imports:

```rust
use std::path::PathBuf;
use tauri::Emitter;

use crate::renamer::conflict::{PendingMove, resolve_conflicts};
use crate::renamer::mover::{move_file, MoveStatus};
```

Then add the types and command:

```rust
/// Input from frontend: a single (source, dest) rename pair.
#[derive(serde::Deserialize)]
pub struct RenameRequest {
    pub source: String,
    pub dest: String,
}

/// Per-file rename outcome status for frontend consumption.
#[derive(serde::Serialize, PartialEq, Debug)]
#[serde(rename_all = "snake_case")]
pub enum RenameOutcomeStatus {
    Success,
    AlreadyInPlace,
    FailToMove,
}

/// Full outcome of a single rename, including the resolved destination path.
#[derive(serde::Serialize)]
pub struct RenameOutcome {
    pub source: String,
    /// Actual destination used (may differ from requested dest if conflict-resolved).
    pub dest: String,
    pub status: RenameOutcomeStatus,
    /// Populated when status is FailToMove.
    pub error: Option<String>,
}

/// Execute a batch of file renames.
///
/// 1. Reads source sizes to build PendingMove list.
/// 2. Runs conflict pre-scan (mutates dest paths for conflicts).
/// 3. Moves each file; emits `rename-progress` event after each.
/// 4. Returns all outcomes.
#[tauri::command]
pub async fn perform_renames(
    renames: Vec<RenameRequest>,
    app: tauri::AppHandle,
) -> Result<Vec<RenameOutcome>, String> {
    // Build PendingMove list with source sizes
    let mut pending: Vec<PendingMove> = renames
        .into_iter()
        .map(|r| {
            let source = PathBuf::from(&r.source);
            let source_size = std::fs::metadata(&source).map(|m| m.len()).unwrap_or(0);
            PendingMove {
                dest: PathBuf::from(r.dest),
                source,
                source_size,
            }
        })
        .collect();

    // Conflict pre-scan (mutates dest paths in-place)
    resolve_conflicts(&mut pending);

    // Execute moves
    let mut outcomes = Vec::with_capacity(pending.len());
    for pm in pending {
        let source = pm.source.clone();
        let dest = pm.dest.clone();

        let result = tokio::task::spawn_blocking({
            let src = source.clone();
            let dst = dest.clone();
            move || move_file(&src, &dst)
        })
        .await
        .map_err(|e| e.to_string())?;

        let outcome = match result {
            Ok(MoveStatus::Success) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::Success,
                error: None,
            },
            Ok(MoveStatus::AlreadyInPlace) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::AlreadyInPlace,
                error: None,
            },
            Ok(MoveStatus::FailToMove(msg)) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::FailToMove,
                error: Some(msg),
            },
            Err(e) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::FailToMove,
                error: Some(e.to_string()),
            },
        };

        // Emit progress event (frontend listens on "rename-progress")
        let _ = app.emit("rename-progress", &outcome);

        outcomes.push(outcome);
    }

    Ok(outcomes)
}
```

- [ ] **Step 4: Register `perform_renames` in `lib.rs`**

In `src-tauri/src/lib.rs`, add `ipc::perform_renames` to the `invoke_handler!` macro:

```rust
.invoke_handler(tauri::generate_handler![
    ipc::ping,
    ipc::search_shows,
    ipc::lookup_episode,
    ipc::validate_tmdb_key,
    ipc::save_tmdb_key,
    ipc::perform_renames,      // ← add this line
])
```

- [ ] **Step 5: Run tests to confirm IPC types compile and serialize correctly**

```bash
cargo test -p tvrenamer --lib ipc
```

Expected: 2 tests pass.

- [ ] **Step 6: Run all renamer tests together**

```bash
cargo test -p tvrenamer --lib renamer
```

Expected: all renamer tests pass (template + conflict + mover).

- [ ] **Step 7: Build to confirm the whole crate compiles**

```bash
cargo build -p tvrenamer
```

Expected: `Finished` with no errors.

- [ ] **Step 8: Commit**

```bash
git add src-tauri/src/ipc.rs src-tauri/src/lib.rs
git commit -m "feat(renamer): add perform_renames IPC command with conflict pre-scan and progress events"
```

---

## Coverage Gaps (Post-MVP, Not In This Plan)

The research identifies these test scenarios that are not covered here:

- Disk full / ENOSPC during copy (requires mocking `std::io::copy`)
- Very long path names
- Unicode filenames
- Cross-filesystem moves (requires two separate mount points in CI)

These can be addressed in a follow-up plan if prioritised.
