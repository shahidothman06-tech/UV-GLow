package com.tubers.blacklight.domain.model

/**
 * The visual light modes. [baseSeconds] is the length of one animation cycle at 1.0× speed.
 * [premium] modes are locked on the free plan.
 */
enum class LightMode(
    val id: String,
    val displayName: String,
    val description: String,
    val baseSeconds: Float,
    val premium: Boolean,
) {
    UV_GLOW("uv", "UV Glow", "Steady violet glow", 6f, false),
    PULSE("pulse", "Pulse", "Slow breathing light", 2.4f, false),
    LIGHT_RAYS("rays", "Light Rays", "Sweeping beam rays", 5f, false),
    SOS("sos", "SOS", "Morse-timed signal", 7f, false),
    STROBE("strobe", "Strobe", "Gentle, max 3 flashes/s", 0.5f, true),
    BLACKLIGHT("black", "Blacklight", "Deep glow, drifting specks", 5f, true);

    companion object {
        fun fromId(id: String?): LightMode = entries.firstOrNull { it.id == id } ?: UV_GLOW
    }
}
