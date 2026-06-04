package com.jalan3d.map

import android.graphics.Color
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.light.Light
import org.maplibre.android.style.light.Position
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Manages the FillExtrusionLayer for 3D road damage visualization.
 *
 * Each report becomes a square polygon extruded to a height proportional
 * to its severity. Color is also driven by severity.
 *
 * Call [initialize] once after the style loads, then [updateExtrusions]
 * whenever the extrusion GeoJSON changes.
 */
object ExtrusionLayer {

    private const val SOURCE_ID = "extrusion-source"
    private const val LAYER_ID = "extrusion-layer"

    /**
     * Add the GeoJsonSource + FillExtrusionLayer to the map style.
     * Safe to call multiple times — skips if already added.
     */
    fun initialize(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return

        val source = GeoJsonSource(SOURCE_ID)
        style.addSource(source)

        // Add light for 3D extrusion rendering
        configureLight(style)

        val layer = FillExtrusionLayer(LAYER_ID, SOURCE_ID).apply {
            setProperties(
                // Height from per-feature property
                PropertyFactory.fillExtrusionHeight(
                    Expression.get("extrusionHeight")
                ),
                // Color from per-feature property (hex string from GeoJSON)
                PropertyFactory.fillExtrusionColor(
                    Expression.get("extrusionColor")
                ),
                // Semi-transparent so underlying markers are still visible
                PropertyFactory.fillExtrusionOpacity(0.7f),
                // Extrude from ground level
                PropertyFactory.fillExtrusionBase(0f)
            )
        }

        // Add below the circle markers so extrusions form the base layer
        style.addLayerBelow(layer, MapMarkers.REPORTS_LAYER_ID)
    }

    /**
     * Update the extrusion source with a new GeoJSON FeatureCollection.
     * Pass an empty GeoJSON to clear all extrusions.
     */
    fun updateExtrusions(style: Style, geoJson: String) {
        val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
        source.setGeoJson(geoJson)
    }

    /**
     * Configure ambient + directional light so fill-extrusion is properly
     * shaded and visible. Only sets if the style doesn't already have light.
     */
    private fun configureLight(style: Style) {
        try {
            val light = style.light
            light?.let {
                it.setColor(Color.parseColor("#FFFFFF"))
                it.setIntensity(0.6f)
                // Position: anchor=viewport, azimuthal=210°, polar=60°
                it.setPosition(Position(1.15f, 210f, 60f))
            }
        } catch (_: Exception) {
            // Light API may not be available in all SDK versions — skip
        }
    }
}
