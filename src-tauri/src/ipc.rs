// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// Error convention: Result<T, String> — AppError serialized via Display.

use std::path::PathBuf;

use tauri::Emitter;
use tauri::State;

use crate::config::keychain;
use crate::metadata::models::{Episode, Series};
use crate::metadata::provider::MetadataProvider as _;
use crate::metadata::tmdb::TmdbProvider;
use crate::renamer::conflict::{resolve_conflicts, PendingMove};
use crate::renamer::mover::{move_file, MoveStatus};
use crate::state::AppState;

/// Smoke-test command — verifies the IPC bridge is operational.
#[tauri::command]
pub async fn ping() -> Result<String, String> {
    Ok("pong".to_string())
}

/// Search TMDB for TV series matching `query`.
/// Reads the API key from the OS keychain on every call.
/// Returns `Err("Unable to find show information")` if no results.
/// Returns `Err("API key invalid or missing")` if no key saved yet.
#[tauri::command]
pub async fn search_shows(
    query: String,
    state: State<'_, AppState>,
) -> Result<Vec<Series>, String> {
    let api_key = keychain::read_api_key().map_err(|e| e.to_string())?;
    TmdbProvider::new(state.http_client.clone(), api_key)
        .search_series(&query)
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
}
