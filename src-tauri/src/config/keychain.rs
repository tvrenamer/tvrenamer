use keyring::Entry;
use crate::errors::AppError;

const SERVICE_NAME: &str = "tvrenamer";
const API_KEY_ACCOUNT: &str = "tmdb_api_key";

/// Read the TMDB API key from the OS keychain.
/// Returns `Err(ApiKeyMissing)` if the key has never been set.
pub fn read_api_key() -> Result<String, AppError> {
    let entry = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT)
        .map_err(|e| AppError::NetworkError(e.to_string()))?;
    entry.get_password().map_err(|e| match e {
        keyring::Error::NoEntry => AppError::ApiKeyMissing,
        _ => AppError::NetworkError(e.to_string()),
    })
}

/// Save the TMDB API key to the OS keychain.
pub fn save_api_key(key: &str) -> Result<(), AppError> {
    let entry = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT)
        .map_err(|e| AppError::NetworkError(e.to_string()))?;
    entry
        .set_password(key)
        .map_err(|e| AppError::NetworkError(e.to_string()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn save_then_read_roundtrip() {
        // Writes to the real OS keychain — requires macOS Keychain or Linux Secret Service.
        let test_key = "test-tmdb-key-tvrenamer-do-not-use";
        save_api_key(test_key).expect("save_api_key should succeed");
        let retrieved = read_api_key().expect("read_api_key should succeed after save");
        assert_eq!(retrieved, test_key);
        // Clean up — leave keychain in the state we found it.
        let entry = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT).unwrap();
        let _ = entry.delete_credential();
    }

    #[test]
    fn read_missing_key_returns_api_key_missing() {
        // Ensure no key is present, then read.
        if let Ok(entry) = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT) {
            let _ = entry.delete_credential();
        }
        let result = read_api_key();
        assert!(
            matches!(result, Err(AppError::ApiKeyMissing)),
            "Expected ApiKeyMissing after deleting credential, got: {:?}",
            result
        );
    }
}
