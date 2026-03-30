mod config;
mod errors;
mod ipc;
mod metadata;
mod overrides;
mod parser;
mod renamer;
mod state;

use state::AppState;

pub fn run() {
    tracing_subscriber::fmt::init();

    let state = AppState::new().expect("Failed to initialise AppState");

    tauri::Builder::default()
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        // updater plugin omitted until pubkey configured — run `npm run tauri signer generate` first
        .plugin(tauri_plugin_store::Builder::default().build())
        .manage(state)
        .invoke_handler(tauri::generate_handler![
            ipc::ping,
            ipc::search_shows,
            ipc::lookup_episode,
            ipc::validate_tmdb_key,
            ipc::save_tmdb_key,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
