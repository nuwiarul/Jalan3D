package com.jalan3d.map

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jalan3d.data.api.ApiClient
import com.jalan3d.data.api.CreateReportRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class CameraPosition(
    val latitude: Double = -8.4095,
    val longitude: Double = 115.1889,
    val zoom: Double = 10.0,
    val bearing: Double = 0.0,
    val tilt: Double = 60.0
)

data class MapUiState(
    val camera: CameraPosition = CameraPosition(),
    val isMapReady: Boolean = false,
    val tappedLat: Double? = null,
    val tappedLng: Double? = null,
    val tappedAddress: String? = null,
    val isLoadingAddress: Boolean = false,
    val showForm: Boolean = false,
    val selectedSeverity: String = "ringan",
    val description: String = "",
    val photoUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onMapReady() {
        _uiState.value = _uiState.value.copy(isMapReady = true)
    }

    fun updateCamera(camera: CameraPosition) {
        _uiState.value = _uiState.value.copy(camera = camera)
    }

    fun onMapTapped(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            tappedLat = lat,
            tappedLng = lng,
            isLoadingAddress = true,
            tappedAddress = null,
            showForm = false,
            submitSuccess = false,
            submitError = null
        )

        viewModelScope.launch {
            try {
                val response = ApiClient.api.reverseGeocode(lat, lng)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        tappedAddress = response.body()?.address,
                        isLoadingAddress = false,
                        showForm = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAddress = false,
                        showForm = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAddress = false,
                    showForm = true
                )
            }
        }
    }

    fun selectSeverity(severity: String) {
        _uiState.value = _uiState.value.copy(selectedSeverity = severity)
    }

    fun updateDescription(text: String) {
        _uiState.value = _uiState.value.copy(description = text)
    }

    fun setPhotoUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }

    fun hideForm() {
        _uiState.value = _uiState.value.copy(showForm = false)
    }

    fun clearTappedLocation() {
        _uiState.value = _uiState.value.copy(
            tappedLat = null,
            tappedLng = null,
            tappedAddress = null,
            isLoadingAddress = false,
            showForm = false,
            selectedSeverity = "ringan",
            description = "",
            photoUri = null,
            submitSuccess = false,
            submitError = null
        )
    }

    fun submitReport(context: Context) {
        val lat = _uiState.value.tappedLat ?: return
        val lng = _uiState.value.tappedLng ?: return
        val severity = _uiState.value.selectedSeverity
        val description = _uiState.value.description
        val photoUri = _uiState.value.photoUri

        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)

        viewModelScope.launch {
            try {
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
                    severity = severity,
                    photoPath = photoPath,
                    address = _uiState.value.tappedAddress,
                    description = description.ifBlank { null }
                )

                val response = ApiClient.api.createReport(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        showForm = false
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = "Gagal: $errorBody"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = "Error: ${e.message}"
                )
            }
        }
    }

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
