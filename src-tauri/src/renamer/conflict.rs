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
