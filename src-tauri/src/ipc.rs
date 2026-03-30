// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// Error convention: Result<T, String> — AppError serialized via Display.

use tauri::State;

use crate::config::keychain;
use crate::metadata::models::{Episode, Series};
use crate::metadata::provider::MetadataProvider;
use crate::metadata::tmdb::TmdbProvider;
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
