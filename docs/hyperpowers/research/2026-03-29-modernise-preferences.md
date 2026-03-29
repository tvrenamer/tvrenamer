# Research: Preferences, Migration & Show Overrides (Rust Port)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Port user preferences and show overrides from Java/XML to Rust/JSON. Implement first-launch migration from existing XML files. Wire the orphaned GlobalOverrides feature into the actual lookup flow.

---

## UserPreferences Schema (12 Fields)

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

---

## Show Overrides

### Current XML Format

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

### Target JSON Format

```json
[
  {"from": "Archer (2009)", "to": "Archer"},
  {"from": "House of Cards (2013)", "to": "House of Cards"},
  {"from": "The Newsroom (2012)", "to": "The Newsroom"}
]
```

Maps year-disambiguated provider names to simplified names. The `getShowName()` method returns the original if no override exists.

### Critical Finding: Overrides Are Orphaned

`GlobalOverrides.getShowName()` exists and loads the XML file, but **is never called anywhere in the production codebase**. The Tauri port **must** wire this into the lookup flow: apply immediately after filename parser output, before provider query. This is a bug fix, not a new feature.

---

## Preferences Migration (XML → JSON)

On first launch, the Rust startup routine checks for `~/.tvrenamer/preferences.xml`. If found, reads and migrates all settings to `~/.tvrenamer/prefs.json`. The XML file is left in place as a backup.

Similarly, `~/.tvrenamer/overrides.xml` is migrated to a JSON equivalent.

### Migration Edge Cases

1. **Both files exist**: Skip migration if `prefs.json` already exists (migration already ran). Check existence first.
2. **XML corrupted**: Java returns `null` and silently uses defaults. Rust should use a distinct error variant, log a warning, then proceed with defaults.
3. **First launch on Windows**: Config path is `%APPDATA%\tvrenamer\`, not `~/.tvrenamer/`. The Tauri capabilities glob `$HOME/.tvrenamer/**` may not match on Windows — verify or use `$APPDATA/.tvrenamer/**`.

---

## Config Paths: `directories` Crate

```toml
directories = "6.0"
```

```rust
let proj_dirs = ProjectDirs::from("", "", "tvrenamer")?;
let config_path = proj_dirs.config_dir();
// macOS: ~/Library/Application Support/tvrenamer
// Windows: %APPDATA%\tvrenamer
// Linux: ~/.config/tvrenamer/
```

**Decision needed:** The design doc uses `~/.tvrenamer/` directly. The `directories` crate gives platform-correct paths. Options:
- Adopt platform-correct paths (cleaner, follows OS conventions)
- Stay with `~/.tvrenamer/` (existing users have data there)
- Use `directories` crate paths but check `~/.tvrenamer/` as migration source

---

## Config Versioning

```toml
serde_flow = "0.3"
```

Add a `version` field to `prefs.json` from v1.0. Use `#[flow(variant = 2)]` + `From<PrefsV1>` for automatic migration. Alternative: `#[serde(default)]` for optional new fields with a manual `version` discriminator.

**Note:** `serde_flow = "0.3"` exact annotation syntax (`#[flow(variant = N)]`) is unverified — test before adopting.

---

## Rust Module Design

```rust
// src-tauri/src/config/prefs.rs
#[derive(Serialize, Deserialize)]
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

// src-tauri/src/config/migration.rs
pub fn migrate_xml_preferences(xml_path: &Path, json_path: &Path) -> Result<(), AppError> { ... }

// src-tauri/src/overrides/mod.rs
pub fn apply_override(show_name: &str, overrides: &[(String, String)]) -> String { ... }
```

---

## Architecture Note

In Java, `UserPreferences` is a singleton accessed via `getInstance()` everywhere (including `FileMover` at line 20). The Rust port must pass preferences explicitly via function parameters or `AppHandle` state — no global mutable singleton.

---

## Test Coverage Gaps (Write From Scratch)

- **XML → JSON migration** — no tests exist in Java
- **Corrupted XML handling** — no tests
- **Overrides application** — never tested because never called

---

## Open Questions

1. **`directories` crate vs `~/.tvrenamer/`**: Adopt platform-correct paths, or stay with existing convention?
2. **XStream XML migration**: Java's XStream XML format has unusual structure for nested objects. Consider shipping migration as a one-time utility or using a simple hand-written XML parser rather than re-implementing XStream parsing.
3. **`keyring` on headless Linux**: Fallback strategy needed when Secret Service daemon is unavailable.
