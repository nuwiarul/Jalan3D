use axum::{
    extract::{Query, State},
    Json,
};
use serde::Deserialize;
use serde_json::{json, Value};

use crate::error::AppError;
use crate::geocode;
use crate::state::AppState;

#[derive(Debug, Deserialize)]
pub struct ReverseParams {
    lat: f64,
    lng: f64,
}

/// GET /api/geocode/reverse?lat=-8.4095&lng=115.1889
pub async fn reverse(
    State(state): State<AppState>,
    Query(params): Query<ReverseParams>,
) -> Result<Json<Value>, AppError> {
    let address = geocode::reverse_geocode(&state.geocode_cache, params.lat, params.lng).await?;

    Ok(Json(json!({
        "lat": params.lat,
        "lng": params.lng,
        "address": address,
    })))
}
