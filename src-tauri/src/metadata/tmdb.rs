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
    async fn search_series(&self, query: &str) -> Result<Vec<Series>, AppError> {
        let url = format!("{}/3/search/tv", self.base_url);
        let mut retry_delay = Duration::from_secs(1);

        for attempt in 0..=3u32 {
            let response = self
                .client
                .get(&url)
                .bearer_auth(&self.api_key)
                .query(&[("query", query)])
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
                    let body: TmdbSearchResponse = response
                        .json()
                        .await
                        .map_err(|e| AppError::NetworkError(e.to_string()))?;
                    if body.results.is_empty() {
                        return Err(AppError::SeriesNotFound);
                    }
                    return Ok(body
                        .results
                        .into_iter()
                        .map(|r| Series {
                            id: r.id,
                            name: r.name,
                            first_air_date: r.first_air_date,
                        })
                        .collect());
                }
                401 => return Err(AppError::ApiKeyMissing),
                429 if attempt < 3 => {
                    tokio::time::sleep(retry_delay).await;
                    retry_delay *= 2;
                    continue;
                }
                429 => return Err(AppError::RateLimited),
                status => {
                    return Err(AppError::NetworkError(format!(
                        "Downloading show listings failed. Check internet connection (HTTP {})",
                        status
                    )))
                }
            }
        }
        unreachable!()
    }

    async fn get_episode(
        &self,
        series_id: u32,
        season: u32,
        episode: u32,
    ) -> Result<Episode, AppError> {
        let url = format!(
            "{}/3/tv/{}/season/{}/episode/{}",
            self.base_url, series_id, season, episode
        );
        let mut retry_delay = Duration::from_secs(1);

        for attempt in 0..=3u32 {
            let response = self
                .client
                .get(&url)
                .bearer_auth(&self.api_key)
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
                    let body: TmdbEpisodeResponse = response
                        .json()
                        .await
                        .map_err(|e| AppError::NetworkError(e.to_string()))?;
                    return Ok(Episode {
                        name: body.name,
                        season_number: body.season_number,
                        episode_number: body.episode_number,
                        air_date: body.air_date,
                        overview: body.overview,
                    });
                }
                401 => return Err(AppError::ApiKeyMissing),
                404 => return Err(AppError::EpisodeNotFound),
                429 if attempt < 3 => {
                    tokio::time::sleep(retry_delay).await;
                    retry_delay *= 2;
                    continue;
                }
                429 => return Err(AppError::RateLimited),
                status => {
                    return Err(AppError::NetworkError(format!(
                        "Could not get episode for show (HTTP {})",
                        status
                    )))
                }
            }
        }
        unreachable!()
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;
    use wiremock::matchers::{header_exists, method, path};
    use wiremock::{Mock, MockServer, ResponseTemplate};

    use crate::errors::AppError;
    use crate::metadata::provider::MetadataProvider;
    use super::TmdbProvider;

    fn test_client() -> reqwest::Client {
        reqwest::Client::builder()
            .timeout(Duration::from_secs(5))
            .build()
            .unwrap()
    }

    // --- search_series tests ---

    #[tokio::test]
    async fn search_series_returns_results() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .and(header_exists("Authorization"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "results": [
                    {"id": 1396, "name": "Breaking Bad", "first_air_date": "2008-01-20"},
                    {"id": 12345, "name": "Breaking Bad: The Movie", "first_air_date": null}
                ]
            })))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let results = provider.search_series("Breaking Bad").await.unwrap();

        assert_eq!(results.len(), 2);
        assert_eq!(results[0].id, 1396);
        assert_eq!(results[0].name, "Breaking Bad");
        assert_eq!(results[0].first_air_date.as_deref(), Some("2008-01-20"));
        assert_eq!(results[1].first_air_date, None);
    }

    #[tokio::test]
    async fn search_series_empty_results_returns_series_not_found() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(serde_json::json!({"results": []})),
            )
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let result = provider.search_series("nonexistent show zzzz").await;
        assert!(matches!(result, Err(AppError::SeriesNotFound)));
    }

    #[tokio::test]
    async fn search_series_401_returns_api_key_missing() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .respond_with(ResponseTemplate::new(401))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "bad-key", mock_server.uri());
        let result = provider.search_series("anything").await;
        assert!(matches!(result, Err(AppError::ApiKeyMissing)));
    }

    #[tokio::test]
    async fn search_series_429_retries_and_succeeds() {
        let mock_server = MockServer::start().await;
        // First request hits 429; subsequent requests get 200.
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .respond_with(ResponseTemplate::new(429))
            .up_to_n_times(1)
            .mount(&mock_server)
            .await;
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "results": [{"id": 42, "name": "Firefly", "first_air_date": "2002-09-20"}]
            })))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let results: Vec<_> = provider.search_series("Firefly").await.unwrap();
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].name, "Firefly");
    }

    #[tokio::test]
    async fn search_series_429_all_retries_exhausted_returns_rate_limited() {
        let mock_server = MockServer::start().await;
        // All 4 requests (attempt 0..=3) return 429 — exhausts retries.
        Mock::given(method("GET"))
            .and(path("/3/search/tv"))
            .respond_with(ResponseTemplate::new(429))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let result = provider.search_series("anything").await;
        assert!(matches!(result, Err(AppError::RateLimited)));
    }

    // --- get_episode tests ---

    #[tokio::test]
    async fn get_episode_returns_episode() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/1"))
            .and(header_exists("Authorization"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "name": "Pilot",
                "air_date": "2008-01-20",
                "episode_number": 1,
                "season_number": 1,
                "overview": "Walter White, a chemistry teacher diagnosed with cancer..."
            })))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let episode = provider.get_episode(1396, 1, 1).await.unwrap();

        assert_eq!(episode.name, "Pilot");
        assert_eq!(episode.season_number, 1);
        assert_eq!(episode.episode_number, 1);
        assert_eq!(episode.air_date.as_deref(), Some("2008-01-20"));
        assert!(episode.overview.is_some());
    }

    #[tokio::test]
    async fn get_episode_optional_fields_absent() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/2"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "name": "Cat's in the Bag",
                "air_date": null,
                "episode_number": 2,
                "season_number": 1,
                "overview": null
            })))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let episode = provider.get_episode(1396, 1, 2).await.unwrap();

        assert_eq!(episode.name, "Cat's in the Bag");
        assert_eq!(episode.air_date, None);
        assert_eq!(episode.overview, None);
    }

    #[tokio::test]
    async fn get_episode_404_returns_episode_not_found() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/tv/9999/season/99/episode/99"))
            .respond_with(ResponseTemplate::new(404))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let result = provider.get_episode(9999, 99, 99).await;
        assert!(matches!(result, Err(AppError::EpisodeNotFound)));
    }

    #[tokio::test]
    async fn get_episode_401_returns_api_key_missing() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/1"))
            .respond_with(ResponseTemplate::new(401))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "bad-key", mock_server.uri());
        let result = provider.get_episode(1396, 1, 1).await;
        assert!(matches!(result, Err(AppError::ApiKeyMissing)));
    }

    #[tokio::test]
    async fn get_episode_429_retries_and_succeeds() {
        let mock_server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/3"))
            .respond_with(ResponseTemplate::new(429))
            .up_to_n_times(1)
            .mount(&mock_server)
            .await;
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/3"))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "name": "...And the Bag's in the River",
                "air_date": "2008-02-10",
                "episode_number": 3,
                "season_number": 1,
                "overview": null
            })))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let episode = provider.get_episode(1396, 1, 3).await.unwrap();
        assert_eq!(episode.name, "...And the Bag's in the River");
    }

    #[tokio::test]
    async fn get_episode_429_all_retries_exhausted_returns_rate_limited() {
        let mock_server = MockServer::start().await;
        // All 4 requests (attempt 0..=3) return 429 — exhausts retries.
        Mock::given(method("GET"))
            .and(path("/3/tv/1396/season/1/episode/99"))
            .respond_with(ResponseTemplate::new(429))
            .mount(&mock_server)
            .await;

        let provider =
            TmdbProvider::new_with_base_url(test_client(), "key", mock_server.uri());
        let result = provider.get_episode(1396, 1, 99).await;
        assert!(matches!(result, Err(AppError::RateLimited)));
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

    #[tokio::test]
    #[ignore]
    async fn live_search_breaking_bad() {
        let key = std::env::var("TMDB_API_KEY").expect("set TMDB_API_KEY");
        let client = reqwest::Client::new();
        let provider = TmdbProvider::new(client, key);
        let results = provider.search_series("Breaking Bad").await.unwrap();
        assert!(!results.is_empty());
        println!("{:?}", results[0].name);
    }
}
