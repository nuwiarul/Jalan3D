package com.jalan3d.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraPosition(
    val latitude: Double = -8.4095,   // Bali default
    val longitude: Double = 115.1889,
    val zoom: Double = 10.0,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0
)

data class MapUiState(
    val camera: CameraPosition = CameraPosition(),
    val isMapReady: Boolean = false
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
}
