# Preferences, Migration & Show Overrides Implementation Plan

> **For Claude:** Run `/execute-plan` to implement this plan (will ask which execution style you prefer). Steps use checkbox (`- [ ]`) syntax for tracking.
> **Related Issues:** None detected.

**Goal:** Implement `config/prefs.rs` load/save, `config/migration.rs` (XStream XML → JSON via `quick-xml`), and `overrides/mod.rs` (show name overrides with bundled defaults); wire migration into a Tauri setup hook; expose `get_preferences` and `save_preferences` IPC commands; apply overrides in `search_shows`.

**Architecture:** Preferences and overrides are loaded at startup inside a Tauri `.setup()` hook using `app.path().app_config_dir()` (the platform-standard path: `~/Library/Application Support/org.tvrenamer.app/` on macOS). Migration reads from `~/.tvrenamer/preferences.xml` and `~/.tvrenamer/overrides.xml` (the old Java locations) using a hand-written `quick-xml` event-based parser, then writes JSON to the new config dir. `UserPreferences` and `ShowOverride` are stored as separate Tauri managed state (`PrefsState`, `OverridesState`). All parsing functions take `&Path` and have no Tauri dependency — testable in isolation with `tempfile`.

**Tech Stack:**
- `quick-xml = "0.39"` (new dependency — latest stable as of 2026-02; event-based XML reader, no features flag needed)
- `directories = "6.0"` (already in Cargo.toml — used for `UserDirs::home_dir()` in migration)
- `serde_json = "1"` (already present — JSON load/save)
- `tempfile = "3.10"` (already in dev-dependencies — used in all file-touching tests)
- Tauri v2 `app.path().app_config_dir()` — setup hook path resolution

**Context Gathered From:**
- `docs/hyperpowers/research/2026-03-29-modernise-preferences.md`

**Decisions made during planning:**
- Config path: Tauri platform-correct (`app.path().app_config_dir()`)
- XML parsing: hand-written `quick-xml` event-based parser
- Migration source: `~/.tvrenamer/preferences.xml` and `~/.tvrenamer/overrides.xml`

---

> ⚠️ **`prefs.rs` is partially implemented:** The `UserPreferences` struct and `Default` impl already exist with a round-trip test. Tasks 1–2 ADD to this file — do not rewrite it.
>
> ⚠️ **XStream field aliases:** Java's `UserPreferencesPersistence.java` aliases `moveSelected` → `<moveEnabled>` and `renameSelected` → `<renameEnabled>` in XML. The parser must map these aliases back. All other field names match.
>
> ⚠️ **Overrides are currently orphaned:** `GlobalOverrides.getShowName()` exists in the Java source but is never called in production. This plan fixes that — wiring override application into `search_shows` is a bug fix, not a new feature.
>
> ⚠️ **`setup` hook error handling:** `app.path().app_config_dir()` returns `Result<PathBuf, tauri::Error>`. Migration failures are logged as warnings and are not fatal — the app falls back to defaults.
>
> ⚠️ **`quick-xml` version:** Plan uses `"0.39"` (latest stable 0.39.2 as of 2026-02). Run `cargo add quick-xml` to confirm the current version before adding manually.

---

### Task 1: Add `quick-xml` dependency

**Files:**
- Modify: `src-tauri/Cargo.toml`

- [ ] **Step 1: Add `quick-xml` to `[dependencies]`**

In `src-tauri/Cargo.toml`, add after the `directories` line:

```toml
quick-xml = "0.39"
```

- [ ] **Step 2: Verify it resolves**

```bash
cargo check --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected: no errors (a download + compile may occur).

- [ ] **Step 3: Commit**

```bash
git add src-tauri/Cargo.toml src-tauri/Cargo.lock
git commit -m "chore: add quick-xml for XStream preferences migration"
```

---

### Task 2: Add load/save and version field to `config/prefs.rs`

The struct and `Default` already exist. This task adds a `version` field (for future schema migration), and the `load()` / `save()` functions with tests.

**Files:**
- Modify: `src-tauri/src/config/prefs.rs`

- [ ] **Step 1: Write the failing tests first**

Add this `#[cfg(test)]` block inside `mod tests` in `prefs.rs` (below the existing `default_prefs_serialize_round_trip` test):

```rust
    #[test]
    fn load_missing_returns_defaults() {
        let dir = tempfile::tempdir().unwrap();
        let prefs = super::load(dir.path()).expect("load from empty dir should return defaults");
        assert_eq!(prefs.dest_dir, "~/TV");
        assert!(prefs.rename_selected);
        assert!(!prefs.move_selected);
        assert_eq!(prefs.ignore_keywords, vec!["sample"]);
    }

    #[test]
    fn save_and_load_roundtrip() {
        let dir = tempfile::tempdir().unwrap();
        let mut prefs = super::UserPreferences::default();
        prefs.dest_dir = "/mnt/tv".to_string();
        prefs.move_selected = true;
        super::save(&prefs, dir.path()).expect("save should succeed");
        let loaded = super::load(dir.path()).expect("load should succeed after save");
        assert_eq!(loaded.dest_dir, "/mnt/tv");
        assert!(loaded.move_selected);
    }

    #[test]
    fn load_corrupted_returns_err() {
        let dir = tempfile::tempdir().unwrap();
        std::fs::write(dir.path().join("prefs.json"), b"not json {{{{").unwrap();
        let result = super::load(dir.path());
        assert!(matches!(result, Err(crate::errors::AppError::PreferencesCorrupted)));
    }

    #[test]
    fn saved_json_contains_version_1() {
        let dir = tempfile::tempdir().unwrap();
        super::save(&super::UserPreferences::default(), dir.path()).unwrap();
        let raw = std::fs::read_to_string(dir.path().join("prefs.json")).unwrap();
        assert!(raw.contains("\"version\": 1"), "prefs.json must include version field");
    }
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
cargo test --manifest-path src-tauri/Cargo.toml config::prefs 2>&1
```

Expected: compile error — `load` and `save` not yet defined.

- [ ] **Step 3: Add `version` field to `UserPreferences`**

In `prefs.rs`, add the `version` field after the `use` imports and `#[derive]`:

```rust
use std::fs;
use std::path::Path;
use crate::errors::AppError;
```

Add `version` as the first field in the struct:

```rust
    #[serde(default = "default_prefs_version")]
    pub version: u32,
```

Add this free function above `impl Default`:

```rust
fn default_prefs_version() -> u32 {
    1
}
```

Add `version: 1` as the first entry in `Default::default()`:

```rust
            version: 1,
```

- [ ] **Step 4: Add `load()` and `save()` functions**

Add after `impl Default for UserPreferences { ... }`, before `#[cfg(test)]`:

```rust
/// Load preferences from `{config_dir}/prefs.json`.
/// Returns `Ok(UserPreferences::default())` if the file doesn't exist.
/// Returns `Err(PreferencesCorrupted)` if the file exists but cannot be parsed.
pub fn load(config_dir: &Path) -> Result<UserPreferences, AppError> {
    let path = config_dir.join("prefs.json");
    if !path.exists() {
        return Ok(UserPreferences::default());
    }
    let content = fs::read_to_string(&path)
        .map_err(|_| AppError::PreferencesCorrupted)?;
    serde_json::from_str(&content)
        .map_err(|_| AppError::PreferencesCorrupted)
}

/// Save preferences to `{config_dir}/prefs.json`.
/// Creates `config_dir` if it doesn't exist.
pub fn save(prefs: &UserPreferences, config_dir: &Path) -> Result<(), AppError> {
    fs::create_dir_all(config_dir)
        .map_err(|e| AppError::PermissionDenied(e.to_string()))?;
    let path = config_dir.join("prefs.json");
    let content = serde_json::to_string_pretty(prefs)
        .expect("UserPreferences is always serializable");
    fs::write(&path, content)
        .map_err(|e| AppError::PermissionDenied(e.to_string()))
}
```

- [ ] **Step 5: Run tests — confirm they pass**

```bash
cargo test --manifest-path src-tauri/Cargo.toml config::prefs 2>&1
```

Expected:
```
test config::prefs::tests::default_prefs_serialize_round_trip ... ok
test config::prefs::tests::load_corrupted_returns_err ... ok
test config::prefs::tests::load_missing_returns_defaults ... ok
test config::prefs::tests::save_and_load_roundtrip ... ok
test config::prefs::tests::saved_json_contains_version_1 ... ok

test result: ok. 5 passed; 0 failed
```

- [ ] **Step 6: Commit**

```bash
git add src-tauri/src/config/prefs.rs
git commit -m "feat(config): add version field + load/save to UserPreferences"
```

---

### Task 3: Implement `overrides/mod.rs`

**Must complete before Task 4** — `migration.rs` imports `ShowOverride` from this module.

**Files:**
- Modify: `src-tauri/src/overrides/mod.rs`

- [ ] **Step 1: Write the failing tests first**

Replace `src-tauri/src/overrides/mod.rs` with the test module only:

```rust
// Show name overrides — ports GlobalOverrides.java.
// CRITICAL: GlobalOverrides.getShowName() existed in Java but was NEVER called in production.
// This module fixes that — apply_override() is called in ipc::search_shows BEFORE the TMDB query.
// Format: JSON array [{"from": "Archer (2009)", "to": "Archer"}]

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn apply_override_known_name() {
        let overrides = bundled_defaults();
        assert_eq!(apply_override("Archer (2009)", &overrides), "Archer");
        assert_eq!(apply_override("House of Cards (2013)", &overrides), "House of Cards");
        assert_eq!(apply_override("The Newsroom (2012)", &overrides), "The Newsroom");
    }

    #[test]
    fn apply_override_unknown_name_passthrough() {
        let overrides = bundled_defaults();
        assert_eq!(apply_override("Breaking Bad", &overrides), "Breaking Bad");
        assert_eq!(apply_override("", &overrides), "");
    }

    #[test]
    fn bundled_defaults_has_three_entries() {
        let defaults = bundled_defaults();
        assert_eq!(defaults.len(), 3);
        assert!(defaults.iter().any(|o| o.from == "Archer (2009)" && o.to == "Archer"));
        assert!(defaults.iter().any(|o| o.from == "House of Cards (2013)"));
        assert!(defaults.iter().any(|o| o.from == "The Newsroom (2012)"));
    }

    #[test]
    fn load_missing_returns_bundled_defaults() {
        let dir = tempfile::tempdir().unwrap();
        let loaded = load(dir.path());
        assert_eq!(loaded, bundled_defaults());
    }

    #[test]
    fn load_from_json_file() {
        let dir = tempfile::tempdir().unwrap();
        let custom = vec![ShowOverride {
            from: "Test Show (2020)".to_owned(),
            to: "Test Show".to_owned(),
        }];
        let json = serde_json::to_string_pretty(&custom).unwrap();
        std::fs::write(dir.path().join("overrides.json"), json).unwrap();

        let loaded = load(dir.path());
        assert_eq!(loaded.len(), 1);
        assert_eq!(loaded[0].from, "Test Show (2020)");
        assert_eq!(loaded[0].to, "Test Show");
    }

    #[test]
    fn load_corrupted_json_returns_bundled_defaults() {
        let dir = tempfile::tempdir().unwrap();
        std::fs::write(dir.path().join("overrides.json"), b"not json").unwrap();
        let loaded = load(dir.path());
        assert_eq!(loaded, bundled_defaults());
    }
}
```

- [ ] **Step 2: Run — confirm compile failure**

```bash
cargo test --manifest-path src-tauri/Cargo.toml overrides 2>&1
```

Expected: compile error — `ShowOverride`, `apply_override`, `bundled_defaults`, `load` not defined.

- [ ] **Step 3: Implement the module**

Prepend the implementation above the `#[cfg(test)]` block:

```rust
use std::fs;
use std::path::Path;

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct ShowOverride {
    pub from: String,
    pub to: String,
}

/// Apply a show name override, returning the mapped name.
/// Returns the original name unchanged if no override matches.
pub fn apply_override(show_name: &str, overrides: &[ShowOverride]) -> String {
    overrides
        .iter()
        .find(|o| o.from == show_name)
        .map(|o| o.to.clone())
        .unwrap_or_else(|| show_name.to_owned())
}

/// Built-in defaults sourced from `etc/default-overrides.xml`.
/// Returned when no `overrides.json` exists yet.
pub fn bundled_defaults() -> Vec<ShowOverride> {
    vec![
        ShowOverride {
            from: "House of Cards (2013)".to_owned(),
            to: "House of Cards".to_owned(),
        },
        ShowOverride {
            from: "Archer (2009)".to_owned(),
            to: "Archer".to_owned(),
        },
        ShowOverride {
            from: "The Newsroom (2012)".to_owned(),
            to: "The Newsroom".to_owned(),
        },
    ]
}

/// Load show overrides from `{config_dir}/overrides.json`.
/// Returns `bundled_defaults()` if the file is absent or unparseable.
/// This function is intentionally infallible — override failures are non-fatal.
pub fn load(config_dir: &Path) -> Vec<ShowOverride> {
    let path = config_dir.join("overrides.json");
    if !path.exists() {
        return bundled_defaults();
    }
    let content = match fs::read_to_string(&path) {
        Ok(c) => c,
        Err(_) => return bundled_defaults(),
    };
    serde_json::from_str(&content).unwrap_or_else(|_| bundled_defaults())
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
cargo test --manifest-path src-tauri/Cargo.toml overrides 2>&1
```

Expected:
```
test overrides::tests::apply_override_known_name ... ok
test overrides::tests::apply_override_unknown_name_passthrough ... ok
test overrides::tests::bundled_defaults_has_three_entries ... ok
test overrides::tests::load_corrupted_json_returns_bundled_defaults ... ok
test overrides::tests::load_from_json_file ... ok
test overrides::tests::load_missing_returns_bundled_defaults ... ok

test result: ok. 6 passed; 0 failed
```

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/overrides/mod.rs
git commit -m "feat(overrides): implement ShowOverride with apply_override, load, and bundled defaults"
```

---

### Task 4: Implement `config/migration.rs`

This module parses XStream-format XML (the Java app's on-disk format) and writes JSON to the new config dir. The module only uses `std`, `quick_xml`, `serde_json`, and `directories` — no Tauri dependency.

**Files:**
- Modify: `src-tauri/src/config/migration.rs`

The XStream XML format for `preferences.xml` (field aliases `moveEnabled`/`renameEnabled` apply):
```xml
<preferences>
  <destDir>~/TV</destDir>
  <seasonPrefix>Season </seasonPrefix>
  <seasonPrefixLeadingZero>false</seasonPrefixLeadingZero>
  <moveEnabled>false</moveEnabled>
  <renameEnabled>true</renameEnabled>
  <removeEmptiedDirectories>true</removeEmptiedDirectories>
  <deleteRowAfterMove>false</deleteRowAfterMove>
  <renameReplacementMask>%S [%sx%0e] %t</renameReplacementMask>
  <checkForUpdates>true</checkForUpdates>
  <recursivelyAddFolders>true</recursivelyAddFolders>
  <ignoreKeywords><string>sample</string></ignoreKeywords>
</preferences>
```

The XStream XML format for `overrides.xml`:
```xml
<overrides>
  <showNames>
    <entry>
      <string>Archer (2009)</string>
      <string>Archer</string>
    </entry>
  </showNames>
</overrides>
```

- [ ] **Step 1: Write the failing tests first**

Replace `src-tauri/src/config/migration.rs` with:

```rust
// XML → JSON migration for preferences.xml and overrides.xml (XStream format).
// Run at startup via try_migrate_preferences() and try_migrate_overrides().
// On first launch: check for prefs.json FIRST — skip if it already exists.
// If not found, check for ~/.tvrenamer/preferences.xml — migrate if present.
// The original XML file is left in place as a backup after migration.

#[cfg(test)]
mod tests {
    use super::{parse_preferences_xml, parse_overrides_xml};
    use crate::config::prefs::UserPreferences;
    use crate::overrides::ShowOverride;

    #[test]
    fn parse_preferences_xml_full_with_aliases() {
        let xml = r#"<preferences>
          <destDir>/mnt/tv</destDir>
          <seasonPrefix>S</seasonPrefix>
          <seasonPrefixLeadingZero>true</seasonPrefixLeadingZero>
          <moveEnabled>true</moveEnabled>
          <renameEnabled>false</renameEnabled>
          <removeEmptiedDirectories>false</removeEmptiedDirectories>
          <deleteRowAfterMove>true</deleteRowAfterMove>
          <renameReplacementMask>%S %e %t</renameReplacementMask>
          <checkForUpdates>false</checkForUpdates>
          <recursivelyAddFolders>false</recursivelyAddFolders>
          <ignoreKeywords><string>sample</string><string>extras</string></ignoreKeywords>
        </preferences>"#;

        let prefs = parse_preferences_xml(xml).unwrap();
        assert_eq!(prefs.dest_dir, "/mnt/tv");
        assert_eq!(prefs.season_prefix, "S");
        assert!(prefs.season_prefix_leading_zero);
        assert!(prefs.move_selected);
        assert!(!prefs.rename_selected);
        assert!(!prefs.remove_emptied_directories);
        assert!(prefs.delete_row_after_move);
        assert_eq!(prefs.rename_replacement_mask, "%S %e %t");
        assert!(!prefs.check_for_updates);
        assert!(!prefs.recursively_add_folders);
        assert_eq!(prefs.ignore_keywords, vec!["sample", "extras"]);
    }

    #[test]
    fn parse_preferences_xml_absent_fields_use_defaults() {
        // Only destDir present — everything else stays at Default
        let xml = "<preferences><destDir>/mnt/movies</destDir></preferences>";
        let prefs = parse_preferences_xml(xml).unwrap();
        assert_eq!(prefs.dest_dir, "/mnt/movies");
        // Defaults should be preserved for absent fields
        assert!(prefs.rename_selected);
        assert!(!prefs.move_selected);
        assert_eq!(prefs.ignore_keywords, vec!["sample"]);
    }

    #[test]
    fn parse_preferences_xml_empty_ignore_keywords_element() {
        // <ignoreKeywords/> self-closing = user cleared all keywords
        let xml = "<preferences><ignoreKeywords/></preferences>";
        let prefs = parse_preferences_xml(xml).unwrap();
        assert!(prefs.ignore_keywords.is_empty());
    }

    #[test]
    fn parse_preferences_xml_corrupted_returns_err() {
        let xml = "<preferences><destDir>not closed";
        // May succeed with partial parse or return error — either way, must not panic
        let _ = parse_preferences_xml(xml);
        // Explicit corruption: not valid XML at all
        let result = parse_preferences_xml("not xml at all {{{{");
        // We accept either Ok(defaults) or Err — but must not panic
        let _ = result;
    }

    #[test]
    fn parse_overrides_xml_three_entries() {
        let xml = r#"<overrides>
          <showNames>
            <entry><string>House of Cards (2013)</string><string>House of Cards</string></entry>
            <entry><string>Archer (2009)</string><string>Archer</string></entry>
            <entry><string>The Newsroom (2012)</string><string>The Newsroom</string></entry>
          </showNames>
        </overrides>"#;

        let overrides = parse_overrides_xml(xml).unwrap();
        assert_eq!(overrides.len(), 3);
        assert_eq!(overrides[0].from, "House of Cards (2013)");
        assert_eq!(overrides[0].to, "House of Cards");
        assert_eq!(overrides[1].from, "Archer (2009)");
        assert_eq!(overrides[1].to, "Archer");
    }

    #[test]
    fn parse_overrides_xml_empty_returns_empty_vec() {
        let xml = "<overrides><showNames/></overrides>";
        let overrides = parse_overrides_xml(xml).unwrap();
        assert!(overrides.is_empty());
    }

    #[test]
    fn migrate_preferences_skips_if_prefs_json_exists() {
        let config_dir = tempfile::tempdir().unwrap();
        // Write a prefs.json with a distinctive dest_dir
        let existing_content = r#"{"version":1,"preload_folder":null,"dest_dir":"/existing","season_prefix":"Season ","season_prefix_leading_zero":false,"move_selected":false,"rename_selected":true,"remove_emptied_directories":true,"delete_row_after_move":false,"rename_replacement_mask":"%S [%sx%0e] %t","check_for_updates":true,"recursively_add_folders":true,"ignore_keywords":["sample"]}"#;
        std::fs::write(config_dir.path().join("prefs.json"), existing_content).unwrap();

        // try_migrate_preferences should not overwrite it
        super::try_migrate_preferences(config_dir.path()).unwrap();

        let loaded = crate::config::prefs::load(config_dir.path()).unwrap();
        assert_eq!(loaded.dest_dir, "/existing", "existing prefs.json must not be overwritten");
    }

    #[test]
    fn migrate_preferences_from_xml_creates_prefs_json() {
        let config_dir = tempfile::tempdir().unwrap();
        let legacy_dir = tempfile::tempdir().unwrap();

        // Write a minimal preferences.xml in legacy_dir
        let xml = r#"<preferences>
          <destDir>/from/xml</destDir>
          <moveEnabled>true</moveEnabled>
          <renameEnabled>true</renameEnabled>
          <ignoreKeywords><string>sample</string></ignoreKeywords>
        </preferences>"#;
        std::fs::write(legacy_dir.path().join("preferences.xml"), xml).unwrap();

        // Call the internal helper that takes explicit paths (not relying on ~/.tvrenamer)
        super::migrate_from_xml_paths(
            &legacy_dir.path().join("preferences.xml"),
            &config_dir.path().join("prefs.json"),
        ).unwrap();

        let loaded = crate::config::prefs::load(config_dir.path()).unwrap();
        assert_eq!(loaded.dest_dir, "/from/xml");
        assert!(loaded.move_selected);
    }

    #[test]
    fn migrate_overrides_from_xml_creates_overrides_json() {
        let config_dir = tempfile::tempdir().unwrap();
        let legacy_dir = tempfile::tempdir().unwrap();

        let xml = r#"<overrides>
          <showNames>
            <entry><string>Archer (2009)</string><string>Archer</string></entry>
          </showNames>
        </overrides>"#;
        std::fs::write(legacy_dir.path().join("overrides.xml"), xml).unwrap();

        super::migrate_overrides_from_xml_paths(
            &legacy_dir.path().join("overrides.xml"),
            &config_dir.path().join("overrides.json"),
        ).unwrap();

        let content = std::fs::read_to_string(config_dir.path().join("overrides.json")).unwrap();
        assert!(content.contains("Archer (2009)"));
        assert!(content.contains("\"to\": \"Archer\""));
    }
}
```

- [ ] **Step 2: Run — confirm compile failure**

```bash
cargo test --manifest-path src-tauri/Cargo.toml config::migration 2>&1
```

Expected: compile error — functions not yet defined.

- [ ] **Step 3: Implement the module**

Replace the contents of `src-tauri/src/config/migration.rs` with:

```rust
// XML → JSON migration for preferences.xml and overrides.xml (XStream format).
// Run at startup via try_migrate_preferences() and try_migrate_overrides().
// On first launch: check for prefs.json FIRST — skip if it already exists.
// If not found, check for ~/.tvrenamer/preferences.xml — migrate if present.
// The original XML file is left in place as a backup after migration.

use std::fs;
use std::path::{Path, PathBuf};

use quick_xml::Reader;
use quick_xml::events::Event;

use crate::config::prefs::UserPreferences;
use crate::errors::AppError;
use crate::overrides::ShowOverride;

/// Returns the legacy Java config directory: `~/.tvrenamer/`.
fn legacy_config_dir() -> Option<PathBuf> {
    directories::UserDirs::new()
        .map(|u| u.home_dir().join(".tvrenamer"))
}

/// Attempt XML → JSON migration for preferences.
///
/// - If `{config_dir}/prefs.json` exists: skip (already migrated or fresh JSON).
/// - If `~/.tvrenamer/preferences.xml` exists: parse and write `prefs.json`.
/// - Otherwise: no-op (first launch; defaults used at load time).
///
/// The original XML is left in place as a backup.
pub fn try_migrate_preferences(config_dir: &Path) -> Result<(), AppError> {
    let json_path = config_dir.join("prefs.json");
    if json_path.exists() {
        return Ok(());
    }
    let Some(legacy) = legacy_config_dir() else {
        return Ok(());
    };
    let xml_path = legacy.join("preferences.xml");
    if !xml_path.exists() {
        return Ok(());
    }
    migrate_from_xml_paths(&xml_path, &json_path)
}

/// Attempt XML → JSON migration for show overrides.
///
/// - If `{config_dir}/overrides.json` exists: skip.
/// - If `~/.tvrenamer/overrides.xml` exists: parse and write `overrides.json`.
/// - Otherwise: write bundled defaults to `overrides.json`.
pub fn try_migrate_overrides(config_dir: &Path) -> Result<(), AppError> {
    let json_path = config_dir.join("overrides.json");
    if json_path.exists() {
        return Ok(());
    }
    let overrides = if let Some(legacy) = legacy_config_dir() {
        let xml_path = legacy.join("overrides.xml");
        if xml_path.exists() {
            let content = fs::read_to_string(&xml_path)
                .map_err(|_| AppError::PreferencesCorrupted)?;
            parse_overrides_xml(&content)?
        } else {
            crate::overrides::bundled_defaults()
        }
    } else {
        crate::overrides::bundled_defaults()
    };
    write_overrides_json(&overrides, &json_path)
}

/// Internal: parse preferences XML at `xml_path`, write JSON to `json_path`.
/// Exposed for testing (allows injecting paths instead of relying on ~/.tvrenamer).
pub fn migrate_from_xml_paths(xml_path: &Path, json_path: &Path) -> Result<(), AppError> {
    let content = fs::read_to_string(xml_path)
        .map_err(|_| AppError::PreferencesCorrupted)?;
    let prefs = parse_preferences_xml(&content)?;
    if let Some(parent) = json_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| AppError::PermissionDenied(e.to_string()))?;
    }
    let json = serde_json::to_string_pretty(&prefs)
        .expect("UserPreferences always serializes");
    fs::write(json_path, json)
        .map_err(|e| AppError::PermissionDenied(e.to_string()))
}

/// Internal: parse overrides XML at `xml_path`, write JSON to `json_path`.
pub fn migrate_overrides_from_xml_paths(xml_path: &Path, json_path: &Path) -> Result<(), AppError> {
    let content = fs::read_to_string(xml_path)
        .map_err(|_| AppError::PreferencesCorrupted)?;
    let overrides = parse_overrides_xml(&content)?;
    write_overrides_json(&overrides, json_path)
}

fn write_overrides_json(overrides: &[ShowOverride], json_path: &Path) -> Result<(), AppError> {
    if let Some(parent) = json_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| AppError::PermissionDenied(e.to_string()))?;
    }
    let json = serde_json::to_string_pretty(overrides)
        .expect("Vec<ShowOverride> always serializes");
    fs::write(json_path, json)
        .map_err(|e| AppError::PermissionDenied(e.to_string()))
}

/// Parse XStream-format `preferences.xml` into `UserPreferences`.
///
/// Field aliases (set in `UserPreferencesPersistence.java`):
/// - `<moveEnabled>` → `move_selected`
/// - `<renameEnabled>` → `rename_selected`
///
/// Absent fields keep their `Default` values.
/// `<ignoreKeywords/>` (self-closing) clears the default keyword list.
/// `<ignoreKeywords><string>…</string></ignoreKeywords>` replaces the list.
pub fn parse_preferences_xml(content: &str) -> Result<UserPreferences, AppError> {
    let mut reader = Reader::from_str(content);
    reader.config_mut().trim_text(true);

    let mut prefs = UserPreferences::default();
    let mut current_field = String::new();
    let mut in_ignore_keywords = false;
    let mut buf = Vec::new();

    loop {
        buf.clear();
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(e)) => {
                let name = String::from_utf8_lossy(e.name().as_ref()).into_owned();
                if name == "ignoreKeywords" {
                    in_ignore_keywords = true;
                    // Clear defaults — we will replace with whatever the XML contains
                    prefs.ignore_keywords.clear();
                }
                current_field = name;
            }
            Ok(Event::Empty(e)) => {
                // Self-closing elements, e.g. <preloadFolder/> or <ignoreKeywords/>
                let name = String::from_utf8_lossy(e.name().as_ref());
                match name.as_ref() {
                    "preloadFolder" => prefs.preload_folder = None,
                    "ignoreKeywords" => prefs.ignore_keywords.clear(),
                    _ => {}
                }
            }
            Ok(Event::End(e)) => {
                let name = String::from_utf8_lossy(e.name().as_ref());
                if name == "ignoreKeywords" {
                    in_ignore_keywords = false;
                }
            }
            Ok(Event::Text(e)) => {
                let text = e
                    .unescape()
                    .map_err(|_| AppError::PreferencesCorrupted)?
                    .into_owned();
                if in_ignore_keywords && current_field == "string" {
                    prefs.ignore_keywords.push(text);
                } else {
                    match current_field.as_str() {
                        "destDir" => prefs.dest_dir = text,
                        "preloadFolder" => prefs.preload_folder = Some(text),
                        "seasonPrefix" => prefs.season_prefix = text,
                        "seasonPrefixLeadingZero" => {
                            prefs.season_prefix_leading_zero = text == "true"
                        }
                        // XStream aliases: moveEnabled → move_selected, renameEnabled → rename_selected
                        "moveEnabled" => prefs.move_selected = text == "true",
                        "renameEnabled" => prefs.rename_selected = text == "true",
                        "removeEmptiedDirectories" => {
                            prefs.remove_emptied_directories = text == "true"
                        }
                        "deleteRowAfterMove" => prefs.delete_row_after_move = text == "true",
                        "renameReplacementMask" => prefs.rename_replacement_mask = text,
                        "checkForUpdates" => prefs.check_for_updates = text == "true",
                        "recursivelyAddFolders" => prefs.recursively_add_folders = text == "true",
                        _ => {}
                    }
                }
            }
            Ok(Event::Eof) => break,
            Err(_) => return Err(AppError::PreferencesCorrupted),
            _ => {}
        }
    }

    Ok(prefs)
}

/// Parse XStream-format `overrides.xml` into `Vec<ShowOverride>`.
///
/// Format: `<overrides><showNames><entry><string>from</string><string>to</string></entry>…`
pub fn parse_overrides_xml(content: &str) -> Result<Vec<ShowOverride>, AppError> {
    let mut reader = Reader::from_str(content);
    reader.config_mut().trim_text(true);

    let mut overrides = Vec::new();
    let mut in_entry = false;
    let mut entry_strings: Vec<String> = Vec::new();
    let mut in_string = false;
    let mut buf = Vec::new();

    loop {
        buf.clear();
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(e)) => {
                let name = String::from_utf8_lossy(e.name().as_ref());
                match name.as_ref() {
                    "entry" => {
                        in_entry = true;
                        entry_strings.clear();
                    }
                    "string" if in_entry => {
                        in_string = true;
                    }
                    _ => {}
                }
            }
            Ok(Event::End(e)) => {
                let name = String::from_utf8_lossy(e.name().as_ref());
                match name.as_ref() {
                    "entry" => {
                        if entry_strings.len() == 2 {
                            overrides.push(ShowOverride {
                                from: entry_strings[0].clone(),
                                to: entry_strings[1].clone(),
                            });
                        }
                        in_entry = false;
                    }
                    "string" => {
                        in_string = false;
                    }
                    _ => {}
                }
            }
            Ok(Event::Text(e)) => {
                if in_string && in_entry {
                    let text = e
                        .unescape()
                        .map_err(|_| AppError::PreferencesCorrupted)?
                        .into_owned();
                    entry_strings.push(text);
                }
            }
            Ok(Event::Eof) => break,
            Err(_) => return Err(AppError::PreferencesCorrupted),
            _ => {}
        }
    }

    Ok(overrides)
}

#[cfg(test)]
mod tests {
    use super::{parse_preferences_xml, parse_overrides_xml};

    #[test]
    fn parse_preferences_xml_full_with_aliases() {
        let xml = r#"<preferences>
          <destDir>/mnt/tv</destDir>
          <seasonPrefix>S</seasonPrefix>
          <seasonPrefixLeadingZero>true</seasonPrefixLeadingZero>
          <moveEnabled>true</moveEnabled>
          <renameEnabled>false</renameEnabled>
          <removeEmptiedDirectories>false</removeEmptiedDirectories>
          <deleteRowAfterMove>true</deleteRowAfterMove>
          <renameReplacementMask>%S %e %t</renameReplacementMask>
          <checkForUpdates>false</checkForUpdates>
          <recursivelyAddFolders>false</recursivelyAddFolders>
          <ignoreKeywords><string>sample</string><string>extras</string></ignoreKeywords>
        </preferences>"#;

        let prefs = parse_preferences_xml(xml).unwrap();
        assert_eq!(prefs.dest_dir, "/mnt/tv");
        assert_eq!(prefs.season_prefix, "S");
        assert!(prefs.season_prefix_leading_zero);
        assert!(prefs.move_selected);
        assert!(!prefs.rename_selected);
        assert!(!prefs.remove_emptied_directories);
        assert!(prefs.delete_row_after_move);
        assert_eq!(prefs.rename_replacement_mask, "%S %e %t");
        assert!(!prefs.check_for_updates);
        assert!(!prefs.recursively_add_folders);
        assert_eq!(prefs.ignore_keywords, vec!["sample", "extras"]);
    }

    #[test]
    fn parse_preferences_xml_absent_fields_use_defaults() {
        let xml = "<preferences><destDir>/mnt/movies</destDir></preferences>";
        let prefs = parse_preferences_xml(xml).unwrap();
        assert_eq!(prefs.dest_dir, "/mnt/movies");
        assert!(prefs.rename_selected);
        assert!(!prefs.move_selected);
        assert_eq!(prefs.ignore_keywords, vec!["sample"]);
    }

    #[test]
    fn parse_preferences_xml_empty_ignore_keywords_element() {
        let xml = "<preferences><ignoreKeywords/></preferences>";
        let prefs = parse_preferences_xml(xml).unwrap();
        assert!(prefs.ignore_keywords.is_empty());
    }

    #[test]
    fn parse_overrides_xml_three_entries() {
        let xml = r#"<overrides>
          <showNames>
            <entry><string>House of Cards (2013)</string><string>House of Cards</string></entry>
            <entry><string>Archer (2009)</string><string>Archer</string></entry>
            <entry><string>The Newsroom (2012)</string><string>The Newsroom</string></entry>
          </showNames>
        </overrides>"#;

        let overrides = parse_overrides_xml(xml).unwrap();
        assert_eq!(overrides.len(), 3);
        assert_eq!(overrides[0].from, "House of Cards (2013)");
        assert_eq!(overrides[0].to, "House of Cards");
        assert_eq!(overrides[1].from, "Archer (2009)");
        assert_eq!(overrides[1].to, "Archer");
    }

    #[test]
    fn parse_overrides_xml_empty_show_names() {
        let xml = "<overrides><showNames/></overrides>";
        let overrides = parse_overrides_xml(xml).unwrap();
        assert!(overrides.is_empty());
    }

    #[test]
    fn migrate_preferences_skips_if_prefs_json_exists() {
        let config_dir = tempfile::tempdir().unwrap();
        let existing = r#"{"version":1,"preload_folder":null,"dest_dir":"/existing","season_prefix":"Season ","season_prefix_leading_zero":false,"move_selected":false,"rename_selected":true,"remove_emptied_directories":true,"delete_row_after_move":false,"rename_replacement_mask":"%S [%sx%0e] %t","check_for_updates":true,"recursively_add_folders":true,"ignore_keywords":["sample"]}"#;
        std::fs::write(config_dir.path().join("prefs.json"), existing).unwrap();

        super::try_migrate_preferences(config_dir.path()).unwrap();

        let loaded = crate::config::prefs::load(config_dir.path()).unwrap();
        assert_eq!(loaded.dest_dir, "/existing");
    }

    #[test]
    fn migrate_from_xml_paths_creates_prefs_json() {
        let config_dir = tempfile::tempdir().unwrap();
        let xml_dir = tempfile::tempdir().unwrap();

        let xml = r#"<preferences>
          <destDir>/from/xml</destDir>
          <moveEnabled>true</moveEnabled>
          <renameEnabled>true</renameEnabled>
          <ignoreKeywords><string>sample</string></ignoreKeywords>
        </preferences>"#;
        let xml_path = xml_dir.path().join("preferences.xml");
        let json_path = config_dir.path().join("prefs.json");
        std::fs::write(&xml_path, xml).unwrap();

        super::migrate_from_xml_paths(&xml_path, &json_path).unwrap();

        let loaded = crate::config::prefs::load(config_dir.path()).unwrap();
        assert_eq!(loaded.dest_dir, "/from/xml");
        assert!(loaded.move_selected);
    }

    #[test]
    fn migrate_overrides_from_xml_paths_creates_overrides_json() {
        let config_dir = tempfile::tempdir().unwrap();
        let xml_dir = tempfile::tempdir().unwrap();

        let xml = r#"<overrides>
          <showNames>
            <entry><string>Archer (2009)</string><string>Archer</string></entry>
          </showNames>
        </overrides>"#;
        let xml_path = xml_dir.path().join("overrides.xml");
        let json_path = config_dir.path().join("overrides.json");
        std::fs::write(&xml_path, xml).unwrap();

        super::migrate_overrides_from_xml_paths(&xml_path, &json_path).unwrap();

        let content = std::fs::read_to_string(&json_path).unwrap();
        assert!(content.contains("Archer (2009)"));
        assert!(content.contains("\"to\": \"Archer\""));
    }
}
```

- [ ] **Step 4: Run migration tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml config::migration 2>&1
```

Expected: all 9 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/config/migration.rs
git commit -m "feat(config): implement XStream XML migration with quick-xml event parser"
```

---

### Task 5: Add `PrefsState` and `OverridesState` to `state.rs`

Keep `AppState` unchanged (avoids breaking its test). Add two new structs as separate Tauri managed state types.

**Files:**
- Modify: `src-tauri/src/state.rs`

- [ ] **Step 1: Write the failing tests first**

Add to the existing `mod tests` block in `state.rs`:

```rust
    #[test]
    fn prefs_state_constructs_and_locks() {
        let dir = tempfile::tempdir().unwrap();
        let state = super::PrefsState::new(
            dir.path().to_path_buf(),
            crate::config::prefs::UserPreferences::default(),
        );
        let prefs = state.prefs.lock().unwrap();
        assert_eq!(prefs.dest_dir, "~/TV");
    }

    #[test]
    fn overrides_state_constructs() {
        let state = super::OverridesState::new(crate::overrides::bundled_defaults());
        assert_eq!(state.overrides.len(), 3);
    }
```

- [ ] **Step 2: Run — confirm compile failure**

```bash
cargo test --manifest-path src-tauri/Cargo.toml state 2>&1
```

Expected: compile error — `PrefsState` and `OverridesState` not defined.

- [ ] **Step 3: Add structs to `state.rs`**

Add after the existing `impl AppState` block, before `#[cfg(test)]`:

```rust
/// Managed state for user preferences.
/// Initialized in the Tauri setup hook from `app.path().app_config_dir()`.
pub struct PrefsState {
    pub config_dir: std::path::PathBuf,
    pub prefs: std::sync::Mutex<crate::config::prefs::UserPreferences>,
}

impl PrefsState {
    pub fn new(config_dir: std::path::PathBuf, prefs: crate::config::prefs::UserPreferences) -> Self {
        Self {
            config_dir,
            prefs: std::sync::Mutex::new(prefs),
        }
    }
}

/// Managed state for show name overrides.
/// Loaded at startup; overrides do not change during a session.
pub struct OverridesState {
    pub overrides: Vec<crate::overrides::ShowOverride>,
}

impl OverridesState {
    pub fn new(overrides: Vec<crate::overrides::ShowOverride>) -> Self {
        Self { overrides }
    }
}
```

- [ ] **Step 4: Run tests — confirm they pass**

```bash
cargo test --manifest-path src-tauri/Cargo.toml state 2>&1
```

Expected:
```
test state::tests::app_state_constructs ... ok
test state::tests::overrides_state_constructs ... ok
test state::tests::prefs_state_constructs_and_locks ... ok

test result: ok. 3 passed; 0 failed
```

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/state.rs
git commit -m "feat(state): add PrefsState and OverridesState for Tauri managed state"
```

---

### Task 6: Add `get_preferences` and `save_preferences` IPC commands, wire overrides into `search_shows`

**Files:**
- Modify: `src-tauri/src/ipc.rs`

- [ ] **Step 1: Write the failing tests first**

Add to the existing `mod tests` block in `ipc.rs`:

```rust
    #[test]
    fn user_preferences_serializes_for_ipc() {
        // IPC commands return UserPreferences as JSON — must serialize without error
        let prefs = crate::config::prefs::UserPreferences::default();
        let json = serde_json::to_string(&prefs).unwrap();
        let back: crate::config::prefs::UserPreferences = serde_json::from_str(&json).unwrap();
        assert_eq!(back.dest_dir, prefs.dest_dir);
        assert_eq!(back.version, 1);
    }

    #[test]
    fn user_preferences_deserializes_from_frontend_json() {
        // The frontend sends UserPreferences as JSON to save_preferences
        let json = r#"{
            "version": 1,
            "preload_folder": null,
            "dest_dir": "~/TV",
            "season_prefix": "Season ",
            "season_prefix_leading_zero": false,
            "move_selected": false,
            "rename_selected": true,
            "remove_emptied_directories": true,
            "delete_row_after_move": false,
            "rename_replacement_mask": "%S [%sx%0e] %t",
            "check_for_updates": true,
            "recursively_add_folders": true,
            "ignore_keywords": ["sample"]
        }"#;
        let prefs: crate::config::prefs::UserPreferences = serde_json::from_str(json).unwrap();
        assert_eq!(prefs.dest_dir, "~/TV");
        assert_eq!(prefs.ignore_keywords, vec!["sample"]);
    }
```

- [ ] **Step 2: Run — confirm tests pass (they don't require new functions)**

```bash
cargo test --manifest-path src-tauri/Cargo.toml ipc 2>&1
```

Expected: new tests pass immediately (they only test serde, not new commands).

- [ ] **Step 3: Add new imports and commands to `ipc.rs`**

Add to the existing `use` block at the top of `ipc.rs`:

```rust
use crate::config::prefs::UserPreferences;
use crate::state::{PrefsState, OverridesState};
use crate::overrides;
```

Add these two commands after `perform_renames`, before `#[cfg(test)]`:

```rust
/// Return the current user preferences.
/// Called by the preferences dialog on open.
#[tauri::command]
pub async fn get_preferences(
    prefs_state: State<'_, PrefsState>,
) -> Result<UserPreferences, String> {
    let prefs = prefs_state.prefs.lock().map_err(|e| e.to_string())?;
    Ok(prefs.clone())
}

/// Persist updated preferences to disk and update in-memory state.
/// Called by the preferences dialog on save.
#[tauri::command]
pub async fn save_preferences(
    new_prefs: UserPreferences,
    prefs_state: State<'_, PrefsState>,
) -> Result<(), String> {
    let config_dir = prefs_state.config_dir.clone();
    crate::config::prefs::save(&new_prefs, &config_dir).map_err(|e| e.to_string())?;
    let mut prefs = prefs_state.prefs.lock().map_err(|e| e.to_string())?;
    *prefs = new_prefs;
    Ok(())
}
```

- [ ] **Step 4: Wire overrides into `search_shows`**

Replace the existing `search_shows` function signature and body:

```rust
/// Search TMDB for TV series matching `query`.
/// Applies show name overrides BEFORE the TMDB query (fixes orphaned GlobalOverrides bug).
/// Reads the API key from the OS keychain on every call.
#[tauri::command]
pub async fn search_shows(
    query: String,
    state: State<'_, AppState>,
    overrides_state: State<'_, OverridesState>,
) -> Result<Vec<Series>, String> {
    let effective_query = overrides::apply_override(&query, &overrides_state.overrides);
    let api_key = keychain::read_api_key().map_err(|e| e.to_string())?;
    TmdbProvider::new(state.http_client.clone(), api_key)
        .search_series(&effective_query)
        .await
        .map_err(|e| e.to_string())
}
```

- [ ] **Step 5: Run all ipc tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml ipc 2>&1
```

Expected:
```
test ipc::tests::rename_request_deserializes ... ok
test ipc::tests::rename_outcome_status_serializes ... ok
test ipc::tests::user_preferences_deserializes_from_frontend_json ... ok
test ipc::tests::user_preferences_serializes_for_ipc ... ok

test result: ok. 4 passed; 0 failed
```

- [ ] **Step 6: Commit**

```bash
git add src-tauri/src/ipc.rs
git commit -m "feat(ipc): add get_preferences + save_preferences commands; wire overrides into search_shows"
```

---

### Task 7: Wire setup hook in `lib.rs` and register new commands

**Files:**
- Modify: `src-tauri/src/lib.rs`

- [ ] **Step 1: Rewrite `lib.rs` with setup hook**

Replace the contents of `src-tauri/src/lib.rs` with:

```rust
mod config;
mod errors;
mod ipc;
mod metadata;
mod overrides;
mod parser;
mod renamer;
mod state;

use state::{AppState, OverridesState, PrefsState};

pub fn run() {
    tracing_subscriber::fmt::init();

    let app_state = AppState::new().expect("Failed to initialise AppState");

    tauri::Builder::default()
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::default().build())
        .manage(app_state)
        .setup(|app| {
            let config_dir = app
                .path()
                .app_config_dir()
                .map_err(|e| Box::new(e) as Box<dyn std::error::Error>)?;

            std::fs::create_dir_all(&config_dir)?;

            // XML → JSON migration (silent on failure — app still works with defaults)
            if let Err(e) = config::migration::try_migrate_preferences(&config_dir) {
                tracing::warn!("Preferences migration failed, using defaults: {e}");
            }
            if let Err(e) = config::migration::try_migrate_overrides(&config_dir) {
                tracing::warn!("Overrides migration failed, using bundled defaults: {e}");
            }

            let prefs = config::prefs::load(&config_dir).unwrap_or_default();
            let override_list = overrides::load(&config_dir);

            app.manage(PrefsState::new(config_dir, prefs));
            app.manage(OverridesState::new(override_list));

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            ipc::ping,
            ipc::search_shows,
            ipc::lookup_episode,
            ipc::validate_tmdb_key,
            ipc::save_tmdb_key,
            ipc::perform_renames,
            ipc::get_preferences,
            ipc::save_preferences,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

- [ ] **Step 2: Run cargo check**

```bash
cargo check --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected: no errors. If `app.path().app_config_dir()` doesn't exist, Tauri v2's path API may use `app.path_resolver().app_config_dir()` — check docs and adjust.

If the method is `path_resolver()`:
```rust
let config_dir = app.path_resolver().app_config_dir()
    .ok_or("Could not resolve app config dir")?;
```

- [ ] **Step 3: Run all unit tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected:
```
test config::migration::tests::migrate_from_xml_paths_creates_prefs_json ... ok
test config::migration::tests::migrate_overrides_from_xml_paths_creates_overrides_json ... ok
test config::migration::tests::migrate_preferences_skips_if_prefs_json_exists ... ok
test config::migration::tests::parse_overrides_xml_empty_show_names ... ok
test config::migration::tests::parse_overrides_xml_three_entries ... ok
test config::migration::tests::parse_preferences_xml_absent_fields_use_defaults ... ok
test config::migration::tests::parse_preferences_xml_empty_ignore_keywords_element ... ok
test config::migration::tests::parse_preferences_xml_full_with_aliases ... ok
test config::prefs::tests::default_prefs_serialize_round_trip ... ok
test config::prefs::tests::load_corrupted_returns_err ... ok
test config::prefs::tests::load_missing_returns_defaults ... ok
test config::prefs::tests::save_and_load_roundtrip ... ok
test config::prefs::tests::saved_json_contains_version_1 ... ok
test errors::tests::all_error_variants_serialize ... ok
test errors::tests::new_error_variants_serialize ... ok
test ipc::tests::rename_outcome_status_serializes ... ok
test ipc::tests::rename_request_deserializes ... ok
test ipc::tests::user_preferences_deserializes_from_frontend_json ... ok
test ipc::tests::user_preferences_serializes_for_ipc ... ok
test overrides::tests::apply_override_known_name ... ok
test overrides::tests::apply_override_unknown_name_passthrough ... ok
test overrides::tests::bundled_defaults_has_three_entries ... ok
test overrides::tests::load_corrupted_json_returns_bundled_defaults ... ok
test overrides::tests::load_from_json_file ... ok
test overrides::tests::load_missing_returns_bundled_defaults ... ok
test state::tests::app_state_constructs ... ok
test state::tests::overrides_state_constructs ... ok
test state::tests::prefs_state_constructs_and_locks ... ok

test result: ok. 28 passed; 0 failed
```

- [ ] **Step 4: Commit**

```bash
git add src-tauri/src/lib.rs
git commit -m "feat(lib): add setup hook for migration, prefs load, overrides; register all IPC commands"
```

---

## Validated Assumptions

*Validated by assumption-checker agent (11 ✅, 0 ❌, 1 ⚠️ corrected in plan)*

### ✅ Validated

- `directories::UserDirs::new()` returns `Option<UserDirs>`; `.home_dir()` returns `&Path` — confirmed against crate docs
- XStream aliases `moveEnabled`/`renameEnabled` confirmed in `UserPreferencesPersistence.java` lines 26–27
- `tempfile = "3.10"` already in `[dev-dependencies]` in `Cargo.toml`
- `serde_json::to_string_pretty` available (serde_json = "1" present)
- `quick_xml::Reader::from_str()` + `read_event_into()` is correct API for quick-xml 0.36+
- `Event::Empty` exists in quick-xml for self-closing elements (e.g. `<ignoreKeywords/>`)
- `State<'_, PrefsState>` as Tauri command parameter is valid v2 syntax (codebase already uses `State<'_, AppState>`)
- Multiple `State<...>` parameters in one command are supported in Tauri v2
- `app.manage()` can be called inside the setup hook
- `UserPreferences` derives `Clone` — confirmed in `prefs.rs:6`
- `app.path().app_config_dir()` returns `Result<PathBuf>` — correct return type; error handling with `?` is correct

### ⚠️ Corrected in Plan

- **`quick-xml` version:** Latest stable is 0.39.2 (as of 2026-02), not 0.37. Plan updated to use `"0.39"` throughout.

---

## Open Questions Carried Forward

1. **`app.path().app_config_dir()` exact API:** Tauri v2 may use `app.path_resolver().app_config_dir()` — verify against installed `tauri = "2"` version. Adjust `lib.rs` Task 7 Step 1 accordingly.

2. **`quick-xml` version:** Latest stable is 0.39.2 (as of 2026-02). Plan uses `"0.39"`. Run `cargo add quick-xml` to confirm the current version before pinning manually.

3. **`keyring` on headless Linux:** Not addressed in this plan. If running in CI, `AppState::new()` (via `keychain` tests) may fail. A future task should add a fallback for headless environments.

4. **Preferences dialog (frontend):** `get_preferences` and `save_preferences` IPC commands are ready. The React preferences dialog that calls them is out of scope for this plan — that belongs in the UI plan.

5. **`preloadFolder` field:** Marked `Option<String>`. The Java source shows it is always `null` at default. If users had set a preload folder in Java, it will be absent from the XStream XML (null fields are omitted) — parsed correctly as `None`.
