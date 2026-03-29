# Research: Project Scaffolding (Tauri v2 + React)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Initialise the Tauri v2 project structure with Rust backend and React/TypeScript frontend. Set up all dependencies, capabilities, and directory layout so that subsequent modules (parser, TMDB, renamer, preferences, UI) can be built independently.

---

## Recommended Rust Module Structure

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
│   ├── patterns.rs      # The 8 compiled Regex patterns
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

## Rust Dependencies (Cargo.toml)

```toml
[dependencies]
tauri = { version = "2", features = [] }
serde = { version = "1", features = ["derive"] }
serde_json = "1"
reqwest = { version = "0.13", features = ["json"] }
tokio = { version = "1", features = ["full"] }
regex = "1"
thiserror = "1"
tracing = "0.1"
tracing-subscriber = "0.3"
keyring = "3.6"
tempfile = "3.10"
directories = "6.0"
```

Note: `once_cell` is superseded by `std::sync::OnceLock` (stable since Rust 1.70). Use `OnceLock` in new code.

---

## Required Tauri v2 Plugins

| Plugin | Purpose |
|--------|---------|
| `@tauri-apps/plugin-fs` | File system operations |
| `@tauri-apps/plugin-dialog` | Native file picker, message boxes |
| `@tauri-apps/plugin-updater` | Auto-update via GitHub Releases |
| `@tauri-apps/plugin-store` | Persistent key-value storage |

---

## Frontend npm Dependencies

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

## Tauri v2 Capabilities Configuration

File system access to `~/.tvrenamer/` must be explicitly declared in `src-tauri/capabilities/default.json`:

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

Scope path globs (`$HOME/.tvrenamer/**`) are declared separately in plugin scope config — verify the exact format against current Tauri v2 plugin-fs documentation before configuring.

**Warning:** Tauri v2 silently returns 403 errors (not clear error messages) when a file system operation isn't covered by a capability. Test home dir file access explicitly before building features on top of it.

---

## Tauri v2 Breaking Changes from v1

| Component | v1 | v2 |
|-----------|----|----|
| IPC import | `@tauri-apps/api/tauri` | `@tauri-apps/api/core` |
| File system | `readBinaryFile` | `readFile` |
| Allowlist | `tauri.allowlist.*` | Capabilities system |
| Drag-drop event (JS) | `tauri://file-drop` | `tauri://drag-drop` |
| Drag-drop config | `fileDropEnabled` | `dragDropEnabled` |
| Window type (Rust) | `Window` | `WebviewWindow` |

---

## Java Architecture (Port Reference)

The existing Java codebase has clean MVC separation:

```
org.tvrenamer
├── model/           # Data + domain objects (no UI, no SWT)
├── controller/      # Business logic + provider integration
└── view/            # SWT only — no business logic
```

Business logic in the controller layer does not leak into the SWT view, making it extractable. The Rust port maps model → structs/enums, controller → Tauri commands + module logic, view → React components.

Three independent thread pools in Java (`ListingsLookup.THREAD_POOL`, `ShowStore.threadPool`, `MoveRunner.EXECUTOR`) consolidate naturally into one Tokio async runtime.

---

## Error Enum (Shared Across Modules)

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

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| Tauri v2 multi-platform (Windows/macOS/Linux) | ✅ Valid |
| Tauri v2 IPC import is `@tauri-apps/api/core` | ✅ Valid |
| `tauri-action` cross-platform CI | ✅ Valid |
| Capabilities use `fs:allow-home-read` etc. (not `fs:scope`) | ✅ Corrected |
| `once_cell` superseded by `std::sync::OnceLock` | ⚠️ Use `OnceLock` in new code |
| `directories = "6.0"` (not 5.0) | ✅ Corrected |
| `reqwest = "0.13"` (not 0.12) | ✅ Corrected |
