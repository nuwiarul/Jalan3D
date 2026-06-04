package com.jalan3d.data

import com.jalan3d.data.Severity

/**
 * Configuration for extruding a single report into a 3D polygon.
 *
 * @property heightMeters Extrusion height in meters (z-axis)
 * @property bboxHalfSizeMeters Half-width of the bounding box in meters
 * @property colorHex CSS hex color for the extrusion fill
 */
data class ExtrusionConfig(
    val heightMeters: Float,
    val bboxHalfSizeMeters: Float,
    val colorHex: String
)

/**
 * Severity → extrusion config mapping.
 */
fun Severity.toExtrusionConfig(): ExtrusionConfig = when (this) {
    Severity.RINGAN -> ExtrusionConfig(0.5f, 25f, "#4CAF50")   // green, 50m square
    Severity.SEDANG -> ExtrusionConfig(1.5f, 40f, "#FF9800")   // orange, 80m square
    Severity.BERAT  -> ExtrusionConfig(3f,   60f, "#F44336")   // red, 120m square
    Severity.KRITIS -> ExtrusionConfig(5f,   80f, "#9C27B0")   // purple, 160m square
}

/**
 * Pipeline that converts [Report] data into MapLibre-compatible
 * GeoJSON FeatureCollection for FillExtrusionLayer rendering.
 *
 * Each report produces a Polygon feature (square bbox around lat/lng)
 * with properties that drive the extrusion style.
 */
object ExtrusionPipeline {

    /**
     * Generate a complete GeoJSON FeatureCollection string from reports.
     * Ready to pass directly to a GeoJsonSource.
     */
    fun reportsToGeoJson(reports: List<Report>): String {
        if (reports.isEmpty()) {
            return """{"type":"FeatureCollection","features":[]}"""
        }

        val features = reports.joinToString(",") { report ->
            reportToGeoJsonFeature(report)
        }

        return """{"type":"FeatureCollection","features":[$features]}"""
    }

    /**
     * Convert a single report into a GeoJSON Polygon feature.
     * Properties carry severity key + height in meters + hex color
     * so the FillExtrusionLayer can style each polygon individually.
     */
    private fun reportToGeoJsonFeature(report: Report): String {
        val config = report.severity.toExtrusionConfig()
        val polygon = generateBboxPolygon(
            report.latitude, report.longitude,
            config.bboxHalfSizeMeters
        )

        // Escape property strings
        val escapedId = report.id
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        return """{"type":"Feature","geometry":${polygon},"properties":{""" +
                """"severity":"${report.severity.key}",""" +
                """"id":"$escapedId",""" +
                """"extrusionHeight":${config.heightMeters},""" +
                """"extrusionColor":"${config.colorHex}"""" +
                "}}"
    }

    /**
     * Generate a square bounding-box Polygon around a lat/lng point.
     * Returns GeoJSON Polygon coordinate array: [[lng, lat], ...]
     */
    private fun generateBboxPolygon(
        lat: Double, lng: Double,
        halfSizeMeters: Float
    ): String {
        val dLat = halfSizeMeters.toDouble() / METERS_PER_DEG_LAT
        val dLng = halfSizeMeters.toDouble() / (METERS_PER_DEG_LAT * Math.cos(Math.toRadians(lat)))

        // Square: SW → NW → NE → SE → close(SW)
        val swLat = lat - dLat
        val swLng = lng - dLng
        val nwLat = lat + dLat
        val nwLng = lng - dLng
        val neLat = lat + dLat
        val neLng = lng + dLng
        val seLat = lat - dLat
        val seLng = lng + dLng

        return """{"type":"Polygon","coordinates":[[
            |[$swLng,$swLat],[$nwLng,$nwLat],
            |[$neLng,$neLat],[$seLng,$seLat],
            |[$swLng,$swLat]
        |]]}""".trimMargin()
    }

    private const val METERS_PER_DEG_LAT = 111_320.0
}
