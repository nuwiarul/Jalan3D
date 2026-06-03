package com.jalan3d.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ReportApi {

    // Health
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>

    // Reports CRUD
    @GET("api/reports")
    suspend fun listReports(): Response<List<ReportResponse>>

    @GET("api/reports/{id}")
    suspend fun getReport(@Path("id") id: String): Response<ReportResponse>

    @POST("api/reports")
    suspend fun createReport(@Body request: CreateReportRequest): Response<ReportResponse>

    @PUT("api/reports/{id}")
    suspend fun updateReport(
        @Path("id") id: String,
        @Body request: UpdateReportRequest
    ): Response<ReportResponse>

    @DELETE("api/reports/{id}")
    suspend fun deleteReport(@Path("id") id: String): Response<Unit>

    // Upload
    @Multipart
    @POST("api/upload")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<UploadResponse>

    // Geocode
    @GET("api/geocode/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<GeocodeResponse>
}
