mod config;
mod error;
mod routes;
mod models;
mod db;

use std::net::SocketAddr;
use axum::{Router, routing::get};
use tower_http::cors::CorsLayer;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() {
    // Load .env
    dotenvy::dotenv().ok();

    // Initialize tracing
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env()
            .unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    // Load config
    let config = config::Config::from_env();

    tracing::info!(
        "Starting server with {}",
        config.database_kind.display_name()
    );

    // Build database pool (auto-detects SQLite / PostgreSQL / MariaDB)
    let pool = db::pool::create_pool(&config.database_url, &config.database_kind).await;
    tracing::info!("Database pool connected");

    // Run migrations
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("Failed to run migrations");
    tracing::info!("Database migrations applied");

    // Build router
    let app = Router::new()
        .route("/api/health", get(routes::health::health_check))
        .layer(CorsLayer::permissive())
        .with_state(pool);

    // Start server
    let addr = SocketAddr::new(
        config.host.parse().expect("Invalid HOST"),
        config.port,
    );
    tracing::info!("Server listening on {addr}");

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
