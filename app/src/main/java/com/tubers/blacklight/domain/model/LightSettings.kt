package com.tubers.blacklight.domain.model

/**
 * Everything that defines the current look of the light. Persisted, and also what a
 * [Preset] captures. Speed is stored in tenths (5..20 = 0.5×..2.0×) to keep equality exact.
 */
data class LightSettings(
    val isOn: Boolean = true,
    val mode: LightMode = LightMode.UV_GLOW,
    val colorArgb: Int = Swatches.VIOLET.argb,
    val colorName: String = Swatches.VIOLET.displayName,
    val groundArgb: Int = DEFAULT_GROUND,
    val themeId: String? = BuiltInThemes.MIDNIGHT_ID,
    val brightness: Int = 75,
    val beamWidth: Int = 55,
    val glowRadius: Int = 60,
    val speedTenths: Int = 10,
) {
    val speed: Float get() = speedTenths / 10f

    companion object {
        const val DEFAULT_GROUND: Int = 0xFF07050C.toInt()
        const val BRIGHTNESS_MIN = 10
        const val BRIGHTNESS_MAX = 100
        const val PERCENT_MIN = 0
        const val PERCENT_MAX = 100
        const val SPEED_MIN_TENTHS = 5
        const val SPEED_MAX_TENTHS = 20
    }
}

/** App-level preferences that are not part of a look. */
data class AppPreferences(
    val reducedMotion: Boolean = false,
    val ledEnabled: Boolean = false,
    /** Demo-only unlock. Never treated as a real purchase. */
    val premiumPreview: Boolean = false,
    val introSeen: Boolean = false,
    /** The user has read and accepted the flashing-lights / photosensitive epilepsy warning. */
    val flashWarningAccepted: Boolean = false,
)

/** The five built-in light colors. */
enum class Swatches(val displayName: String, val argb: Int) {
    VIOLET("Violet", 0xFFA855F7.toInt()),
    BLUE("Blue", 0xFF4C7DFF.toInt()),
    CYAN("Cyan", 0xFF22D3EE.toInt()),
    GREEN("Green", 0xFF4ADE80.toInt()),
    PINK("Pink", 0xFFF472D0.toInt());
}
