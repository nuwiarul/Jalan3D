package com.jalan3d.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jalan3d.data.api.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraPosition(
    val latitude: Double = -8.4095,   // Bali default
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
    val isLoadingAddress: Boolean = false
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
            tappedAddress = null
        )

        // Reverse geocode via backend API
        viewModelScope.launch {
            try {
                val response = ApiClient.api.reverseGeocode(lat, lng)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        tappedAddress = response.body()?.address,
                        isLoadingAddress = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingAddress = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingAddress = false)
            }
        }
    }

    fun clearTappedLocation() {
        _uiState.value = _uiState.value.copy(
            tappedLat = null,
            tappedLng = null,
            tappedAddress = null,
            isLoadingAddress = false
        )
    }
}
