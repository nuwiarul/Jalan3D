use std::collections::HashMap;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::Mutex;

use crate::error::AppError;

const NOMINATIM_URL: &str = "https://nominatim.openstreetmap.org/reverse";
const CACHE_TTL: Duration = Duration::from_secs(86400); // 24 hours
const USER_AGENT: &str = "Jalan3D/0.1 (Android road damage reporter)";

#[derive(Clone)]
pub struct GeoCache {
    inner: Arc<Mutex<HashMap<String, CacheEntry>>>,
}

struct CacheEntry {
    address: String,
    expires_at: Instant,
}

impl GeoCache {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

/// Reverse geocode lat/lng via Nominatim, with in-memory cache.
pub async fn reverse_geocode(
    cache: &GeoCache,
    lat: f64,
    lng: f64,
) -> Result<String, AppError> {
    let key = format!("{:.5},{:.5}", lat, lng);

    // Check cache first
    {
        let map = cache.inner.lock().await;
        if let Some(entry) = map.get(&key) {
            if entry.expires_at > Instant::now() {
                tracing::info!("Geocode cache HIT for {}", key);
                return Ok(entry.address.clone());
            }
        }
    }

    // Cache miss — call Nominatim
    tracing::info!("Geocode cache MISS for {} — calling Nominatim", key);

    let url = format!(
        "{}?format=json&lat={}&lon={}&addressdetails=1&zoom=18",
        NOMINATIM_URL, lat, lng
    );

    let client = reqwest::Client::new();
    let resp = client
        .get(&url)
        .header("User-Agent", USER_AGENT)
        .send()
        .await
        .map_err(|e| AppError::Internal(format!("Nominatim request failed: {e}")))?;

    if !resp.status().is_success() {
        return Err(AppError::Internal(format!(
            "Nominatim returned {}",
            resp.status()
        )));
    }

    let body: serde_json::Value = resp
        .json()
        .await
        .map_err(|e| AppError::Internal(format!("Nominatim parse failed: {e}")))?;

    let address = body["display_name"]
        .as_str()
        .unwrap_or("Unknown location")
        .to_string();

    // Store in cache
    {
        let mut map = cache.inner.lock().await;
        map.insert(
            key,
            CacheEntry {
                address: address.clone(),
                expires_at: Instant::now() + CACHE_TTL,
            },
        );
    }

    Ok(address)
}
