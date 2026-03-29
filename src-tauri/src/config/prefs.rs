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
