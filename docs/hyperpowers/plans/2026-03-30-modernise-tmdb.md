# TMDB Client Implementation Plan

> **For Claude:** Run `/execute-plan` to implement this plan (will ask which execution style you prefer). Steps use checkbox (`- [ ]`) syntax for tracking.
> **Related Issues:** None found in repository (no issue tracker detected).

**Goal:** Implement `TmdbProvider` in `src-tauri/src/metadata/tmdb.rs`, replacing `unimplemented!()` stubs with a fully functional async TMDB v3 REST client: show search, episode lookup, API key management (OS keychain), and exponential backoff on rate limits.

**Architecture:** `TmdbProvider` takes a shared `reqwest::Client` (from `AppState`) and an API key (read from OS keychain by the IPC layer) at construction time. All HTTP calls use a `Authorization: Bearer <key>` header. 429 responses trigger exponential backoff (up to 3 retries: 1s → 2s → 4s). The base URL is a constructor parameter so `wiremock`-based unit tests can point at a local mock server without live network access.

**Tech Stack:** `reqwest = "0.13"`, `keyring = "3.6"`, `thiserror = "1"`, `tokio = "1"` (all already in Cargo.toml), `wiremock = "0.6"` (new dev dependency).

**Context Gathered From:**
- `docs/hyperpowers/research/2026-03-29-modernise-tmdb.md`
- `docs/hyperpowers/research/2026-03-29-modernise-stack.md` (parent research)
- `src-tauri/src/metadata/tmdb.rs` (current stub — `unimplemented!()`)
- `src-tauri/src/metadata/models.rs` (`Series` / `Episode` structs — do NOT modify)
- `src-tauri/src/metadata/provider.rs` (`MetadataProvider` trait — do NOT modify)
- `src-tauri/src/errors.rs` (`AppError` enum — needs new variants)
- `src-tauri/src/ipc.rs` (only has `ping()` — needs real commands)
- `src-tauri/src/config/mod.rs` (has `prefs` and `migration` — add `keychain`)
- `src-tauri/Cargo.toml` (add `wiremock` dev-dep)

---

## Critical Design Notes (Read Before Writing a Line)

### TmdbProvider struct change

The current stub stores only `client`. This plan changes it to also store `api_key` and `base_url`:

```rust
pub struct TmdbProvider {
    client: reqwest::Client,
    api_key: String,
    base_url: String, // "https://api.themoviedb.org" in production; mock URL in tests
}
```

**Why `base_url`?** Tests use `wiremock` which spawns a local HTTP server. By injecting the base URL at construction time, tests can point the provider at the mock server. Only `new_with_base_url` is `#[cfg(test)]`; production code always uses `new(client, api_key)`.

**How the API key flows:** IPC command reads key from keyring → constructs `TmdbProvider::new(client, key)` → calls provider method → returns result to frontend. The provider holds the key only for the IPC handler's lifetime — it is NOT stored in `AppState`.

### Authentication header

All TMDB v3 requests use bearer auth:

```
Authorization: Bearer <api_key>
```

Do **not** use `?api_key=<key>` query param. Bearer token is TMDB's preferred method.

### Exponential backoff (manual loop — no async closures)

Use an explicit `for attempt in 0..=3` loop inside each method. Async closures require nightly for the `async Fn` trait bounds; stable Rust 1.77 does not support them.

```
attempt 0 → 429: sleep 1s, retry
attempt 1 → 429: sleep 2s, retry
attempt 2 → 429: sleep 4s, retry
attempt 3 → 429: return Err(RateLimited)
```

### Error mapping

| HTTP status | `AppError` variant |
|-------------|-------------------|
| 200, empty results | `SeriesNotFound` |
| 401 | `ApiKeyMissing` |
| 404 | `EpisodeNotFound` (episode endpoint only) |
| 429 (exhausted) | `RateLimited` |
| timeout | `NetworkTimeout(msg)` |
| other | `NetworkError(msg)` |

### IPC error convention

`ipc.rs` uses `Result<T, String>`. Map `AppError` via `.map_err(|e| e.to_string())`.

### User-facing error messages (port verbatim from Java)

| Situation | Message stored in `AppError::Display` |
|-----------|--------------------------------------|
| Empty search results | `"Unable to find show information"` |
| Episode not found | `"Could not get episode for show"` |
| Network timeout | `"Timed out trying to look up"` |
| General network failure | `"Downloading show listings failed. Check internet connection"` |

These are already in the error variant `#[error(...)]` strings below.

---

## Task 1: Add Missing AppError Variants

**Files:**
- Modify: `src-tauri/src/errors.rs`

- [ ] **Step 1: Write the failing test**

Add inside the existing `tests` module in `errors.rs`:

```rust
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
```

- [ ] **Step 2: Run — expect compile error**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib errors::tests::new_error_variants_serialize 2>&1 | tail -8
```

Expected: compile error — `SeriesNotFound` etc. not found in `AppError`.

- [ ] **Step 3: Add variants to `AppError`**

Add after the existing `ParseFailed` line in `errors.rs`:

```rust
#[error("Unable to find show information")]
SeriesNotFound,
#[error("Could not get episode for show")]
EpisodeNotFound,
#[error("Rate limit exceeded")]
RateLimited,
#[error("Downloading show listings failed. Check internet connection: {0}")]
NetworkError(String),
```

- [ ] **Step 4: Run all error tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib errors::tests 2>&1
```

Expected: all pass (existing + new).

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/errors.rs
git commit -m "feat(errors): add SeriesNotFound, EpisodeNotFound, RateLimited, NetworkError variants"
```

---

## Task 2: Add wiremock Dev Dependency

**Files:**
- Modify: `src-tauri/Cargo.toml`

- [ ] **Step 1: Add `[dev-dependencies]` section at the end of Cargo.toml**

```toml
[dev-dependencies]
wiremock = "0.6"
```

- [ ] **Step 2: Verify it resolves**

```bash
cargo fetch --manifest-path src-tauri/Cargo.toml 2>&1 | tail -5
```

Expected: downloads `wiremock` and its transitive deps, no errors.

- [ ] **Step 3: Commit**

```bash
git add src-tauri/Cargo.toml
git commit -m "chore(deps): add wiremock dev dependency for TMDB HTTP-level tests"
```

---

## Task 3: Replace TmdbProvider Stub + Add Response DTOs + Implement validate_key

**Files:**
- Modify: `src-tauri/src/metadata/tmdb.rs`

- [ ] **Step 1: Write failing tests for `validate_key`**

Replace the entire contents of `src-tauri/src/metadata/tmdb.rs` with:

```rust
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
```

- [ ] **Step 2: Run the validate_key tests (they should pass — validate_key is already implemented)**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata::tmdb::tests::validate_key 2>&1
```

Expected: both `validate_key_*` tests pass.

- [ ] **Step 3: Commit**

```bash
git add src-tauri/src/metadata/tmdb.rs
git commit -m "feat(tmdb): replace stub — add response DTOs, TmdbProvider struct, validate_key"
```

---

## Task 4: Implement and Test `search_series`

**Files:**
- Modify: `src-tauri/src/metadata/tmdb.rs`

- [ ] **Step 1: Write failing tests**

Add these four tests inside the `tests` module in `tmdb.rs` (after the existing validate_key tests):

```rust
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
    let results = provider.search_series("Firefly").await.unwrap();
    assert_eq!(results.len(), 1);
    assert_eq!(results[0].name, "Firefly");
}
```

- [ ] **Step 2: Run to confirm all 4 fail (unimplemented!)**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata::tmdb::tests::search_series 2>&1 | tail -10
```

Expected: 4 failures with `not implemented: implement in Task 4`.

- [ ] **Step 3: Implement `search_series`**

Replace the `search_series` stub:

```rust
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
```

- [ ] **Step 4: Run all search_series tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata::tmdb::tests::search_series 2>&1
```

Expected: all 4 pass. The `429_retries_and_succeeds` test takes ~1 second due to the retry sleep.

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/metadata/tmdb.rs
git commit -m "feat(tmdb): implement search_series with exponential backoff — 4 tests pass"
```

---

## Task 5: Implement and Test `get_episode`

**Files:**
- Modify: `src-tauri/src/metadata/tmdb.rs`

- [ ] **Step 1: Write failing tests**

Add to the `tests` module in `tmdb.rs`:

```rust
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
```

- [ ] **Step 2: Run to confirm all 5 fail**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata::tmdb::tests::get_episode 2>&1 | tail -10
```

Expected: 5 failures with `not implemented: implement in Task 5`.

- [ ] **Step 3: Implement `get_episode`**

Replace the `get_episode` stub:

```rust
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
```

- [ ] **Step 4: Run all get_episode tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata::tmdb::tests::get_episode 2>&1
```

Expected: all 5 pass.

- [ ] **Step 5: Run the full metadata test suite**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib metadata 2>&1
```

Expected: all 11 tests pass (2 validate_key + 4 search_series + 5 get_episode).

- [ ] **Step 6: Commit**

```bash
git add src-tauri/src/metadata/tmdb.rs
git commit -m "feat(tmdb): implement get_episode with retry backoff — all 11 metadata tests pass"
```

---

## Task 6: API Key Keychain Helpers

**Files:**
- Create: `src-tauri/src/config/keychain.rs`
- Modify: `src-tauri/src/config/mod.rs`

The API key lives in the OS keychain, not in `prefs.json` (as noted in `prefs.rs`). These helpers are `config` module concerns — they are configuration accessors, not provider logic.

- [ ] **Step 1: Write failing tests**

Create `src-tauri/src/config/keychain.rs`:

```rust
use keyring::Entry;
use crate::errors::AppError;

const SERVICE_NAME: &str = "tvrenamer";
const API_KEY_ACCOUNT: &str = "tmdb_api_key";

/// Read the TMDB API key from the OS keychain.
/// Returns `Err(ApiKeyMissing)` if the key has never been set.
pub fn read_api_key() -> Result<String, AppError> {
    unimplemented!("implement in Step 3")
}

/// Save the TMDB API key to the OS keychain.
pub fn save_api_key(key: &str) -> Result<(), AppError> {
    unimplemented!("implement in Step 3")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn save_then_read_roundtrip() {
        // Writes to the real OS keychain — requires macOS Keychain or Linux Secret Service.
        // On headless Linux without Secret Service, this test will fail (known gap).
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
```

- [ ] **Step 2: Declare `keychain` in `config/mod.rs`**

Add this line to `src-tauri/src/config/mod.rs`:

```rust
pub mod keychain;
```

- [ ] **Step 3: Run to confirm tests fail (unimplemented!)**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib config::keychain::tests 2>&1 | tail -5
```

Expected: failures with `not implemented`.

- [ ] **Step 4: Implement `read_api_key` and `save_api_key`**

Replace the unimplemented functions in `keychain.rs`:

```rust
pub fn read_api_key() -> Result<String, AppError> {
    let entry = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT)
        .map_err(|e| AppError::NetworkError(e.to_string()))?;
    entry.get_password().map_err(|e| match e {
        keyring::Error::NoEntry => AppError::ApiKeyMissing,
        _ => AppError::NetworkError(e.to_string()),
    })
}

pub fn save_api_key(key: &str) -> Result<(), AppError> {
    let entry = Entry::new(SERVICE_NAME, API_KEY_ACCOUNT)
        .map_err(|e| AppError::NetworkError(e.to_string()))?;
    entry
        .set_password(key)
        .map_err(|e| AppError::NetworkError(e.to_string()))
}
```

**Note:** If the `keyring::Error::NoEntry` variant does not match the actual error type, run:

```bash
cargo doc --manifest-path src-tauri/Cargo.toml --open
```

Navigate to `keyring::Error` and check the correct variant name. Common alternatives: `keyring::Error::NoStorageAccess`, `keyring::Error::NotFound`. Update the match arm accordingly.

- [ ] **Step 5: Run keychain tests**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib config::keychain::tests 2>&1
```

Expected on macOS: both pass. On headless Linux without Secret Service: `save_then_read_roundtrip` may fail — this is the documented known gap. The `read_missing_key_returns_api_key_missing` test may also behave differently on Linux.

- [ ] **Step 6: Commit**

```bash
git add src-tauri/src/config/keychain.rs src-tauri/src/config/mod.rs
git commit -m "feat(config): add read_api_key and save_api_key OS keychain helpers"
```

---

## Task 7: IPC Commands

**Files:**
- Modify: `src-tauri/src/ipc.rs`
- Modify: `src-tauri/src/lib.rs`

Expose four commands to the TypeScript frontend: search shows, lookup episode, validate key (onboarding "Test" button), save key.

- [ ] **Step 1: Replace `ipc.rs` entirely**

```rust
// All #[tauri::command] functions — the IPC boundary between Rust and the React frontend.
// Error convention: Result<T, String> — AppError serialized via Display.

use tauri::State;

use crate::config::keychain;
use crate::metadata::models::{Episode, Series};
use crate::metadata::provider::MetadataProvider;
use crate::metadata::tmdb::TmdbProvider;
use crate::state::AppState;

/// Smoke-test command — verifies the IPC bridge is operational.
#[tauri::command]
pub async fn ping() -> Result<String, String> {
    Ok("pong".to_string())
}

/// Search TMDB for TV series matching `query`.
/// Reads the API key from the OS keychain on every call.
/// Returns `Err("Unable to find show information")` if no results.
/// Returns `Err("API key invalid or missing")` if no key saved yet.
#[tauri::command]
pub async fn search_shows(
    query: String,
    state: State<'_, AppState>,
) -> Result<Vec<Series>, String> {
    let api_key = keychain::read_api_key().map_err(|e| e.to_string())?;
    TmdbProvider::new(state.http_client.clone(), api_key)
        .search_series(&query)
        .await
        .map_err(|e| e.to_string())
}

/// Fetch a specific episode from TMDB by series ID, season number, and episode number.
/// Returns `Err("Could not get episode for show")` if the episode doesn't exist on TMDB.
#[tauri::command]
pub async fn lookup_episode(
    series_id: u32,
    season: u32,
    episode: u32,
    state: State<'_, AppState>,
) -> Result<Episode, String> {
    let api_key = keychain::read_api_key().map_err(|e| e.to_string())?;
    TmdbProvider::new(state.http_client.clone(), api_key)
        .get_episode(series_id, season, episode)
        .await
        .map_err(|e| e.to_string())
}

/// Validate a TMDB API key without saving it.
/// Called by the onboarding modal's "Test" button.
/// Returns Ok(()) if the key is accepted by TMDB /3/authentication.
#[tauri::command]
pub async fn validate_tmdb_key(
    key: String,
    state: State<'_, AppState>,
) -> Result<(), String> {
    TmdbProvider::validate_key(&state.http_client, &key, "https://api.themoviedb.org")
        .await
        .map_err(|e| e.to_string())
}

/// Save a TMDB API key to the OS keychain.
/// Call only after `validate_tmdb_key` returns Ok.
#[tauri::command]
pub async fn save_tmdb_key(key: String) -> Result<(), String> {
    keychain::save_api_key(&key).map_err(|e| e.to_string())
}
```

- [ ] **Step 2: Register the new commands in `lib.rs`**

Replace the `.invoke_handler(...)` line in `lib.rs`:

```rust
.invoke_handler(tauri::generate_handler![
    ipc::ping,
    ipc::search_shows,
    ipc::lookup_episode,
    ipc::validate_tmdb_key,
    ipc::save_tmdb_key,
])
```

- [ ] **Step 3: Check compile**

```bash
cargo check --manifest-path src-tauri/Cargo.toml 2>&1
```

Expected: no errors.

- [ ] **Step 4: Run the full test suite**

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib 2>&1
```

Expected: all tests pass — errors, state, metadata (11), config/prefs, config/keychain, parser (103+).

If any test fails, check the error message. Common issues:
- **`keyring::Error::NoEntry` variant wrong**: Run `cargo doc` and check the variant name — update the `match e` arm in `keychain.rs`.
- **`wiremock` API mismatch** (e.g., `up_to_n_times` not found): Replace with two separate `MockServer` instances in the retry tests — one that always returns 429 and one that always returns 200, and swap the URL on the second attempt.

- [ ] **Step 5: Commit**

```bash
git add src-tauri/src/ipc.rs src-tauri/src/lib.rs
git commit -m "feat(ipc): add search_shows, lookup_episode, validate_tmdb_key, save_tmdb_key commands"
```

---

## Validated Assumptions

### ✅ Verified Against Research + Codebase

| Assumption | Source |
|------------|--------|
| `reqwest = "0.13"` in Cargo.toml | `src-tauri/Cargo.toml:25` |
| `keyring = "3.6"` in Cargo.toml | `src-tauri/Cargo.toml:32` |
| `tokio = { version = "1", features = ["full"] }` | `src-tauri/Cargo.toml:27` |
| `AppState.http_client: reqwest::Client` | `src-tauri/src/state.rs:5` |
| `MetadataProvider` trait: `search_series` + `get_episode` | `src-tauri/src/metadata/provider.rs` |
| `Series` struct: `id: u32`, `name: String`, `first_air_date: Option<String>` | `src-tauri/src/metadata/models.rs:5-8` |
| `Episode` struct: `name`, `season_number`, `episode_number`, `air_date`, `overview` | `src-tauri/src/metadata/models.rs:11-17` |
| `prefs.rs` explicitly says API key must NOT go in `prefs.json` | `src-tauri/src/config/prefs.rs:4` |
| TMDB `GET /3/search/tv` returns `results[]` with `id`, `name`, `first_air_date` | Research doc — validated |
| TMDB rate limits: 50 req/sec, 20 concurrent | Research doc — validated |
| Bearer token preferred over `?api_key=` query param | Research doc — validated |
| `keyring = "3.6"`: macOS Keychain / Windows Credential Manager / Linux Secret Service | Research doc — validated |
| TMDB `/3/authentication` response: `{"success": true, "status_code": 1, "status_message": "..."}` | Research doc — validated (3 fields) |

### ⚠️ Verify at Implementation Time

| Assumption | Risk |
|------------|------|
| `keyring::Error::NoEntry` variant name | The exact variant may differ in keyring 3.6. Run `cargo doc --manifest-path src-tauri/Cargo.toml` and inspect `keyring::Error` before writing the match arm. |
| TMDB episode endpoint field names | Research flags this as "likely correct, verify". All `Episode` fields use `Option<String>` as a guard against missing fields. |
| `wiremock = "0.6"` — `up_to_n_times(u64)` method signature | If this method doesn't exist, replace the retry test with a counter-based approach using `wiremock::Request` inspection or two separate MockServers. |

### ❌ Known Gaps (Not In Scope for This Plan)

| Gap | Decision |
|-----|----------|
| `keyring` on headless Linux (no Secret Service daemon) | Known gap from research — document, skip CI keychain tests on headless Linux. |
| Season 0 / TMDB special episodes | Not supported by parser; known gap, do not fix here. |
| API key revalidation on app startup | Out of scope — validate only at onboarding entry time. |

---

## Assumption Validation Results

*From assumption-checker agent — 23 validated, 1 invalid (non-blocking), 4 unverified.*

### ✅ Validated (23)

- `MockServer::start().await`, `Mock::given(...).and(...).respond_with(...).up_to_n_times(N).mount(&server).await` — all wiremock 0.6 API confirmed
- `keyring::Entry::new("tvrenamer", "tmdb_api_key")`, `.get_password()`, `.set_password()`, `.delete_credential()` — all keyring 3.6 API confirmed
- **`keyring::Error::NoEntry`** is the correct variant for "key not found" — confirmed
- `client.get(url).bearer_auth(token).query(&[...]).send().await`, `response.status().as_u16()`, `response.json::<T>().await`, `e.is_timeout()` — all reqwest 0.13 API confirmed
- `#[tokio::test]` macro works correctly for async test functions
- Async methods in traits (no `async_trait` macro needed) — stable since Rust 1.75
- `for attempt in 0..=3u32` loop, `Duration::from_secs(1)`, `retry_delay *= 2` — all valid stable Rust 1.77
- TMDB `GET /3/search/tv` returns `id`, `name`, `first_air_date` in `results[]` — confirmed
- TMDB `GET /3/tv/{id}/season/{s}/episode/{e}` returns `name`, `air_date`, `episode_number`, `season_number`, `overview` — confirmed
- TMDB returns 401 on invalid key, 429 on rate limit — confirmed

### ❌ Invalid (1 — non-blocking)

| Claim | Correction |
|-------|------------|
| `#[tokio::test]` requires `features = ["full"]` | Only `rt` (or `rt-multi-thread`) and `macros` features are required. `"full"` is overkill — but since `Cargo.toml` already uses `"full"`, this has no impact on this plan. Do NOT change it here. |

### ⚠️ Unverified (4)

- TMDB `/3/authentication` exact response shape — likely `{"success": bool, "status_code": u32, "status_message": String}` but not 100% confirmed from interactive schema
- TMDB `id` field is `u32` specifically — confirmed as numeric, bit-width not explicitly stated in docs
- `ResponseTemplate::set_body_json` accepts `serde_json::json!()` macro output — very likely, verify if compile fails
- `wiremock = "0.6"` exact version available — use `^0.6` if `"0.6"` doesn't resolve

---

## Open Questions (Carry Forward from Research)

1. **`keyring` on headless Linux**: On systems without a Secret Service daemon (CI, servers), `save_api_key` and `read_api_key` will fail. Fallback options: (a) encrypted config file, (b) plaintext `prefs.json` with a warning banner. Neither is in scope here — document as a known gap.

2. **API key revalidation strategy**: Should the app call `validate_key` on startup to detect revoked/expired keys? Current plan: no startup validation (avoids network call on launch). The onboarding modal handles initial validation; re-entry is via Preferences.
