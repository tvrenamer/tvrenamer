// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// IPC error convention: commands return Result<T, String> (AppError serialized via Display trait).

/// Smoke-test command — verifies the IPC bridge is operational.
/// Remove or replace once the first real command is implemented.
#[tauri::command]
pub async fn ping() -> Result<String, String> {
    Ok("pong".to_string())
}
