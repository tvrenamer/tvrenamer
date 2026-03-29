// TmdbProvider — TMDB v3 REST API client
// API key fetched from OS keychain via keyring crate on each call (NOT stored in AppState)
// Implementation: metadata plan
use crate::errors::AppError;
use super::provider::MetadataProvider;

pub struct TmdbProvider {
    pub(crate) client: reqwest::Client,
}

impl TmdbProvider {
    pub fn new(client: reqwest::Client) -> Self {
        Self { client }
    }
}

impl MetadataProvider for TmdbProvider {
    async fn search_series(&self, _query: &str) -> Result<Vec<super::models::Series>, AppError> {
        unimplemented!("implement in metadata plan")
    }

    async fn get_episode(
        &self,
        _series_id: u32,
        _season: u32,
        _episode: u32,
    ) -> Result<super::models::Episode, AppError> {
        unimplemented!("implement in metadata plan")
    }
}
