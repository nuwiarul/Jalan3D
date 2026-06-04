package com.jalan3d.data

/**
 * Severity level for road damage reports.
 */
enum class Severity(val key: String, val label: String, val colorHex: String) {
    RINGAN("ringan", "Ringan", "#4CAF50"),
    SEDANG("sedang", "Sedang", "#FF9800"),
    BERAT("berat", "Berat", "#F44336"),
    KRITIS("kritis", "Kritis", "#9C27B0");

    companion object {
        fun fromKey(key: String): Severity {
            return entries.firstOrNull { it.key == key } ?: SEDANG
        }
    }
}
