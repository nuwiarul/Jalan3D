package com.jalan3d.map

import android.graphics.Color
import com.jalan3d.data.Report
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Helper to manage all map markers:
 * 1. Tapped-location marker (red dot) — user taps a spot on the map
 * 2. Reports markers (severity-colored circles) — existing reports from API
 */
object MapMarkers {

    // ─── Tapped location marker ───
    private const val TAP_SOURCE_ID = "tapped-location-source"
    private const val TAP_LAYER_ID = "tapped-location-layer"

    // ─── Reports markers ───
    private const val REPORTS_SOURCE_ID = "reports-source"
    internal const val REPORTS_LAYER_ID = "reports-layer"

    /**
     * Call once after style is loaded to add all sources + layers.
     */
    fun initialize(style: Style) {
        initTapMarker(style)
        initReportsLayer(style)
    }

    // ─── Tapped location (red dot) ───

    private fun initTapMarker(style: Style) {
        if (style.getSource(TAP_SOURCE_ID) != null) return

        val source = GeoJsonSource(TAP_SOURCE_ID)
        style.addSource(source)

        val layer = CircleLayer(TAP_LAYER_ID, TAP_SOURCE_ID).apply {
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
     * Update tapped-location marker position.
     * Point coordinates are [lng, lat].
     */
    fun updatePosition(style: Style, lat: Double, lng: Double) {
        val source = style.getSourceAs<GeoJsonSource>(TAP_SOURCE_ID)
        source?.setGeoJson("{\"type\":\"Point\",\"coordinates\":[$lng,$lat]}")
    }

    /**
     * Remove tapped-location marker.
     */
    fun removeTapMarker(style: Style) {
        val source = style.getSourceAs<GeoJsonSource>(TAP_SOURCE_ID)
        source?.setGeoJson("{\"type\":\"GeometryCollection\",\"geometries\":[]}")
    }

    // ─── Reports markers (severity-colored circles) ───

    private fun initReportsLayer(style: Style) {
        if (style.getSource(REPORTS_SOURCE_ID) != null) return

        val source = GeoJsonSource(REPORTS_SOURCE_ID)
        style.addSource(source)

        // Severity → color mapping
        val severityColor = Expression.match(
            Expression.get("severity"),
            Expression.literal("#888888"),           // default (gray)
            Expression.stop("ringan", Expression.literal("#4CAF50")),  // green
            Expression.stop("sedang", Expression.literal("#FF9800")),  // orange
            Expression.stop("berat", Expression.literal("#F44336")),   // red
            Expression.stop("kritis", Expression.literal("#9C27B0"))   // purple
        )

        // Severity → radius mapping
        val severityRadius = Expression.match(
            Expression.get("severity"),
            Expression.literal(8f),                           // default
            Expression.stop("ringan", Expression.literal(8f)),
            Expression.stop("sedang", Expression.literal(10f)),
            Expression.stop("berat", Expression.literal(12f)),
            Expression.stop("kritis", Expression.literal(14f))
        )

        val layer = CircleLayer(REPORTS_LAYER_ID, REPORTS_SOURCE_ID).apply {
            setProperties(
                PropertyFactory.circleRadius(severityRadius),
                PropertyFactory.circleColor(severityColor),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleOpacity(0.85f)
            )
        }
        style.addLayer(layer)
    }

    /**
     * Update all report markers from a list of [Report].
     * Replaces the entire GeoJSON FeatureCollection.
     */
    fun updateReports(style: Style, reports: List<Report>) {
        val source = style.getSourceAs<GeoJsonSource>(REPORTS_SOURCE_ID) ?: return

        if (reports.isEmpty()) {
            source.setGeoJson("{\"type\":\"FeatureCollection\",\"features\":[]}")
            return
        }

        val features = reports.joinToString(",") { report ->
            // Escape any special chars in properties
            val escapedSeverity = report.severity.key
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            val escapedId = report.id
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${report.longitude},${report.latitude}]},"properties":{"severity":"$escapedSeverity","id":"$escapedId"}}"""
        }

        source.setGeoJson("""{"type":"FeatureCollection","features":[$features]}""")
    }
}
