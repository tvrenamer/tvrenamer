use serde::{Deserialize, Serialize};
use std::fs;
use std::path::Path;
use crate::errors::AppError;

fn default_prefs_version() -> u32 {
    1
}

/// UserPreferences — mirrors Java UserPreferences (12 fields).
/// Stored at ~/.tvrenamer/prefs.json (see open question re: directories crate).
/// TMDB API key is NOT stored here — it lives in the OS keychain via the keyring crate.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserPreferences {
    #[serde(default = "default_prefs_version")]
    pub version: u32,
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
            version: 1,
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
}
