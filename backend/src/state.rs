use crate::config::Config;
use crate::geocode::GeoCache;
use sqlx::AnyPool;

#[derive(Clone)]
pub struct AppState {
    pub pool: AnyPool,
    pub config: Config,
    pub geocode_cache: GeoCache,
}
