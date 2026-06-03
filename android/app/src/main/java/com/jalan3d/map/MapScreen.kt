package com.jalan3d.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) {
                mapViewModel.onMapReady()

                // Set default camera to Bali
                val position = uiState.camera
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        org.maplibre.android.geometry.LatLng(
                            position.latitude,
                            position.longitude
                        ),
                        position.zoom
                    ),
                    2000
                )
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
