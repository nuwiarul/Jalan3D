mod config;
mod error;
mod geocode;
mod models;
mod db;
mod routes;
mod state;

use std::net::SocketAddr;
use axum::{Router, routing::{get, post}};
use tower_http::cors::CorsLayer;
use tracing_subscriber::EnvFilter;

use state::AppState;

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
    let pool = {
        // Install SQLx Any drivers (sqlite, postgres, mysql) before connecting
        sqlx::any::install_default_drivers();
        db::pool::create_pool(&config.database_url, &config.database_kind).await
    };
    tracing::info!("Database pool connected");

    // Run migrations
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("Failed to run migrations");
    tracing::info!("Database migrations applied");

    // App state
    let state = AppState {
        pool,
        config: config.clone(),
        geocode_cache: geocode::GeoCache::new(),
    };

    // Build router
    let app = Router::new()
        // Health
        .route("/api/health", get(routes::health::health_check))
        // Reports CRUD
        .route("/api/reports", get(routes::reports::list_reports).post(routes::reports::create_report))
        .route("/api/reports/{id}", get(routes::reports::get_report).put(routes::reports::update_report).delete(routes::reports::delete_report))
        // Upload
        .route("/api/upload", post(routes::upload::upload_photo))
        // Reverse Geocode
        .route("/api/geocode/reverse", get(routes::geocode::reverse))
        .layer(CorsLayer::permissive())
        .with_state(state);

    // Start server
    let addr = SocketAddr::new(
        config.host.parse().expect("Invalid HOST"),
        config.port,
    );
    tracing::info!("Server listening on {addr}");

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
