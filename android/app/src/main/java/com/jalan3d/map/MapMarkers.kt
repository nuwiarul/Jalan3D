package com.jalan3d.map

import android.graphics.Color
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Helper to manage a single tapped-location marker on the map.
 * Uses a CircleLayer + GeoJsonSource for clean red dot marker.
 */
object MapMarkers {

    private const val SOURCE_ID = "tapped-location-source"
    private const val LAYER_ID = "tapped-location-layer"

    /**
     * Call once after style is loaded to add the marker source + layer.
     */
    fun initialize(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return

        val source = GeoJsonSource(SOURCE_ID)
        style.addSource(source)

        val layer = CircleLayer(LAYER_ID, SOURCE_ID).apply {
            setProperties(
                PropertyFactory.circleRadius(12f),
                PropertyFactory.circleColor(Color.RED),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleOpacity(0.9f)
            )
        }
        style.addLayer(layer)
    }

    /**
     * Update marker position using raw GeoJSON string.
     * Point coordinates are [lng, lat].
     */
    fun updatePosition(style: Style, lat: Double, lng: Double) {
        val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        source?.setGeoJson("{\"type\":\"Point\",\"coordinates\":[$lng,$lat]}")
    }

    /**
     * Remove marker from the map.
     */
    fun remove(style: Style) {
        val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        source?.setGeoJson("{\"type\":\"GeometryCollection\",\"geometries\":[]}")
    }
}
