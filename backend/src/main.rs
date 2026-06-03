mod config;
mod error;
mod routes;
mod models;
mod db;

use std::net::SocketAddr;
use axum::{Router, routing::get};
use tower_http::cors::CorsLayer;
use tracing_subscriber::EnvFilter;
use sqlx::any::AnyPoolOptions;

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

    // Build database pool
    let pool = AnyPoolOptions::new()
        .max_connections(5)
        .connect(&config.database_url)
        .await
        .expect("Failed to create database pool");
    tracing::info!("Database connected");

    // Run migrations
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("Failed to run migrations");

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
    tracing::info!("Server starting on {addr}");

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
