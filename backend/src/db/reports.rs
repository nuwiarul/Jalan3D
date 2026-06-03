use crate::error::AppError;
use crate::models::report::Report;
use sqlx::AnyPool;

pub async fn list_reports(pool: &AnyPool) -> Result<Vec<Report>, AppError> {
    Ok(sqlx::query_as::<_, Report>("SELECT * FROM reports ORDER BY created_at DESC")
        .fetch_all(pool).await?)
}
