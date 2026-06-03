package com.jalan3d.data.api

/**
 * API configuration.
 * Change BASE_URL to match your backend address:
 * - Android Emulator: http://10.0.2.2:3000
 * - Physical device on same Tailscale: http://100.72.147.67:3000
 */
object ApiConfig {
    // Default: emulator → host machine
    const val BASE_URL = "http://10.0.2.2:3000"

    // Tailscale IP for physical device testing
    // const val BASE_URL = "http://100.72.147.67:3000"
}
