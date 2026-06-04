package com.jalan3d.map

import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap

/**
 * Controls 3D camera interactions for the map.
 *
 * Handles fly-to animations that position the camera at a 3D angle
 * (pitch ≈ 60°) focused on a specific report location.
 */
object Map3DController {

    /** Default zoom level for report close-up view. */
    private const val REPORT_ZOOM = 17.0

    /** Default tilt (pitch) for 3D view. */
    private const val REPORT_TILT = 60.0

    /**
     * Fly the camera to [lat]/[lng] with a 3D viewing angle.
     * Smoothly animates over [durationMs] milliseconds.
     */
    fun flyToLocation(
        map: MapLibreMap,
        lat: Double,
        lng: Double,
        durationMs: Int = 2000
    ) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                org.maplibre.android.camera.CameraPosition.Builder()
                    .target(org.maplibre.android.geometry.LatLng(lat, lng))
                    .zoom(REPORT_ZOOM)
                    .bearing(0.0)
                    .tilt(REPORT_TILT)
                    .build()
            ),
            durationMs
        )
    }
}
