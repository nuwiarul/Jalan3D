use axum::{
    extract::{Multipart, State},
    http::StatusCode,
    Json,
};
use serde_json::{json, Value};
use std::path::PathBuf;
use uuid::Uuid;

use crate::error::AppError;
use crate::state::AppState;

/// POST /api/upload
pub async fn upload_photo(
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> Result<(StatusCode, Json<Value>), AppError> {
    let upload_dir = PathBuf::from(&state.config.upload_dir);
    tokio::fs::create_dir_all(&upload_dir)
        .await
        .map_err(|e| AppError::Internal(format!("Failed to create upload dir: {e}")))?;

    let mut uploaded = Vec::new();

    while let Ok(Some(field)) = multipart.next_field().await {
        let filename = field
            .file_name()
            .unwrap_or("photo.jpg")
            .to_string();

        let ext = std::path::Path::new(&filename)
            .extension()
            .and_then(|e| e.to_str())
            .unwrap_or("jpg");

        let unique_name = format!("{}.{}", Uuid::new_v4(), ext);
        let save_path = upload_dir.join(&unique_name);

        let data = field
            .bytes()
            .await
            .map_err(|e| AppError::BadRequest(format!("Failed to read upload: {e}")))?;

        tokio::fs::write(&save_path, &data)
            .await
            .map_err(|e| AppError::Internal(format!("Failed to save file: {e}")))?;

        tracing::info!("Uploaded file: {} ({} bytes)", unique_name, data.len());
        uploaded.push(json!({
            "filename": unique_name,
            "size_bytes": data.len(),
            "path": format!("/uploads/{}", unique_name),
        }));
    }

    Ok((
        StatusCode::CREATED,
        Json(json!({ "uploaded": uploaded })),
    ))
}
