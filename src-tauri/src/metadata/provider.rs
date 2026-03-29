// MetadataProvider trait — implemented by TmdbProvider
// Uses native async-in-trait (Rust 1.75+, required by Tauri's rust-version = "1.77")
use crate::errors::AppError;
use super::models::{Episode, Series};

pub trait MetadataProvider: Send + Sync {
    async fn search_series(&self, query: &str) -> Result<Vec<Series>, AppError>;
    async fn get_episode(
        &self,
        series_id: u32,
        season: u32,
        episode: u32,
    ) -> Result<Episode, AppError>;
}
