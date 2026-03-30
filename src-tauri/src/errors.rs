use thiserror::Error;

#[derive(Error, Debug, serde::Serialize)]
pub enum AppError {
    #[error("API key invalid or missing")]
    ApiKeyMissing,
    #[error("API discontinued")]
    ApiDiscontinued,
    #[error("Network timeout: {0}")]
    NetworkTimeout(String),
    #[error("File not found: {0}")]
    FileNotFound(String),
    #[error("Permission denied: {0}")]
    PermissionDenied(String),
    #[error("Destination already exists")]
    DestinationExists,
    #[error("Parse failed: no pattern matched")]
    ParseFailed,
    #[error("Unable to find show information")]
    SeriesNotFound,
    #[error("Could not get episode for show")]
    EpisodeNotFound,
    #[error("Rate limit exceeded")]
    RateLimited,
    #[error("Downloading show listings failed. Check internet connection: {0}")]
    NetworkError(String),
    #[error("Preferences corrupted")]
    PreferencesCorrupted,
    #[error("Disk full: {0}")]
    DiskFull(String),
}

#[cfg(test)]
mod tests {
    use super::AppError;

    #[test]
    fn new_error_variants_serialize() {
        let variants: Vec<AppError> = vec![
            AppError::SeriesNotFound,
            AppError::EpisodeNotFound,
            AppError::RateLimited,
            AppError::NetworkError("general failure".into()),
        ];
        for v in &variants {
            serde_json::to_string(v).expect("new AppError variants must serialize for IPC");
        }
    }

    #[test]
    fn all_error_variants_serialize() {
        let variants: Vec<AppError> = vec![
            AppError::ApiKeyMissing,
            AppError::ApiDiscontinued,
            AppError::NetworkTimeout("timeout".into()),
            AppError::FileNotFound("path".into()),
            AppError::PermissionDenied("path".into()),
            AppError::DestinationExists,
            AppError::ParseFailed,
            AppError::PreferencesCorrupted,
            AppError::DiskFull("no space".into()),
        ];
        for v in &variants {
            serde_json::to_string(v).expect("AppError must be serializable to pass through IPC");
        }
    }
}
