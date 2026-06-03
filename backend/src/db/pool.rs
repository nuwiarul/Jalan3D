use sqlx::any::AnyPoolOptions;
use sqlx::AnyPool;

pub async fn create_pool(database_url: &str) -> AnyPool {
    AnyPoolOptions::new()
        .max_connections(5)
        .connect(database_url)
        .await
        .expect("Failed to create database pool")
}
