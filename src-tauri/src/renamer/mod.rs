// Renamer — ports FileMover.java + MoveRunner.java conflict pre-scan
// Conflict algorithm: sort by size desc, largest → primary dest, others → versions/ with "(N)" suffix
// Implementation: docs/hyperpowers/plans/2026-03-30-modernise-renamer.md
pub mod conflict;
pub mod mover;
pub mod template;
