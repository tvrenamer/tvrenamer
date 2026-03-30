// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// Error convention: Result<T, String> — AppError serialized via Display.

use std::path::PathBuf;

use tauri::Emitter;
use tauri::State;

use crate::config::keychain;
use crate::config::prefs::UserPreferences;
use crate::metadata::models::{Episode, Series};
use crate::metadata::provider::MetadataProvider as _;
use crate::metadata::tmdb::TmdbProvider;
use crate::overrides;
use crate::renamer::conflict::{resolve_conflicts, PendingMove};
use crate::renamer::mover::{move_file, MoveStatus};
use crate::state::{AppState, OverridesState, PrefsState};

/// Smoke-test command — verifies the IPC bridge is operational.
#[tauri::command]
pub async fn ping() -> Result<String, String> {
    Ok("pong".to_string())
}

/// Parse a batch of file paths using the Rust filename parser.
/// Returns None for paths that no pattern could match.
/// Call this after `tauri://drag-drop` to extract show/season/episode from filenames.
#[tauri::command]
pub async fn parse_files(paths: Vec<String>) -> Vec<Option<crate::parser::ParseResult>> {
    paths.iter().map(|p| crate::parser::parse_filename(p)).collect()
}

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

/// Fetch a specific episode from TMDB by series ID, season number, and episode number.
/// Returns `Err("Could not get episode for show")` if the episode doesn't exist on TMDB.
#[tauri::command]
pub async fn lookup_episode(
    series_id: u32,
    season: u32,
    episode: u32,
    state: State<'_, AppState>,
) -> Result<Episode, String> {
    let api_key = keychain::read_api_key().map_err(|e| e.to_string())?;
    TmdbProvider::new(state.http_client.clone(), api_key)
        .get_episode(series_id, season, episode)
        .await
        .map_err(|e| e.to_string())
}

/// Validate a TMDB API key without saving it.
/// Called by the onboarding modal's "Test" button.
/// Returns Ok(()) if the key is accepted by TMDB /3/authentication.
#[tauri::command]
pub async fn validate_tmdb_key(
    key: String,
    state: State<'_, AppState>,
) -> Result<(), String> {
    TmdbProvider::validate_key(&state.http_client, &key, "https://api.themoviedb.org")
        .await
        .map_err(|e| e.to_string())
}

/// Save a TMDB API key to the OS keychain.
/// Call only after `validate_tmdb_key` returns Ok.
#[tauri::command]
pub async fn save_tmdb_key(key: String) -> Result<(), String> {
    keychain::save_api_key(&key).map_err(|e| e.to_string())
}

/// Input from frontend: a single (source, dest) rename pair.
#[derive(serde::Deserialize)]
pub struct RenameRequest {
    pub source: String,
    pub dest: String,
}

/// Per-file rename outcome status for frontend consumption.
#[derive(serde::Serialize, PartialEq, Debug)]
#[serde(rename_all = "snake_case")]
pub enum RenameOutcomeStatus {
    Success,
    AlreadyInPlace,
    FailToMove,
}

/// Full outcome of a single rename, including the resolved destination path.
#[derive(serde::Serialize)]
pub struct RenameOutcome {
    pub source: String,
    /// Actual destination used (may differ from requested dest if conflict-resolved).
    pub dest: String,
    pub status: RenameOutcomeStatus,
    /// Populated when status is FailToMove.
    pub error: Option<String>,
}

/// Execute a batch of file renames.
///
/// 1. Reads source sizes to build PendingMove list.
/// 2. Runs conflict pre-scan (mutates dest paths for conflicts).
/// 3. Moves each file; emits `rename-progress` event after each.
/// 4. Returns all outcomes.
#[tauri::command]
pub async fn perform_renames(
    renames: Vec<RenameRequest>,
    app: tauri::AppHandle,
) -> Result<Vec<RenameOutcome>, String> {
    // Build PendingMove list with source sizes
    let mut pending: Vec<PendingMove> = renames
        .into_iter()
        .map(|r| {
            let source = PathBuf::from(&r.source);
            let source_size = std::fs::metadata(&source).map(|m| m.len()).unwrap_or(0);
            PendingMove {
                dest: PathBuf::from(r.dest),
                source,
                source_size,
            }
        })
        .collect();

    // Conflict pre-scan (mutates dest paths in-place)
    resolve_conflicts(&mut pending);

    // Execute moves
    let mut outcomes = Vec::with_capacity(pending.len());
    for pm in pending {
        let source = pm.source.clone();
        let dest = pm.dest.clone();

        let result = tokio::task::spawn_blocking({
            let src = source.clone();
            let dst = dest.clone();
            move || move_file(&src, &dst)
        })
        .await
        .map_err(|e| e.to_string())?;

        let outcome = match result {
            Ok(MoveStatus::Success) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::Success,
                error: None,
            },
            Ok(MoveStatus::AlreadyInPlace) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::AlreadyInPlace,
                error: None,
            },
            Ok(MoveStatus::FailToMove(msg)) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::FailToMove,
                error: Some(msg),
            },
            Err(e) => RenameOutcome {
                source: source.display().to_string(),
                dest: dest.display().to_string(),
                status: RenameOutcomeStatus::FailToMove,
                error: Some(e.to_string()),
            },
        };

        // Emit progress event (frontend listens on "rename-progress")
        let _ = app.emit("rename-progress", &outcome);

        outcomes.push(outcome);
    }

    Ok(outcomes)
}

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

#[cfg(test)]
mod tests {
    use super::{RenameOutcomeStatus, RenameRequest};

    #[test]
    fn rename_request_deserializes() {
        let json = r#"{"source":"/a/b.mkv","dest":"/c/d.mkv"}"#;
        let req: RenameRequest = serde_json::from_str(json).unwrap();
        assert_eq!(req.source, "/a/b.mkv");
        assert_eq!(req.dest, "/c/d.mkv");
    }

    #[test]
    fn rename_outcome_status_serializes() {
        assert_eq!(
            serde_json::to_string(&RenameOutcomeStatus::Success).unwrap(),
            r#""success""#
        );
        assert_eq!(
            serde_json::to_string(&RenameOutcomeStatus::AlreadyInPlace).unwrap(),
            r#""already_in_place""#
        );
    }

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
    fn parse_files_returns_serializable_result() {
        // ParseResult must implement Serialize for IPC — verify via serde_json
        let result = crate::parser::parse_filename("Fargo.S01E01.HDTV.x264-2HD.mp4");
        let json = serde_json::to_string(&result).expect("ParseResult must be serializable");
        assert!(json.contains("Fargo"), "show_name must be present: {json}");
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
}
