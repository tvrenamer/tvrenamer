mod config;
mod errors;
mod ipc;
mod metadata;
mod overrides;
mod parser;
mod renamer;
mod state;

use state::{AppState, OverridesState, PrefsState};
use tauri::Manager;

pub fn run() {
    tracing_subscriber::fmt::init();

    let app_state = AppState::new().expect("Failed to initialise AppState");

    tauri::Builder::default()
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::default().build())
        .manage(app_state)
        .setup(|app| {
            let config_dir = app
                .path()
                .app_config_dir()
                .map_err(|e| Box::new(e) as Box<dyn std::error::Error>)?;

            std::fs::create_dir_all(&config_dir)?;

            // XML → JSON migration (silent on failure — app still works with defaults)
            if let Err(e) = config::migration::try_migrate_preferences(&config_dir) {
                tracing::warn!("Preferences migration failed, using defaults: {e}");
            }
            if let Err(e) = config::migration::try_migrate_overrides(&config_dir) {
                tracing::warn!("Overrides migration failed, using bundled defaults: {e}");
            }

            let prefs = config::prefs::load(&config_dir).unwrap_or_default();
            let override_list = overrides::load(&config_dir);

            app.manage(PrefsState::new(config_dir, prefs));
            app.manage(OverridesState::new(override_list));

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            ipc::ping,
            ipc::parse_files,
            ipc::search_shows,
            ipc::lookup_episode,
            ipc::validate_tmdb_key,
            ipc::save_tmdb_key,
            ipc::perform_renames,
            ipc::get_preferences,
            ipc::save_preferences,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
