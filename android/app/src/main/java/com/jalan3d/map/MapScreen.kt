package com.jalan3d.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jalan3d.ui.ReportDetailSheet
import com.jalan3d.ui.ReportFormSheet
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"
private const val CONTROL_STEP = 15.0 // degrees for tilt & bearing adjustments

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

    // Lifecycle management — follow activity lifecycle for GL context
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load map style and set up listeners
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapLibreMap = map

            // Register tap listener: marker tap → fly to + detail, else location picker
            map.addOnMapClickListener { latLng ->
                // Check if tap hit a report marker
                val screenPoint = map.projection.toScreenLocation(latLng)
                val hitBox = RectF(
                    screenPoint.x - 15f, screenPoint.y - 15f,
                    screenPoint.x + 15f, screenPoint.y + 15f
                )
                val features = map.queryRenderedFeatures(hitBox, MapMarkers.REPORTS_LAYER_ID)

                if (features.isNotEmpty()) {
                    // Tap on a report marker → fly to it + show detail
                    val feature = features.first()
                    val reportId = feature.properties()?.get("id")?.asString
                    val report = reportId?.let { id ->
                        mapViewModel.uiState.value.reports.firstOrNull { it.id == id }
                    }
                    if (report != null) {
                        mapViewModel.selectReport(report)
                    }
                    Map3DController.flyToLocation(map, latLng.latitude, latLng.longitude)
                } else {
                    // Tap on empty map → location picker
                    mapViewModel.onMapTapped(latLng.latitude, latLng.longitude)
                }
                true
            }

            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                MapMarkers.initialize(style)
                ExtrusionLayer.initialize(style)
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
        if (uiState.isMapReady) {
            mapViewModel.loadReports()
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

    // ─── Update report markers when reports list changes ───
    LaunchedEffect(uiState.reports) {
        mapLibreMap?.style?.let { style ->
            MapMarkers.updateReports(style, uiState.reports)
        }
    }

    // ─── Update 3D extrusions when extrusion data changes ───
    LaunchedEffect(uiState.extrusionGeoJson) {
        val geoJson = uiState.extrusionGeoJson ?: return@LaunchedEffect
        mapLibreMap?.style?.let { style ->
            ExtrusionLayer.updateExtrusions(style, geoJson)
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

        // ── Snackbar ──
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ── Address overlay (when location is tapped but form not yet shown) ──
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

        // ── Loading indicator while geocoding ──
        if (uiState.isLoadingAddress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // ── Map controls panel (right side) ──
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Compass — reset bearing to 0
            MapControlButton(onClick = { resetBearing(mapLibreMap) }) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Reset bearing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tilt up
            MapControlButton(onClick = { adjustTilt(mapLibreMap, CONTROL_STEP) }) {
                Icon(Icons.Default.ArrowUpward, "Tilt naik", modifier = Modifier.size(20.dp))
            }

            // Zoom +
            MapControlButton(onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 300) }) {
                Icon(Icons.Default.Add, "Zoom in", modifier = Modifier.size(20.dp))
            }

            // Zoom -
            MapControlButton(onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 300) }) {
                Icon(Icons.Default.Remove, "Zoom out", modifier = Modifier.size(20.dp))
            }

            // Tilt down
            MapControlButton(onClick = { adjustTilt(mapLibreMap, -CONTROL_STEP) }) {
                Icon(Icons.Default.ArrowDownward, "Tilt turun", modifier = Modifier.size(20.dp))
            }

            // Bearing left / right
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapControlButton(onClick = { adjustBearing(mapLibreMap, -CONTROL_STEP) }) {
                    Icon(Icons.Default.ChevronLeft, "Bearing kiri", modifier = Modifier.size(20.dp))
                }
                MapControlButton(onClick = { adjustBearing(mapLibreMap, CONTROL_STEP) }) {
                    Icon(Icons.Default.ChevronRight, "Bearing kanan", modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── "Laporkan Lokasi Saya" button ──
        // Shown only when GPS is active and no tap-dropped marker is on the map
        if (uiState.hasGpsFix && !uiState.showForm && uiState.tappedLat == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp), // above snackbar
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                TextButton(
                    onClick = {
                        val lat = uiState.currentLat ?: return@TextButton
                        val lng = uiState.currentLng ?: return@TextButton
                        mapViewModel.onMapTapped(lat, lng)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Laporkan Lokasi Saya",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ── Report form bottom sheet ──
        // Always rendered (not inside `if`) so ModalBottomSheet can animate
        // its scrim out properly. Visibility controlled by `isVisible` param.
        ReportFormSheet(
            isVisible = uiState.showForm,
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

        // ── Report detail sheet (tap marker) ──
        if (uiState.selectedReport != null) {
            ReportDetailSheet(
                report = uiState.selectedReport!!,
                onDismiss = { mapViewModel.dismissReport() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─── Small circular map control button ───

@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

// ─── Camera helpers ───

private fun adjustTilt(map: MapLibreMap?, delta: Double) {
    map?.let { m ->
        val pos = m.cameraPosition
        val newTilt = (pos.tilt + delta).coerceIn(0.0, 80.0)
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                org.maplibre.android.camera.CameraPosition.Builder()
                    .target(pos.target)
                    .zoom(pos.zoom)
                    .bearing(pos.bearing)
                    .tilt(newTilt)
                    .build()
            ),
            300
        )
    }
}

private fun adjustBearing(map: MapLibreMap?, delta: Double) {
    map?.let { m ->
        val pos = m.cameraPosition
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                org.maplibre.android.camera.CameraPosition.Builder()
                    .target(pos.target)
                    .zoom(pos.zoom)
                    .bearing(pos.bearing + delta)
                    .tilt(pos.tilt)
                    .build()
            ),
            300
        )
    }
}

private fun resetBearing(map: MapLibreMap?) {
    map?.let { m ->
        val pos = m.cameraPosition
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                org.maplibre.android.camera.CameraPosition.Builder()
                    .target(pos.target)
                    .zoom(pos.zoom)
                    .bearing(0.0)
                    .tilt(pos.tilt)
                    .build()
            ),
            300
        )
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
