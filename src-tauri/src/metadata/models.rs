use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Series {
    pub id: u32,
    pub name: String,
    pub first_air_date: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Episode {
    pub name: String,
    pub season_number: u32,
    pub episode_number: u32,
    pub air_date: Option<String>,
    pub overview: Option<String>,
}
