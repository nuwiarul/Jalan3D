package com.jalan3d.data.api

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/reports and GET /api/reports/{id}
 */
data class ReportResponse(
    val id: String,
    val lat: Double,
    val lng: Double,
    val severity: String,
    @SerializedName("photo_path") val photoPath: String?,
    val address: String?,
    val description: String?,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

/**
 * Request body for POST /api/reports
 */
data class CreateReportRequest(
    val lat: Double,
    val lng: Double,
    val severity: String,
    @SerializedName("photo_path") val photoPath: String? = null,
    val address: String? = null,
    val description: String? = null
)

/**
 * Request body for PUT /api/reports/{id}
 */
data class UpdateReportRequest(
    val status: String,
    val address: String? = null,
    val description: String? = null
)

/**
 * Response from POST /api/upload
 */
data class UploadResponse(
    val uploaded: List<UploadedFile>
)

data class UploadedFile(
    val filename: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
    val path: String
)

/**
 * Response from GET /api/geocode/reverse
 */
data class GeocodeResponse(
    val lat: Double,
    val lng: Double,
    val address: String
)

/**
 * Response from GET /api/health
 */
data class HealthResponse(
    val status: String,
    val version: String
)
