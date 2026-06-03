use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use uuid::Uuid;
use chrono::Utc;

use crate::db;
use crate::error::AppError;
use crate::models::report::{CreateReportRequest, ReportResponse, UpdateReportRequest};
use crate::state::AppState;

/// GET /api/reports
pub async fn list_reports(
    State(state): State<AppState>,
) -> Result<Json<Vec<ReportResponse>>, AppError> {
    let reports = db::reports::list_reports(&state.pool).await?;
    Ok(Json(reports.into_iter().map(ReportResponse::from).collect()))
}

/// GET /api/reports/:id
pub async fn get_report(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<Json<ReportResponse>, AppError> {
    let report = db::reports::get_report(&state.pool, &id).await
        .map_err(|_| AppError::NotFound(format!("Report {id} not found")))?;
    Ok(Json(ReportResponse::from(report)))
}

/// POST /api/reports
pub async fn create_report(
    State(state): State<AppState>,
    Json(req): Json<CreateReportRequest>,
) -> Result<(StatusCode, Json<ReportResponse>), AppError> {
    // Validate severity
    let valid_severities = ["ringan", "sedang", "berat", "kritis"];
    if !valid_severities.contains(&req.severity.as_str()) {
        return Err(AppError::BadRequest(format!(
            "Invalid severity: {}. Must be one of: ringan, sedang, berat, kritis",
            req.severity
        )));
    }

    let id = Uuid::new_v4().to_string();
    let now = Utc::now().to_rfc3339();

    let report = db::reports::create_report(&state.pool, req, &id, &now).await?;
    Ok((StatusCode::CREATED, Json(ReportResponse::from(report))))
}

/// PUT /api/reports/:id
pub async fn update_report(
    State(state): State<AppState>,
    Path(id): Path<String>,
    Json(req): Json<UpdateReportRequest>,
) -> Result<Json<ReportResponse>, AppError> {
    // Validate status
    let valid_statuses = ["pending", "verified", "fixed"];
    if !valid_statuses.contains(&req.status.as_str()) {
        return Err(AppError::BadRequest(format!(
            "Invalid status: {}. Must be one of: pending, verified, fixed",
            req.status
        )));
    }

    let now = Utc::now().to_rfc3339();
    let report = db::reports::update_report(&state.pool, &id, req, &now).await?;
    Ok(Json(ReportResponse::from(report)))
}

/// DELETE /api/reports/:id
pub async fn delete_report(
    State(state): State<AppState>,
    Path(id): Path<String>,
) -> Result<StatusCode, AppError> {
    db::reports::delete_report(&state.pool, &id).await?;
    Ok(StatusCode::NO_CONTENT)
}
