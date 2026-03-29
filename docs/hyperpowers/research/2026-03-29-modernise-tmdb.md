# Research: TMDB Client Module (Rust)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Build an async TMDB v3 client in Rust using `reqwest` that replaces the discontinued TheTVDB provider. Handles show search, episode lookup, API key management, and rate limiting.

---

## TMDB v3 API Endpoints

**Search:** `GET /3/search/tv?query=<show>`

Key response fields: `id` (integer — use for episode lookup), `name`, `first_air_date`, `original_language`

**Episode:** `GET /3/tv/{series_id}/season/{season_number}/episode/{episode_number}`

Key response fields: `name` (episode title), `air_date`, `episode_number`, `season_number`, `overview`

**Authentication:** Bearer token in `Authorization` header is preferred over `api_key` query parameter.

**Rate limits:** 50 req/sec, 20 concurrent connections per IP — no difference between free and paid tiers. Returns HTTP 429 on excess.

---

## reqwest: Static Client Pattern

Create ONE `reqwest::Client` at startup and share it via `AppHandle` state. Never create a new client per API call — this destroys connection pooling.

```rust
let client = reqwest::Client::builder()
    .connect_timeout(Duration::from_secs(10))
    .timeout(Duration::from_secs(30))
    .build()?;
app.manage(client);
```

---

## API Key Storage: OS Keychain (`keyring` crate)

```toml
keyring = "3.6"
```

```rust
use keyring::Entry;
let entry = Entry::new("tvrenamer", "tmdb_api_key")?;
entry.set_password(&api_key)?;
let key = entry.get_password()?;
```

Platform mapping: macOS Keychain, Windows Credential Manager, Linux Secret Service. **Do NOT store API key in plaintext `prefs.json`.**

---

## TMDB API Key Onboarding (Resolved)

**3-step non-blocking modal on first launch:**

1. Explain why the key is needed
2. Direct link to `https://www.themoviedb.org/settings/api`
3. Input field + "Test" button that calls `GET /3/authentication`

Validation response: `{"success": true, "status_code": 1, "status_message": "Success."}`

Validate before saving. Show graceful degradation banner if key is absent. Key stored in OS keychain. Do NOT block app usage if skipped.

---

## Rust Module Design

```rust
// src-tauri/src/metadata/provider.rs
pub trait MetadataProvider {
    async fn search_shows(&self, query: &str) -> Result<Vec<ShowResult>, AppError>;
    async fn get_episode(&self, series_id: u64, season: u32, episode: u32) -> Result<Episode, AppError>;
}

// src-tauri/src/metadata/tmdb.rs
pub struct TmdbClient {
    client: reqwest::Client,
    api_key: String,
}

impl MetadataProvider for TmdbClient { ... }
```

---

## Error Handling

| State | User-Facing Message (port verbatim from Java) |
|-------|----------------------------------------------|
| Fetching | `"Downloading ..."` |
| Lookup failed | `"Unable to find show information"` |
| Episode not found | `"Could not get episode for show"` |
| Network timeout | `"Timed out trying to look up"` |
| General download failure | `"Downloading show listings failed. Check internet connection"` |

**Rate limiting (429):** Java treats 429 as generic failure. Rust port must implement exponential backoff.

**API key validation:** Validate at input time (`GET /3/authentication`) before saving — catches typos immediately.

---

## API & Network Edge Cases

1. **TMDB 429 rate limit**: Must implement exponential backoff for 429 responses.
2. **No direct episode ID lookup**: Episodes must be queried by `season_number + episode_number`, not by ID.
3. **Special episodes (Season 0)**: Season 0 contains specials, pilots, and unaired episodes. The episode endpoint works identically — but the parser may not extract season 0 from filenames. Known gap.

---

## Git History Context

- **2017-05-24**: Attempted migration to TheTVDB Swagger/REST API. Vipul Delwadia led this.
- **2017-11-20**: `DiscontinuedApiException` added when TheTVDB announced v1 sunset.
- **Result**: The app has been non-functional for show lookup for some time. The full switch to TMDB replaces all TheTVDB code.

---

## Test Coverage Gaps (Write From Scratch)

- No mock TMDB API responses exist — must create mock fixtures
- No error path tests for API failures
- No rate limit handling tests

---

## Open Questions

1. **`keyring` on headless Linux**: The `keyring` crate requires a Secret Service daemon (e.g., `gnome-keyring`). On headless Linux, this isn't available. Fallback: encrypted config file, or skip keychain and use `prefs.json` with a warning.
2. **API key validation timing**: Validate stored key on every startup (network call on launch) or only when user explicitly re-tests? Current design says validate at entry only.

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| TMDB v3 `GET /3/search/tv` returns `id`, `name`, `first_air_date` | ✅ Valid |
| TMDB rate limits: 50 req/sec, 20 concurrent | ✅ Valid |
| TMDB registration URL: `https://www.themoviedb.org/settings/api` | ✅ Valid |
| `keyring = "3.6"` provides cross-platform keychain | ✅ Valid |
| TMDB `/3/authentication` response includes `status_message` field | ✅ Corrected (3 fields, not 2) |
| `reqwest = "0.13"` (not 0.12) | ✅ Corrected |
| TMDB v3 episode endpoint exact field names | ⚠️ Likely correct, verify against actual response |
