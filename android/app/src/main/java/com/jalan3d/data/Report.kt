package com.jalan3d.data

import com.jalan3d.data.api.ReportResponse

/**
 * Domain model for a road damage report.
 * Decoupled from API DTOs so the rest of the app doesn't depend on network types.
 */
data class Report(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Severity,
    val photoPath: String?,
    val address: String?,
    val description: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromResponse(response: ReportResponse): Report {
            return Report(
                id = response.id,
                latitude = response.lat,
                longitude = response.lng,
                severity = Severity.fromKey(response.severity),
                photoPath = response.photoPath,
                address = response.address,
                description = response.description,
                status = response.status,
                createdAt = response.createdAt,
                updatedAt = response.updatedAt
            )
        }
    }
}
