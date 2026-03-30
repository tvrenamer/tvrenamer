// Show name overrides — ports GlobalOverrides.java.
// CRITICAL: GlobalOverrides.getShowName() existed in Java but was NEVER called in production.
// This module fixes that — apply_override() is called in ipc::search_shows BEFORE the TMDB query.
// Format: JSON array [{"from": "Archer (2009)", "to": "Archer"}]

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
