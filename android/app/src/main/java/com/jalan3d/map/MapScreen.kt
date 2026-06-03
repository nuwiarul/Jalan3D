package com.jalan3d.map

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jalan3d.camera.PhotoCapture
import com.jalan3d.ui.ReportFormSheet
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mapViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Camera launcher
    var photoUri: Uri? = null
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            mapViewModel.setPhotoUri(photoUri)
        }
    }

    // Initialize MapLibre once
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // Track MapLibreMap instance
    var mapLibreMap: MapLibreMap? = null

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map

            // Register tap listener
            map.addOnMapClickListener { latLng ->
                mapViewModel.onMapTapped(latLng.latitude, latLng.longitude)
                true
            }

            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                MapMarkers.initialize(style)
                mapViewModel.onMapReady()

                val position = uiState.camera
                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        org.maplibre.android.camera.CameraPosition.Builder()
                            .target(
                                org.maplibre.android.geometry.LatLng(
                                    position.latitude,
                                    position.longitude
                                )
                            )
                            .zoom(position.zoom)
                            .bearing(position.bearing)
                            .tilt(position.tilt)
                            .build()
                    ),
                    2000
                )
            }
        }
    }

    // Update marker when tapped location changes
    LaunchedEffect(uiState.tappedLat, uiState.tappedLng) {
        val lat = uiState.tappedLat ?: return@LaunchedEffect
        val lng = uiState.tappedLng ?: return@LaunchedEffect
        mapLibreMap?.style?.let { style ->
            MapMarkers.updatePosition(style, lat, lng)
        }
    }

    // Show success snackbar
    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            snackbarHostState.showSnackbar("Laporan berhasil dikirim! ✅")
            mapViewModel.clearTappedLocation()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Address overlay when location is tapped
        if (uiState.tappedAddress != null && !uiState.showForm) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Text(
                    text = uiState.tappedAddress!!,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Loading indicator while geocoding
        if (uiState.isLoadingAddress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // Report form bottom sheet
        if (uiState.showForm) {
            ReportFormSheet(
                address = uiState.tappedAddress,
                selectedSeverity = uiState.selectedSeverity,
                photoUri = uiState.photoUri,
                isSubmitting = uiState.isSubmitting,
                submitError = uiState.submitError,
                submitSuccess = uiState.submitSuccess,
                onDismiss = { mapViewModel.hideForm() },
                onSeveritySelected = { mapViewModel.selectSeverity(it) },
                onDescriptionChange = { mapViewModel.updateDescription(it) },
                onTakePhoto = {
                    photoUri = PhotoCapture.createPhotoUri(context)
                    photoUri?.let { cameraLauncher.launch(it) }
                },
                onSubmit = { mapViewModel.submitReport(context) }
            )
        }
    }
}
