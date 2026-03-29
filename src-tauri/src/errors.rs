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
    #[error("Preferences corrupted")]
    PreferencesCorrupted,
}

#[cfg(test)]
mod tests {
    use super::AppError;

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
        ];
        for v in &variants {
            serde_json::to_string(v).expect("AppError must be serializable to pass through IPC");
        }
    }
}
