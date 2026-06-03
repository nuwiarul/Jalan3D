package com.jalan3d.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jalan3d.ui.ReportFormSheet
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mapViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Track MapLibreMap instance for camera updates
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    // Pending photo URI for after camera permission is granted
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // ─── Camera launcher (system camera) ───
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoUri != null) {
            mapViewModel.setPhotoUri(pendingPhotoUri)
        }
        pendingPhotoUri = null
    }

    // ─── Camera permission launcher ───
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingPhotoUri != null) {
            cameraLauncher.launch(pendingPhotoUri!!)
        } else if (!granted) {
            pendingPhotoUri = null
        }
    }

    // ─── Location permission launcher ───
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getCurrentLocation(context, fusedLocationClient, mapViewModel)
        }
    }

    // ─── Initialize MapLibre ───
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    // Load map style and set up listeners
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map

            // Register tap listener for location picker
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

    // ─── Auto-center to GPS on map ready + permission ───
    LaunchedEffect(uiState.isMapReady) {
        if (uiState.isMapReady && !uiState.isGpsCentered) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                getCurrentLocation(context, fusedLocationClient, mapViewModel)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // ─── Animate camera to GPS position once fix is obtained ───
    LaunchedEffect(uiState.hasGpsFix, uiState.currentLat, uiState.currentLng) {
        if (uiState.hasGpsFix && !uiState.isGpsCentered) {
            val lat = uiState.currentLat ?: return@LaunchedEffect
            val lng = uiState.currentLng ?: return@LaunchedEffect
            val map = mapLibreMap ?: return@LaunchedEffect

            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    org.maplibre.android.camera.CameraPosition.Builder()
                        .target(org.maplibre.android.geometry.LatLng(lat, lng))
                        .zoom(15.0)
                        .bearing(0.0)
                        .tilt(60.0)
                        .build()
                ),
                2000
            )
            mapViewModel.markGpsCentered()
        }
    }

    // ─── Update marker when tapped location changes ───
    LaunchedEffect(uiState.tappedLat, uiState.tappedLng) {
        val lat = uiState.tappedLat ?: return@LaunchedEffect
        val lng = uiState.tappedLng ?: return@LaunchedEffect
        mapLibreMap?.style?.let { style ->
            MapMarkers.updatePosition(style, lat, lng)
        }
    }

    // ─── Show success snackbar ───
    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            snackbarHostState.showSnackbar("Laporan berhasil dikirim! ✅")
            mapViewModel.clearTappedLocation()
        }
    }

    // ─── UI ───
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Address overlay when location is tapped but form not yet shown
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
                onDismiss = { mapViewModel.cancelReport() },
                onSeveritySelected = { mapViewModel.selectSeverity(it) },
                onDescriptionChange = { mapViewModel.updateDescription(it) },
                onTakePhoto = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    val uri = com.jalan3d.camera.PhotoCapture.createPhotoUri(context)
                    pendingPhotoUri = uri

                    if (hasCameraPermission) {
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSubmit = { mapViewModel.submitReport(context) }
            )
        }
    }
}

/**
 * Get the last known location via FusedLocationProviderClient
 * and update the ViewModel.
 */
private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: MapViewModel
) {
    try {
        @SuppressLint("MissingPermission")
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                viewModel.setCurrentLocation(location.latitude, location.longitude)
            }
        }
    } catch (_: SecurityException) {
        // Permission was revoked between check and call — do nothing
    }
}
