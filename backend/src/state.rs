use crate::config::Config;
use sqlx::AnyPool;

#[derive(Clone)]
pub struct AppState {
    pub pool: AnyPool,
    pub config: Config,
}
