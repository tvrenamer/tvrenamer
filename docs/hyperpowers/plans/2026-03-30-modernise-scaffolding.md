# TVRenamer Scaffolding (Tauri v2 + React) Implementation Plan

> **For Claude:** Run `/execute-plan` to implement this plan (will ask which execution style you prefer). Steps use checkbox (`- [ ]`) syntax for tracking.
> **Related Issues:** None detected in repository configuration.

**Goal:** Initialise the Tauri v2 project structure with Rust backend (`src-tauri/`) and React/TypeScript frontend (`ui/`) so that subsequent modules (parser, TMDB, renamer, preferences, UI) can each be built and tested independently.

**Architecture:** Tauri v2 with Rust backend managing all OS operations, exposed as typed IPC commands. React/TypeScript frontend lives in `ui/` — not `src/`, which already contains the Java source (`src/main/java/`). Module directories created as stubs; no business logic implemented here. Single static `reqwest::Client` stored in Tauri `AppState` via `manage()`. API key stored in OS keychain via `keyring` crate, not plaintext `prefs.json`.

**Tech Stack:**
- Rust: Tauri 2, reqwest 0.13, serde/serde_json 1, thiserror 1, tokio 1 (full), regex 1, tracing 0.1, tracing-subscriber 0.3, keyring 3.6, tempfile 3.10, directories 6.0
- Frontend: React 18, TypeScript 5, Vite 5, @tauri-apps/api v2, @tanstack/react-table v8, @tanstack/react-query v5, @tanstack/react-virtual v3

**Context Gathered From:**
- `docs/hyperpowers/research/2026-03-29-modernise-scaffolding.md`
- `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

> ⚠️ **Frontend directory:** React frontend goes in `ui/` (not `src/`) — `src/main/java/` already holds the Java source. `tauri.conf.json` sets `frontendDist: "../ui/dist"`.
>
> ⚠️ **`dragDropEnabled` is confusing:** The Tauri team acknowledges the naming is non-standard. `dragDropEnabled: false` disables Tauri's internal drag system and allows HTML5 drag-drop to work in the webview. `dragDropEnabled: true` (default) enables Tauri's internal system, but the exact event names and API for receiving file paths must be verified against current Tauri v2 docs before implementing file drop in the UI plan. The plan sets `true` as the conservative default — do not assume `tauri://drag-drop` event names without checking.
>
> ⚠️ **Capabilities scope glob:** `$HOME/.tvrenamer/**` scope format must be verified against current Tauri v2 `plugin-fs` docs before building any file-system module. Tauri v2 silently returns 403 on uncovered paths with no useful error message.
>
> ⚠️ **`/docs/` is in .gitignore:** This plan file and research docs exist on disk but are not committed. The gitignore task (Task 8) updates this.

---

### Task 1: Create Rust project skeleton

**Files:**
- Create: `src-tauri/Cargo.toml`
- Create: `src-tauri/build.rs`
- Create: `src-tauri/src/main.rs`

- [ ] **Step 1: Create `src-tauri/Cargo.toml`**

```toml
[package]
name = "tvrenamer"
version = "0.1.0"
description = "TVRenamer — Tauri v2 port"
authors = []
license = "GPL-3.0"
edition = "2021"
rust-version = "1.77"

[lib]
name = "tvrenamer_lib"
crate-type = ["staticlib", "cdylib", "rlib"]

[build-dependencies]
tauri-build = { version = "2", features = [] }

[dependencies]
tauri = { version = "2", features = [] }
tauri-plugin-fs = "2"
tauri-plugin-dialog = "2"
tauri-plugin-updater = "2"
tauri-plugin-store = "2"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
reqwest = { version = "0.13", features = ["json"] }
tokio = { version = "1", features = ["full"] }
regex = "1"
thiserror = "1"
tracing = "0.1"
tracing-subscriber = { version = "0.3", features = ["env-filter"] }
keyring = "3.6"
tempfile = "3.10"
directories = "6.0"

[profile.release]
codegen-units = 1
lto = true
opt-level = "s"
panic = "abort"
strip = true
```

- [ ] **Step 2: Create `src-tauri/build.rs`**

```rust
fn main() {
    tauri_build::build()
}
```

- [ ] **Step 3: Create `src-tauri/src/main.rs`**

```rust
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    tvrenamer_lib::run()
}
```

- [ ] **Step 4: Commit**

```bash
git add src-tauri/Cargo.toml src-tauri/build.rs src-tauri/src/main.rs
git commit -m "chore: add Rust project skeleton (Cargo.toml, build.rs, main.rs)"
```

---

### Task 2: Configure Tauri

`tauri_build::build()` (in `build.rs`) reads `tauri.conf.json` at compile time. This must exist before `cargo check` succeeds.

**Files:**
- Create: `src-tauri/tauri.conf.json`
- Create: `src-tauri/capabilities/default.json`
- Create: `src-tauri/icons/` (placeholder)

- [ ] **Step 1: Create `src-tauri/tauri.conf.json`**

```json
{
  "$schema": "https://schema.tauri.app/config/2",
  "productName": "TVRenamer",
  "version": "0.1.0",
  "identifier": "org.tvrenamer.app",
  "build": {
    "beforeDevCommand": "cd ui && npm run dev",
    "beforeBuildCommand": "cd ui && npm run build",
    "devUrl": "http://localhost:5173",
    "frontendDist": "../ui/dist"
  },
  "app": {
    "windows": [
      {
        "title": "TVRenamer",
        "width": 1200,
        "height": 700,
        "resizable": true,
        "fullscreen": false,
        "dragDropEnabled": true
      }
    ],
    "security": {
      "csp": null
    }
  },
  "bundle": {
    "active": true,
    "targets": "all",
    "icon": [
      "icons/32x32.png",
      "icons/128x128.png",
      "icons/128x128@2x.png",
      "icons/icon.icns",
      "icons/icon.ico"
    ]
  }
}
```

Note: The `plugins.updater` block is intentionally omitted from the scaffold. The updater requires a signed key pair — an empty `pubkey` string causes build failure. Add the updater configuration in the CI/CD plan after running `npm run tauri signer generate`.

- [ ] **Step 2: Create `src-tauri/capabilities/default.json`**

```json
{
  "$schema": "https://schema.tauri.app/capability/2",
  "identifier": "default",
  "description": "Default capabilities for TVRenamer",
  "windows": ["*"],
  "permissions": [
    "core:default",
    "fs:allow-home-read",
    "fs:allow-home-write",
    "fs:allow-home-read-recursive",
    "fs:allow-home-write-recursive",
    "dialog:default",
    "updater:default",
    "store:default"
  ]
}
```

⚠️ The `$HOME/.tvrenamer/**` scope glob format is separate from these permission identifiers. Verify exact format in plugin-fs scope configuration docs before implementing any file-system module. Test home-dir access explicitly before building on top of it.

- [ ] **Step 3: Create placeholder icons directory**

```bash
mkdir -p src-tauri/icons
echo "# Populate with 32x32.png, 128x128.png, 128x128@2x.png, icon.icns, icon.ico before release build" > src-tauri/icons/README.md
```

Icons are required for `tauri build` (release). `tauri dev` works without them for development.

- [ ] **Step 4: Commit**

```bash
git add src-tauri/tauri.conf.json src-tauri/capabilities/ src-tauri/icons/
git commit -m "chore: add tauri.conf.json, capabilities config, and icons placeholder"
```

---

### Task 3: Create shared AppError type

**Files:**
- Create: `src-tauri/src/errors.rs`

- [ ] **Step 1: Create `src-tauri/src/errors.rs` — test module only (will fail to compile)**

```rust
#[cfg(test)]
mod tests {
    use super::AppError;

    #[test]
    fn all_error_variants_serialize() {
        let variants: Vec<AppError> = vec![
            AppError::ApiKeyMissing,
            AppError::ApiDiscontinued,
            AppError::NetworkTimeout("timeout".into()),
            AppError::FileNotFound("path".into()),
            AppError::PermissionDenied("path".into()),
            AppError::DestinationExists,
            AppError::ParseFailed,
            AppError::PreferencesCorrupted,
        ];
        for v in &variants {
            serde_json::to_string(v).expect("AppError must be serializable to pass through IPC");
        }
    }
}
```

- [ ] **Step 2: Prepend AppError implementation above the test module**

```rust
use thiserror::Error;

#[derive(Error, Debug, serde::Serialize)]
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

The `#[cfg(test)]` block from Step 1 remains at the bottom of the file unchanged.

- [ ] **Step 3: Commit**

```bash
git add src-tauri/src/errors.rs
git commit -m "feat: add shared AppError enum with thiserror + serde serialization"
```

---

### Task 4: Create AppState

**Files:**
- Create: `src-tauri/src/state.rs`

- [ ] **Step 1: Create `src-tauri/src/state.rs` — test only**

```rust
#[cfg(test)]
mod tests {
    use super::AppState;

    #[test]
    fn app_state_constructs() {
        let state = AppState::new().expect("AppState::new() must succeed in normal environment");
        drop(state);
    }
}
```

- [ ] **Step 2: Prepend AppState implementation**

```rust
use std::time::Duration;
use crate::errors::AppError;

pub struct AppState {
    /// Shared HTTP client — one instance per process, connection pool reused across all TMDB calls.
    /// Never construct a new Client per request (destroys pooling; TMDB allows 20 concurrent connections).
    pub http_client: reqwest::Client,
}

impl AppState {
    pub fn new() -> Result<Self, AppError> {
        let http_client = reqwest::Client::builder()
            .connect_timeout(Duration::from_secs(10))
            .timeout(Duration::from_secs(30))
            .build()
            .map_err(|e| AppError::NetworkTimeout(e.to_string()))?;
        Ok(Self { http_client })
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src-tauri/src/state.rs
git commit -m "feat: add AppState with shared reqwest::Client (connect 10s, request 30s)"
```

---

### Task 5: Create module skeleton files

All modules created as stubs that compile but contain no business logic.

**Files (Create):**
- `src-tauri/src/parser/mod.rs`
- `src-tauri/src/metadata/mod.rs`, `provider.rs`, `tmdb.rs`, `models.rs`
- `src-tauri/src/renamer/mod.rs`, `mover.rs`, `conflict.rs`, `template.rs`
- `src-tauri/src/overrides/mod.rs`
- `src-tauri/src/config/mod.rs`, `prefs.rs`, `migration.rs`
- `src-tauri/src/ipc.rs`

- [ ] **Step 1: Create `src-tauri/src/parser/mod.rs`**

```rust
// Filename parser — ports FilenameParser.java
// 8 compiled regex patterns; 95 test cases in FilenameParserTest.java become #[test] functions here.
// Implementation: docs/hyperpowers/plans/2026-03-30-modernise-parser.md
```

- [ ] **Step 2: Create `src-tauri/src/metadata/mod.rs`**

```rust
// TMDB v3 client — replaces TheTVDB provider
// Implementation: docs/hyperpowers/plans/2026-03-30-modernise-tmdb.md
pub mod models;
pub mod provider;
pub mod tmdb;
```

- [ ] **Step 3: Create `src-tauri/src/metadata/models.rs`**

```rust
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Series {
    pub id: u32,
    pub name: String,
    pub first_air_date: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Episode {
    pub name: String,
    pub season_number: u32,
    pub episode_number: u32,
    pub air_date: Option<String>,
    pub overview: Option<String>,
}
```

- [ ] **Step 4: Create `src-tauri/src/metadata/provider.rs`**

```rust
// MetadataProvider trait — implemented by TmdbProvider
// Uses native async-in-trait (Rust 1.75+, required by Tauri's rust-version = "1.77")
use crate::errors::AppError;
use super::models::{Episode, Series};

pub trait MetadataProvider: Send + Sync {
    async fn search_series(&self, query: &str) -> Result<Vec<Series>, AppError>;
    async fn get_episode(
        &self,
        series_id: u32,
        season: u32,
        episode: u32,
    ) -> Result<Episode, AppError>;
}
```

- [ ] **Step 5: Create `src-tauri/src/metadata/tmdb.rs`**

```rust
// TmdbProvider — TMDB v3 REST API client
// API key fetched from OS keychain via keyring crate on each call (NOT stored in AppState)
// Implementation: metadata plan
use crate::errors::AppError;
use super::provider::MetadataProvider;

pub struct TmdbProvider {
    pub(crate) client: reqwest::Client,
}

impl TmdbProvider {
    pub fn new(client: reqwest::Client) -> Self {
        Self { client }
    }
}

impl MetadataProvider for TmdbProvider {
    async fn search_series(&self, _query: &str) -> Result<Vec<super::models::Series>, AppError> {
        unimplemented!("implement in metadata plan")
    }

    async fn get_episode(
        &self,
        _series_id: u32,
        _season: u32,
        _episode: u32,
    ) -> Result<super::models::Episode, AppError> {
        unimplemented!("implement in metadata plan")
    }
}
```

- [ ] **Step 6: Create `src-tauri/src/renamer/mod.rs`**

```rust
// Renamer — ports FileMover.java + MoveRunner.java conflict pre-scan
// Conflict algorithm: sort by size desc, largest → primary dest, others → versions/ with "(N)" suffix
// Implementation: docs/hyperpowers/plans/2026-03-30-modernise-renamer.md
pub mod conflict;
pub mod mover;
pub mod template;
```

- [ ] **Step 7: Create `src-tauri/src/renamer/mover.rs`**

```rust
// Atomic file move with copy-delete fallback for cross-filesystem moves (std::fs::rename fails cross-fs on Windows)
// Ports FileMover.java
```

- [ ] **Step 8: Create `src-tauri/src/renamer/conflict.rs`**

```rust
// Pre-move conflict detection — runs BEFORE any moves execute
// Ports MoveRunner.java pre-scan: group by dest dir, sort by source size desc, assign versions/ indices
// 4 ConflictTest scenarios are the acceptance criteria (see research doc)
```

- [ ] **Step 9: Create `src-tauri/src/renamer/template.rs`**

```rust
// Rename format string evaluation
// Default template: "%S [%sx%0e] %t"  (show name, season, zero-padded episode, title)
```

- [ ] **Step 10: Create `src-tauri/src/overrides/mod.rs`**

```rust
// Show name overrides — ports GlobalOverrides.java
// CRITICAL: GlobalOverrides.getShowName() exists in Java but is NEVER called in production.
// This port must wire it into the lookup flow: AFTER parser output, BEFORE provider query.
// Format: JSON array [{"from": "Archer (2009)", "to": "Archer"}]
// Migrated from etc/default-overrides.xml on first launch.
```

- [ ] **Step 11: Create `src-tauri/src/config/mod.rs`**

```rust
// Configuration — load/save UserPreferences + XML migration on first launch
// Implementation: docs/hyperpowers/plans/2026-03-30-modernise-preferences.md
pub mod migration;
pub mod prefs;
```

- [ ] **Step 12: Create `src-tauri/src/config/prefs.rs`**

```rust
use serde::{Deserialize, Serialize};

/// UserPreferences — mirrors Java UserPreferences (12 fields).
/// Stored at ~/.tvrenamer/prefs.json (see open question re: directories crate).
/// TMDB API key is NOT stored here — it lives in the OS keychain via the keyring crate.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserPreferences {
    pub preload_folder: Option<String>,
    pub dest_dir: String,
    pub season_prefix: String,
    pub season_prefix_leading_zero: bool,
    pub move_selected: bool,
    pub rename_selected: bool,
    pub remove_emptied_directories: bool,
    pub delete_row_after_move: bool,
    pub rename_replacement_mask: String,
    pub check_for_updates: bool,
    pub recursively_add_folders: bool,
    pub ignore_keywords: Vec<String>,
}

impl Default for UserPreferences {
    fn default() -> Self {
        Self {
            preload_folder: None,
            dest_dir: "~/TV".to_string(),
            season_prefix: "Season ".to_string(),
            season_prefix_leading_zero: false,
            move_selected: false,
            rename_selected: true,
            remove_emptied_directories: true,
            delete_row_after_move: false,
            rename_replacement_mask: "%S [%sx%0e] %t".to_string(),
            check_for_updates: true,
            recursively_add_folders: true,
            ignore_keywords: vec!["sample".to_string()],
        }
    }
}

#[cfg(test)]
mod tests {
    use super::UserPreferences;

    #[test]
    fn default_prefs_serialize_round_trip() {
        let prefs = UserPreferences::default();
        let json = serde_json::to_string(&prefs).expect("serialize");
        let back: UserPreferences = serde_json::from_str(&json).expect("deserialize");
        assert_eq!(back.rename_replacement_mask, "%S [%sx%0e] %t");
        assert_eq!(back.season_prefix, "Season ");
        assert_eq!(back.ignore_keywords, vec!["sample"]);
        assert!(!back.move_selected);
        assert!(back.rename_selected);
    }
}
```

- [ ] **Step 13: Create `src-tauri/src/config/migration.rs`**

```rust
// XML → JSON migration for preferences.xml and overrides.xml (XStream format)
// On first launch: check for ~/.tvrenamer/prefs.json FIRST — skip if it already exists.
// If not found, check for ~/.tvrenamer/preferences.xml — migrate if present.
// Leave preferences.xml in place as a backup after migration.
// Implementation: preferences plan
```

- [ ] **Step 14: Create `src-tauri/src/ipc.rs`**

```rust
// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// IPC error convention: commands return Result<T, String> (AppError serialized via Display trait).

/// Smoke-test command — verifies the IPC bridge is operational.
/// Remove or replace once the first real command is implemented.
#[tauri::command]
pub async fn ping() -> Result<String, String> {
    Ok("pong".to_string())
}
```

- [ ] **Step 15: Commit**

```bash
git add src-tauri/src/parser/ src-tauri/src/metadata/ src-tauri/src/renamer/ \
        src-tauri/src/overrides/ src-tauri/src/config/ src-tauri/src/ipc.rs
git commit -m "chore: add stub modules for parser, metadata, renamer, overrides, config, ipc"
```

---

### Task 6: Wire up lib.rs and verify cargo check

**Files:**
- Create: `src-tauri/src/lib.rs`

- [ ] **Step 1: Create `src-tauri/src/lib.rs`**

```rust
mod config;
mod errors;
mod ipc;
mod metadata;
mod overrides;
mod parser;
mod renamer;
mod state;

use state::AppState;

pub fn run() {
    tracing_subscriber::fmt::init();

    let state = AppState::new().expect("Failed to initialise AppState");

    tauri::Builder::default()
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_store::Builder::default().build())
        .manage(state)
        .invoke_handler(tauri::generate_handler![ipc::ping])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

- [ ] **Step 2: Run cargo check**

```bash
cargo check --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected: clean (zero errors; dead-code/unused warnings at scaffold stage are acceptable).

If errors:
- Missing `pub` on module items — add `pub`
- `unimplemented!()` in trait impl — already `impl Trait` so this is fine at check time
- `tauri.conf.json` not found — confirm Task 2 was completed

- [ ] **Step 3: Run unit tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected:
```
test config::prefs::tests::default_prefs_serialize_round_trip ... ok
test errors::tests::all_error_variants_serialize ... ok
test state::tests::app_state_constructs ... ok

test result: ok. 3 passed; 0 failed
```

- [ ] **Step 4: Commit**

```bash
git add src-tauri/src/lib.rs
git commit -m "feat: wire lib.rs — registers Tauri plugins, AppState, and ping IPC command"
```

---

### Task 7: Initialize React/TypeScript frontend in ui/

**Files (Create):**
- `ui/package.json`
- `ui/vite.config.ts`
- `ui/tsconfig.json`
- `ui/index.html`
- `ui/src/vite-env.d.ts`
- `ui/src/main.tsx`
- `ui/src/App.tsx`

- [ ] **Step 1: Create `ui/package.json`**

```json
{
  "name": "tvrenamer-ui",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@tauri-apps/api": "^2",
    "@tanstack/react-query": "^5.0",
    "@tanstack/react-table": "^8.17",
    "@tanstack/react-virtual": "^3.0",
    "react": "^18",
    "react-dom": "^18"
  },
  "devDependencies": {
    "@types/react": "^18",
    "@types/react-dom": "^18",
    "@vitejs/plugin-react": "^4",
    "typescript": "^5",
    "vite": "^5"
  }
}
```

- [ ] **Step 2: Create `ui/vite.config.ts`**

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    port: 5173,
    strictPort: true,
    watch: {
      ignored: ["**/src-tauri/**"],
    },
  },
  envPrefix: ["VITE_", "TAURI_"],
  build: {
    // Tauri expects ES2020 + modern targets; adjust per platform if needed
    target: process.env.TAURI_ENV_PLATFORM === "windows" ? "chrome105" : "safari13",
    minify: !process.env.TAURI_ENV_DEBUG ? "esbuild" : false,
    sourcemap: !!process.env.TAURI_ENV_DEBUG,
  },
});
```

- [ ] **Step 3: Create `ui/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"]
}
```

- [ ] **Step 4: Create `ui/index.html`**

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TVRenamer</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: Create `ui/src/vite-env.d.ts`**

```typescript
/// <reference types="vite/client" />
```

- [ ] **Step 6: Create `ui/src/main.tsx`**

```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

- [ ] **Step 7: Create `ui/src/App.tsx`**

```tsx
import { invoke } from "@tauri-apps/api/core";
import { useState } from "react";

function App() {
  const [pingResult, setPingResult] = useState<string>("");

  async function testPing() {
    const result = await invoke<string>("ping");
    setPingResult(result);
  }

  return (
    <div>
      <h1>TVRenamer</h1>
      <p>Scaffold placeholder — file table implemented in UI plan.</p>
      <button onClick={testPing}>Test IPC ping</button>
      {pingResult && <p>IPC response: {pingResult}</p>}
    </div>
  );
}

export default App;
```

- [ ] **Step 8: Install frontend dependencies**

```bash
npm install --prefix ui
```

Expected: no errors; `ui/node_modules/` created.

- [ ] **Step 9: Verify TypeScript compiles**

```bash
npm run --prefix ui build 2>&1
```

Expected: `ui/dist/` built successfully with no TypeScript errors.

- [ ] **Step 10: Commit**

```bash
git add ui/
git commit -m "chore: add React/TypeScript frontend scaffold in ui/ with Vite and TanStack deps"
```

---

### Task 8: Configure root workspace and update .gitignore

**Files:**
- Create: `package.json` (root)
- Modify: `.gitignore`

- [ ] **Step 1: Create root `package.json`**

```json
{
  "scripts": {
    "dev": "tauri dev",
    "build": "tauri build"
  },
  "devDependencies": {
    "@tauri-apps/cli": "^2"
  }
}
```

- [ ] **Step 2: Install root dependencies**

```bash
npm install
```

Expected: `node_modules/@tauri-apps/cli` installed; `npx tauri --version` prints `tauri-cli 2.x.x`.

- [ ] **Step 3: Update .gitignore**

The current `.gitignore` ignores `/docs/` — this hides the hyperpowers research and plans. Remove the `/docs/` rule and add targeted ignores instead:

In `.gitignore`, replace:
```
/docs/
```
With:
```
# Rust / Tauri
src-tauri/target/
src-tauri/WixTools/

# Frontend
ui/node_modules/
ui/dist/

# Root workspace
node_modules/
```

- [ ] **Step 4: Stage and confirm docs/ is now tracked**

```bash
git status docs/
```

Expected: shows `docs/hyperpowers/` files as untracked (now trackable).

- [ ] **Step 5: Commit**

```bash
git add package.json package-lock.json .gitignore
git commit -m "chore: add root package.json (tauri CLI), update .gitignore to unblock docs/"
```

---

### Task 9: End-to-end build verification

- [ ] **Step 1: Run all Rust unit tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected:
```
test config::prefs::tests::default_prefs_serialize_round_trip ... ok
test errors::tests::all_error_variants_serialize ... ok
test state::tests::app_state_constructs ... ok

test result: ok. 3 passed; 0 failed
```

- [ ] **Step 2: Run cargo check**

```bash
cargo check --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected: no errors.

- [ ] **Step 3: Verify Tauri CLI**

```bash
npx tauri info 2>&1
```

Expected: prints system info including Rust toolchain version, Tauri CLI version, and confirms `tauri.conf.json` is valid.

- [ ] **Step 4: Run tauri dev — smoke test**

```bash
npm run dev
```

Expected:
1. Vite dev server starts on `http://localhost:5173`
2. Tauri compiles Rust backend
3. Window opens showing "TVRenamer" with "Test IPC ping" button
4. Click the button → "IPC response: pong" appears

Press Ctrl+C to stop.

If the window fails to open:
- Run `npx tauri info` to verify configuration
- Check `src-tauri/capabilities/default.json` identifiers match installed plugin versions
- Confirm `ui/dist/` exists (run `npm run --prefix ui build` first if dev server isn't starting)

- [ ] **Step 5: Commit final state**

```bash
git add -A
git commit -m "chore: verified scaffold — cargo test passes, tauri dev IPC ping works"
```

---

## Validated Assumptions

*Validated by assumption-checker agent (23 ✅, 2 ❌ corrected in plan, 1 ⚠️)*

✅ `src-tauri/` does not yet exist in the repo (confirmed by Glob)
✅ `src/main/java/` already occupies `src/` — frontend placed in `ui/` to avoid conflict
✅ `tauri_build::build()` requires `tauri.conf.json` at compile time — Task 2 creates it before any `cargo check`
✅ Native async-in-trait available in Rust 1.75+ — no `async_trait` crate needed (`rust-version = "1.77"`)
✅ `reqwest = "0.13"` current stable (0.13.2 as of early 2026) — per research
✅ `directories = "6.0"` current stable (6.0.1 Jan 2025) — per research
✅ `keyring = "3.6"` current stable — per research
✅ `std::sync::OnceLock` stable since Rust 1.70 — plan contains no `once_cell` usage
✅ `tauri_plugin_fs::init()` correct init API
✅ `tauri_plugin_dialog::init()` correct init API
✅ `tauri_plugin_updater::Builder::new().build()` correct init API
✅ `tauri_plugin_store::Builder::default().build()` correct init API
✅ Capabilities identifiers `"fs:allow-home-read"` etc. correct format for Tauri v2
✅ `@tauri-apps/api/core` correct import path for Tauri v2 IPC
✅ `invoke<T>(command, args)` correct IPC call signature
✅ `process.env.TAURI_ENV_PLATFORM` set by Tauri CLI during build
✅ `process.env.TAURI_ENV_DEBUG` set by Tauri CLI in dev mode
✅ `.manage(state)` stores `AppState` accessible via `State<AppState>` in commands
✅ `tauri-action` generates `latest.json` automatically for updater endpoint
✅ `"frontendDist": "../ui/dist"` relative path format supported
✅ Capabilities `$schema`: `"https://schema.tauri.app/capability/2"` correct
✅ `tauri.conf.json` `$schema`: `"https://schema.tauri.app/config/2"` correct

❌ **`"updater.pubkey": ""` (empty string) — CORRECTED:** Empty pubkey causes build failure. Plan updated to omit `plugins.updater` block entirely from the scaffold. Configure in CI/CD plan after generating keys with `npm run tauri signer generate`.

❌ **`dragDropEnabled: true` fires `tauri://drag-*` named events — CORRECTED:** Tauri team acknowledges the naming is confusing. `true` enables Tauri's internal drag system, but the exact event API must be verified in the UI plan. Plan warning updated; scaffold sets `true` as default only.

⚠️ **`reqwest::Client::builder().connect_timeout().timeout().build()` builder method order** — likely correct but verify against 0.13 docs during implementation

---

## Open Questions Carried Forward

1. **`keyring` on headless Linux:** `keyring` crate requires a Secret Service daemon (e.g., `gnome-keyring`). On headless Linux (CI, servers), this isn't available. Fallback strategy needed in the preferences plan: encrypted config file or `prefs.json` with a warning.

2. **`~/.tvrenamer/` vs `directories` crate:** Design doc specifies `~/.tvrenamer/` directly. The `directories` crate gives platform-correct paths (`~/Library/Application Support/tvrenamer` on macOS, `%APPDATA%\tvrenamer` on Windows). Decision must be made before implementing the config module — it affects Tauri capability scope globs.

3. **Tauri capabilities scope glob format:** `$HOME/.tvrenamer/**` glob syntax must be verified against current Tauri v2 `plugin-fs` documentation before any file-system module is built.

4. **App icons:** `src-tauri/icons/` must be populated with correctly sized images before `tauri build` produces a release artifact. Use `npm run tauri icon <source-image.png>` to auto-generate all sizes from a 1024×1024 source.

5. **Updater pubkey:** Generate with `npm run tauri signer generate`. Store private key as a GitHub Actions secret; embed public key in `tauri.conf.json` before first release build.

6. **XStream XML migration:** Java's XStream format has unusual structure for nested objects. The preferences plan should evaluate whether to implement XStream parsing in Rust or ship a one-time Java migration utility.
