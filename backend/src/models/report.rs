use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Report {
    pub id: String,
    pub lat: f64,
    pub lng: f64,
    pub severity: String,
    pub photo_path: Option<String>,
    pub address: Option<String>,
    pub description: Option<String>,
    pub status: String,
    pub created_at: String,
    pub updated_at: String,
}

#[derive(Debug, Deserialize)]
pub struct CreateReportRequest {
    pub lat: f64,
    pub lng: f64,
    pub severity: String,
    pub photo_path: Option<String>,
    pub address: Option<String>,
    pub description: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateReportRequest {
    pub status: String,
    pub address: Option<String>,
    pub description: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct ReportResponse {
    pub id: String,
    pub lat: f64,
    pub lng: f64,
    pub severity: String,
    pub photo_path: Option<String>,
    pub address: Option<String>,
    pub description: Option<String>,
    pub status: String,
    pub created_at: String,
    pub updated_at: String,
}

impl From<Report> for ReportResponse {
    fn from(r: Report) -> Self {
        Self {
            id: r.id,
            lat: r.lat,
            lng: r.lng,
            severity: r.severity,
            photo_path: r.photo_path,
            address: r.address,
            description: r.description,
            status: r.status,
            created_at: r.created_at,
            updated_at: r.updated_at,
        }
    }
}
