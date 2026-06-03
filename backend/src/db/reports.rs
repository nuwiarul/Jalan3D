use crate::error::AppError;
use crate::models::report::{CreateReportRequest, Report, UpdateReportRequest};
use sqlx::AnyPool;

pub async fn list_reports(pool: &AnyPool) -> Result<Vec<Report>, AppError> {
    Ok(
        sqlx::query_as::<_, Report>("SELECT * FROM reports ORDER BY created_at DESC")
            .fetch_all(pool)
            .await?,
    )
}

pub async fn get_report(pool: &AnyPool, id: &str) -> Result<Report, AppError> {
    Ok(
        sqlx::query_as::<_, Report>("SELECT * FROM reports WHERE id = ?")
            .bind(id)
            .fetch_one(pool)
            .await?,
    )
}

pub async fn create_report(
    pool: &AnyPool,
    req: CreateReportRequest,
    id: &str,
    now: &str,
) -> Result<Report, AppError> {
    sqlx::query(
        "INSERT INTO reports (id, lat, lng, severity, photo_path, address, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?)",
    )
    .bind(id)
    .bind(req.lat)
    .bind(req.lng)
    .bind(&req.severity)
    .bind(&req.photo_path)
    .bind(&req.address)
    .bind(&req.description)
    .bind(now)
    .bind(now)
    .execute(pool)
    .await?;

    get_report(pool, id).await
}

pub async fn update_report(
    pool: &AnyPool,
    id: &str,
    req: UpdateReportRequest,
    now: &str,
) -> Result<Report, AppError> {
    sqlx::query(
        "UPDATE reports SET status = ?, address = COALESCE(?, address), description = COALESCE(?, description), updated_at = ? WHERE id = ?",
    )
    .bind(&req.status)
    .bind(&req.address)
    .bind(&req.description)
    .bind(now)
    .bind(id)
    .execute(pool)
    .await?;

    get_report(pool, id).await
}

pub async fn delete_report(pool: &AnyPool, id: &str) -> Result<(), AppError> {
    let result = sqlx::query("DELETE FROM reports WHERE id = ?")
        .bind(id)
        .execute(pool)
        .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound(format!("Report {id} not found")));
    }

    Ok(())
}
