// Atomic file move with copy-delete fallback for cross-filesystem moves.
// Ports FileMover.java.
// move_file is synchronous — wrap in tokio::task::spawn_blocking for async callers.

use std::io;
use std::path::Path;

use renamore;
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
        Ok(_) => return Ok(MoveStatus::Success),
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
