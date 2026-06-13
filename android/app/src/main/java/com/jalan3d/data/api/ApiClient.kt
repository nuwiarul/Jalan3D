package com.jalan3d.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the Jalan3D backend API.
 */
object ApiClient {

    private val bodyLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val headersLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val safeLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val isMultipart = request.body?.contentType()?.type == "multipart"
        if (isMultipart) {
            headersLoggingInterceptor.intercept(chain)
        } else {
            bodyLoggingInterceptor.intercept(chain)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(safeLoggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ReportApi = retrofit.create(ReportApi::class.java)
}

