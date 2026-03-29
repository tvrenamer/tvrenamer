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

#[cfg(test)]
mod tests {
    use super::AppState;

    #[test]
    fn app_state_constructs() {
        let state = AppState::new().expect("AppState::new() must succeed in normal environment");
        drop(state);
    }
}
