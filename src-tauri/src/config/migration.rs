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
                let name_bytes = e.name().as_ref().to_vec();
                let name = String::from_utf8_lossy(&name_bytes);
                match name.as_ref() {
                    "preloadFolder" => prefs.preload_folder = None,
                    "ignoreKeywords" => prefs.ignore_keywords.clear(),
                    _ => {}
                }
            }
            Ok(Event::End(e)) => {
                let name_bytes = e.name().as_ref().to_vec();
                let name = String::from_utf8_lossy(&name_bytes);
                if name == "ignoreKeywords" {
                    in_ignore_keywords = false;
                }
            }
            Ok(Event::Text(e)) => {
                let text = e
                    .decode()
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
                let name_bytes = e.name().as_ref().to_vec();
                let name = String::from_utf8_lossy(&name_bytes);
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
                let name_bytes = e.name().as_ref().to_vec();
                let name = String::from_utf8_lossy(&name_bytes);
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
                        .decode()
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
