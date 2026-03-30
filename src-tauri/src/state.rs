use std::time::Duration;
use crate::errors::AppError;

pub struct AppState {
    /// Shared HTTP client — one instance per process, connection pool reused across all TMDB calls.
    /// Never construct a new Client per request (destroys pooling; TMDB allows 20 concurrent connections).
    pub http_client: reqwest::Client,
}

impl AppState {
    pub fn new() -> Result<Self, AppError> {
        let http_client = reqwest::Client::builder()
            .connect_timeout(Duration::from_secs(10))
            .timeout(Duration::from_secs(30))
            .build()
            .map_err(|e| AppError::NetworkTimeout(e.to_string()))?;
        Ok(Self { http_client })
    }
}

/// Managed state for user preferences.
/// Initialized in the Tauri setup hook from `app.path().app_config_dir()`.
pub struct PrefsState {
    pub config_dir: std::path::PathBuf,
    pub prefs: std::sync::Mutex<crate::config::prefs::UserPreferences>,
}

impl PrefsState {
    pub fn new(config_dir: std::path::PathBuf, prefs: crate::config::prefs::UserPreferences) -> Self {
        Self {
            config_dir,
            prefs: std::sync::Mutex::new(prefs),
        }
    }
}

/// Managed state for show name overrides.
/// Loaded at startup; overrides do not change during a session.
pub struct OverridesState {
    pub overrides: Vec<crate::overrides::ShowOverride>,
}

impl OverridesState {
    pub fn new(overrides: Vec<crate::overrides::ShowOverride>) -> Self {
        Self { overrides }
    }
}

#[cfg(test)]
mod tests {
    use super::AppState;

    #[test]
    fn app_state_constructs() {
        let state = AppState::new().expect("AppState::new() must succeed in normal environment");
        drop(state);
    }

    #[test]
    fn prefs_state_constructs_and_locks() {
        let dir = tempfile::tempdir().unwrap();
        let state = super::PrefsState::new(
            dir.path().to_path_buf(),
            crate::config::prefs::UserPreferences::default(),
        );
        let prefs = state.prefs.lock().unwrap();
        assert_eq!(prefs.dest_dir, "~/TV");
    }

    #[test]
    fn overrides_state_constructs() {
        let state = super::OverridesState::new(crate::overrides::bundled_defaults());
        assert_eq!(state.overrides.len(), 3);
    }
}
