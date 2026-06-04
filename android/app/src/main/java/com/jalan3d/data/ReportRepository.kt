package com.jalan3d.data

import android.content.Context
import android.net.Uri
import com.jalan3d.data.api.ApiClient
import com.jalan3d.data.api.CreateReportRequest
import com.jalan3d.data.api.GeocodeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    companion object {
        /** Max dimension (width or height) for uploaded images. */
        private const val MAX_IMAGE_DIMENSION = 1920
        /** JPEG quality for compressed uploads. */
        private const val JPEG_QUALITY = 85
    }

    /**
     * Convert a photo URI to a resized JPEG file.
     * Decodes the image, scales down to [MAX_IMAGE_DIMENSION] on the
     * longest side, and compresses as JPEG at [JPEG_QUALITY] quality.
     * This keeps uploads small and fast.
     *
     * Heavy work (bitmap decode + compress) runs on [Dispatchers.IO]
     * to avoid blocking the main thread.
     */
    private suspend fun uriToFile(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")

        try {
            // Step 1: Decode bounds only to compute sample size
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }

            // Step 2: Calculate sample size to fit within MAX_IMAGE_DIMENSION
            val sampleSize = calculateSampleSize(
                options.outWidth, options.outHeight, MAX_IMAGE_DIMENSION
            )

            // Step 3: Decode with sample size
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            // Step 4: Compress and save
            if (bitmap != null) {
                file.outputStream().use { output ->
                    bitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG,
                        JPEG_QUALITY,
                        output
                    )
                }
                bitmap.recycle()
            } else {
                // Fallback: copy raw bytes if decoding fails
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback: copy raw bytes
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        file
    }

    /**
     * Calculate the largest sample size that keeps the image within [maxDimension].
     * Sample size must be a power of 2.
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
