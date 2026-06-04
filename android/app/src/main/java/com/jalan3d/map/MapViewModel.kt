package com.jalan3d.map

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jalan3d.data.ExtrusionPipeline
import com.jalan3d.data.Report
import com.jalan3d.data.ReportRepository
import com.jalan3d.data.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val submitError: String? = null,
    // GPS location
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val hasGpsFix: Boolean = false,
    val isGpsCentered: Boolean = false,
    // Reports from API
    val reports: List<Report> = emptyList(),
    val isLoadingReports: Boolean = false,
    val reportsError: String? = null,
    // 3D extrusion data
    val extrusionGeoJson: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val repository = ReportRepository()

    fun onMapReady() {
        _uiState.value = _uiState.value.copy(isMapReady = true)
    }

    fun updateCamera(camera: CameraPosition) {
        _uiState.value = _uiState.value.copy(camera = camera)
    }

    fun setCurrentLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            currentLat = lat,
            currentLng = lng,
            hasGpsFix = true
        )
    }

    fun markGpsCentered() {
        _uiState.value = _uiState.value.copy(isGpsCentered = true)
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
            val result = repository.reverseGeocode(lat, lng)
            result.onSuccess { address ->
                _uiState.value = _uiState.value.copy(
                    tappedAddress = address.ifBlank { null },
                    isLoadingAddress = false,
                    showForm = true
                )
            }.onFailure {
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

    /**
     * Cancel the current report and clear the tapped location.
     * This makes the "Laporkan Lokasi Saya" button reappear.
     */
    fun cancelReport() {
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

    /**
     * Clear tapped location after successful submit.
     * Does NOT reset isGpsCentered — GPS centering only happens once.
     */
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

    // ─── Load reports from backend ───

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingReports = true,
                reportsError = null
            )
            val result = repository.getReports()
            result.onSuccess { reports ->
                val extrusionGeoJson = ExtrusionPipeline.reportsToGeoJson(reports)
                _uiState.value = _uiState.value.copy(
                    reports = reports,
                    extrusionGeoJson = extrusionGeoJson,
                    isLoadingReports = false,
                    reportsError = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingReports = false,
                    reportsError = error.message ?: "Gagal muat laporan"
                )
            }
        }
    }

    // ─── Submit report ───

    fun submitReport(context: Context) {
        val lat = _uiState.value.tappedLat ?: return
        val lng = _uiState.value.tappedLng ?: return
        val severity = Severity.fromKey(_uiState.value.selectedSeverity)
        val description = _uiState.value.description
        val photoUri = _uiState.value.photoUri

        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)

        viewModelScope.launch {
            val result = repository.createReport(
                lat = lat,
                lng = lng,
                severity = severity,
                address = _uiState.value.tappedAddress,
                description = description,
                photoUri = photoUri,
                context = context
            )
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true,
                    showForm = false
                )
                // Reload reports to show the new one on map
                loadReports()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = error.message ?: "Gagal kirim laporan"
                )
            }
        }
    }
}
