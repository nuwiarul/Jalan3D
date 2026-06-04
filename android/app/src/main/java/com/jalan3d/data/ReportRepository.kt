package com.jalan3d.data

import android.content.Context
import android.net.Uri
import com.jalan3d.data.api.ApiClient
import com.jalan3d.data.api.CreateReportRequest
import com.jalan3d.data.api.GeocodeResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Repository layer for Jalan3D reports.
 *
 * Wraps API calls, maintains an in-memory cache of reports,
 * and maps DTOs to domain models so the rest of the app
 * doesn't depend on network types.
 */
class ReportRepository {

    // ─── In-memory cache ───

    private var cachedReports: List<Report>? = null

    /**
     * Fetch reports from the API and update the in-memory cache.
     * Returns [Result.success] with the fetched list, or [Result.failure].
     */
    suspend fun getReports(forceRefresh: Boolean = false): Result<List<Report>> {
        // Return cached data if available and no force refresh
        if (!forceRefresh && cachedReports != null) {
            return Result.success(cachedReports!!)
        }

        return try {
            val response = ApiClient.api.listReports()
            if (response.isSuccessful) {
                val reports = (response.body() ?: emptyList()).map { Report.fromResponse(it) }
                cachedReports = reports
                Result.success(reports)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Gagal muat laporan: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    /**
     * Get a single report by ID. Checks cache first, then falls back to API.
     */
    suspend fun getReport(id: String): Result<Report> {
        // Check cache first
        cachedReports?.firstOrNull { it.id == id }?.let { return Result.success(it) }

        return try {
            val response = ApiClient.api.getReport(id)
            if (response.isSuccessful) {
                val report = response.body()?.let { Report.fromResponse(it) }
                    ?: return Result.failure(Exception("Report not found"))
                Result.success(report)
            } else {
                Result.failure(Exception("Report not found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    /**
     * Submit a new report. Invalidates the cache on success.
     */
    suspend fun createReport(
        lat: Double,
        lng: Double,
        severity: Severity,
        address: String?,
        description: String?,
        photoUri: Uri?,
        context: Context
    ): Result<Report> {
        return try {
            var photoPath: String? = null

            // Upload photo first if present
            if (photoUri != null) {
                val file = uriToFile(context, photoUri)
                val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)

                val uploadResponse = ApiClient.api.uploadPhoto(part)
                if (uploadResponse.isSuccessful) {
                    val body = uploadResponse.body()
                    if (body != null && body.uploaded.isNotEmpty()) {
                        photoPath = body.uploaded.first().path
                    }
                }
            }

            // Create report
            val request = CreateReportRequest(
                lat = lat,
                lng = lng,
                severity = severity.key,
                photoPath = photoPath,
                address = address,
                description = description?.ifBlank { null }
            )

            val response = ApiClient.api.createReport(request)
            if (response.isSuccessful) {
                val report = response.body()?.let { Report.fromResponse(it) }
                    ?: return Result.failure(Exception("Gagal parse response"))
                // Invalidate cache so next getReports fetches fresh data
                cachedReports = null
                Result.success(report)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Gagal kirim laporan: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    /**
     * Delete a report by ID. Invalidates cache on success.
     */
    suspend fun deleteReport(id: String): Result<Unit> {
        return try {
            val response = ApiClient.api.deleteReport(id)
            if (response.isSuccessful) {
                cachedReports = null // Invalidate cache
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal hapus laporan"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    /**
     * Reverse geocode coordinates to an address string.
     */
    suspend fun reverseGeocode(lat: Double, lng: Double): Result<String> {
        return try {
            val response = ApiClient.api.reverseGeocode(lat, lng)
            if (response.isSuccessful) {
                Result.success(response.body()?.address ?: "")
            } else {
                Result.success("") // Non-fatal — just return empty
            }
        } catch (e: Exception) {
            Result.success("") // Non-fatal during network issues
        }
    }

    /**
     * Clear the in-memory cache.
     */
    fun clearCache() {
        cachedReports = null
    }

    // ─── Helpers ───

    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
