use std::time::Duration;

use crate::errors::AppError;
use super::models::{Episode, Series};
use super::provider::MetadataProvider;

const TMDB_BASE_URL: &str = "https://api.themoviedb.org";

// --- Internal TMDB response DTOs (serde only, never exposed publicly) ---

#[derive(serde::Deserialize)]
struct TmdbSearchResponse {
    results: Vec<TmdbSearchResult>,
}

#[derive(serde::Deserialize)]
struct TmdbSearchResult {
    id: u32,
    name: String,
    first_air_date: Option<String>,
}

#[derive(serde::Deserialize)]
struct TmdbEpisodeResponse {
    name: String,
    air_date: Option<String>,
    episode_number: u32,
    season_number: u32,
    overview: Option<String>,
}

#[derive(serde::Deserialize)]
struct TmdbAuthResponse {
    success: bool,
}

// --- TmdbProvider ---

pub struct TmdbProvider {
    client: reqwest::Client,
    api_key: String,
    base_url: String,
}

impl TmdbProvider {
    /// Production constructor — uses the live TMDB API endpoint.
    pub fn new(client: reqwest::Client, api_key: impl Into<String>) -> Self {
        Self {
            client,
            api_key: api_key.into(),
            base_url: TMDB_BASE_URL.to_string(),
        }
    }

    /// Test constructor — injects a mock server base URL.
    #[cfg(test)]
    pub fn new_with_base_url(
        client: reqwest::Client,
        api_key: impl Into<String>,
        base_url: impl Into<String>,
    ) -> Self {
        Self {
            client,
            api_key: api_key.into(),
            base_url: base_url.into(),
        }
    }

    /// Validate an API key against TMDB's /3/authentication endpoint.
    /// Returns Ok(()) if the key is valid. Does not save the key.
    /// Takes `base_url` so tests can point at a mock server.
    pub async fn validate_key(
        client: &reqwest::Client,
        api_key: &str,
        base_url: &str,
    ) -> Result<(), AppError> {
        let url = format!("{}/3/authentication", base_url);
        let response = client
            .get(&url)
            .bearer_auth(api_key)
            .send()
            .await
            .map_err(|e| {
                if e.is_timeout() {
                    AppError::NetworkTimeout(e.to_string())
                } else {
                    AppError::NetworkError(e.to_string())
                }
            })?;

        match response.status().as_u16() {
            200 => {
                let body: TmdbAuthResponse = response
                    .json()
                    .await
                    .map_err(|e| AppError::NetworkError(e.to_string()))?;
                if body.success {
                    Ok(())
                } else {
                    Err(AppError::ApiKeyMissing)
                }
            }
            401 => Err(AppError::ApiKeyMissing),
            status => Err(AppError::NetworkError(format!(
                "Unexpected status {} from /3/authentication",
                status
            ))),
        }
    }
}

impl MetadataProvider for TmdbProvider {
    async fn search_series(&self, _query: &str) -> Result<Vec<Series>, AppError> {
        unimplemented!("implement in Task 4")
    }

    async fn get_episode(
        &self,
        _series_id: u32,
        _season: u32,
        _episode: u32,
    ) -> Result<Episode, AppError> {
        unimplemented!("implement in Task 5")
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;
    use wiremock::matchers::{header_exists, method, path};
    use wiremock::{Mock, MockServer, ResponseTemplate};

    use crate::errors::AppError;
    use super::TmdbProvider;

    fn test_client() -> reqwest::Client {
        reqwest::Client::builder()
            .timeout(Duration::from_secs(5))
            .build()
            .unwrap()
    }

    // --- validate_key tests ---

    #[tokio::test]
    async fn validate_key_success() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/authentication"))
            .and(header_exists("Authorization"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "success": true,
                "status_code": 1,
                "status_message": "Success."
            })))
            .mount(&mock_server)
            .await;

        let result =
            TmdbProvider::validate_key(&test_client(), "valid-key", &mock_server.uri()).await;
        assert!(result.is_ok(), "Expected Ok(()), got {:?}", result);
    }

    #[tokio::test]
    async fn validate_key_401_returns_api_key_missing() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/authentication"))
            .respond_with(ResponseTemplate::new(401))
            .mount(&mock_server)
            .await;

        let result =
            TmdbProvider::validate_key(&test_client(), "bad-key", &mock_server.uri()).await;
        assert!(matches!(result, Err(AppError::ApiKeyMissing)));
    }
}
