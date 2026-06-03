use axum::{
    extract::{Multipart, State},
    http::StatusCode,
    Json,
};
use image::codecs::jpeg::JpegEncoder;
use image::{imageops::FilterType, DynamicImage};
use serde_json::{json, Value};
use std::io::Cursor;
use std::path::PathBuf;
use uuid::Uuid;

use crate::error::AppError;
use crate::state::AppState;

const MAX_DIMENSION: u32 = 1920;
const JPEG_QUALITY: u8 = 85;

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
        let data = field
            .bytes()
            .await
            .map_err(|e| AppError::BadRequest(format!("Failed to read upload: {e}")))?;

        let unique_name = format!("{}.jpg", Uuid::new_v4());
        let save_path = upload_dir.join(&unique_name);

        // Try to decode as image and resize
        let saved_size = match resize_image(&data) {
            Ok(resized) => {
                tokio::fs::write(&save_path, &resized)
                    .await
                    .map_err(|e| AppError::Internal(format!("Failed to save file: {e}")))?;
                resized.len()
            }
            Err(_) => {
                // Not a supported image format — save raw bytes with original extension
                // but we still use .jpg extension as fallback
                tokio::fs::write(&save_path, &data)
                    .await
                    .map_err(|e| AppError::Internal(format!("Failed to save file: {e}")))?;
                data.len()
            }
        };

        tracing::info!(
            "Uploaded: {} ({} bytes)",
            unique_name,
            saved_size
        );
        uploaded.push(json!({
            "filename": unique_name,
            "size_bytes": saved_size,
            "path": format!("/uploads/{}", unique_name),
        }));
    }

    Ok((
        StatusCode::CREATED,
        Json(json!({ "uploaded": uploaded })),
    ))
}

/// Decode image, resize to max 1920px on longest side, re-encode as JPEG.
fn resize_image(data: &[u8]) -> Result<Vec<u8>, AppError> {
    let img = image::load_from_memory(data)
        .map_err(|_| AppError::BadRequest("Unsupported image format".to_string()))?;

    let (w, h) = (img.width(), img.height());

    // Check if resize is needed
    let resized = if w > MAX_DIMENSION || h > MAX_DIMENSION {
        let ratio = if w > h {
            MAX_DIMENSION as f64 / w as f64
        } else {
            MAX_DIMENSION as f64 / h as f64
        };
        let new_w = (w as f64 * ratio) as u32;
        let new_h = (h as f64 * ratio) as u32;
        tracing::info!("Resizing image from {}x{} to {}x{}", w, h, new_w, new_h);
        DynamicImage::ImageRgba8(
            img.resize_exact(new_w, new_h, FilterType::Lanczos3).into_rgba8(),
        )
    } else {
        img
    };

    let mut output = Cursor::new(Vec::new());
    let mut encoder = JpegEncoder::new_with_quality(&mut output, JPEG_QUALITY);
    let rgb = resized.to_rgb8();
    encoder
        .encode(rgb.as_raw(), rgb.width(), rgb.height(), image::ExtendedColorType::Rgb8)
        .map_err(|e| AppError::Internal(format!("JPEG encode failed: {e}")))?;

    let output_bytes = output.into_inner();

    // If JPEG is larger than original (e.g. very small PNG), keep original
    if output_bytes.len() > data.len() {
        tracing::info!("JPEG re-encode larger than original ({} vs {}), keeping original", output_bytes.len(), data.len());
        return Ok(Vec::from(data));
    }

    Ok(output_bytes)
}
