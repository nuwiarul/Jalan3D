use crate::config::DatabaseKind;
use sqlx::AnyPool;
use sqlx::any::AnyPoolOptions;
use tracing::info;

pub async fn create_pool(database_url: &str, kind: &DatabaseKind) -> AnyPool {
    let pool_size = if kind.is_production() { 10 } else { 5 };

    info!(
        "Database: {} — URL scheme detected, pool size: {}",
        kind.display_name(),
        pool_size,
    );

    AnyPoolOptions::new()
        .max_connections(pool_size)
        .connect(database_url)
        .await
        .expect("Failed to create database pool")
}
