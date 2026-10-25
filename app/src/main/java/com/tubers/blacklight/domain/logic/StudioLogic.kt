package com.tubers.blacklight.domain.logic

import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.Swatches
import com.tubers.blacklight.domain.model.ThemeItem

/**
 * Free-plan limits. Premium (real subscription or the clearly labelled demo preview)
 * removes them.
 */
object PremiumRules {
    const val FREE_PRESET_LIMIT = 5
    const val FREE_CUSTOM_THEME_LIMIT = 1

    fun isModeLocked(mode: LightMode, premium: Boolean) = mode.premium && !premium
    fun canSavePreset(existing: Int, premium: Boolean) = premium || existing < FREE_PRESET_LIMIT
    fun canCreateCustomTheme(existingCustom: Int, premium: Boolean) =
        premium || existingCustom < FREE_CUSTOM_THEME_LIMIT
}

/**
 * Pure transformations of [LightSettings]. The ViewModel routes every user action through
 * these, so the rules are in one place and covered by unit tests.
 */
object StudioLogic {

    const val MAX_NAME_LENGTH = 32

    fun togglePower(s: LightSettings) = s.copy(isOn = !s.isOn)

    fun setBrightness(s: LightSettings, v: Int) =
        s.copy(brightness = v.coerceIn(LightSettings.BRIGHTNESS_MIN, LightSettings.BRIGHTNESS_MAX))

    fun setBeamWidth(s: LightSettings, v: Int) =
        s.copy(beamWidth = v.coerceIn(LightSettings.PERCENT_MIN, LightSettings.PERCENT_MAX))

    fun setGlowRadius(s: LightSettings, v: Int) =
        s.copy(glowRadius = v.coerceIn(LightSettings.PERCENT_MIN, LightSettings.PERCENT_MAX))

    fun setSpeed(s: LightSettings, tenths: Int) =
        s.copy(speedTenths = tenths.coerceIn(LightSettings.SPEED_MIN_TENTHS, LightSettings.SPEED_MAX_TENTHS))

    /** Returns null when the mode is locked for this user. */
    fun selectMode(s: LightSettings, mode: LightMode, premium: Boolean): LightSettings? =
        if (PremiumRules.isModeLocked(mode, premium)) null else s.copy(mode = mode)

    fun selectSwatch(s: LightSettings, swatch: Swatches) =
        s.copy(colorArgb = swatch.argb, colorName = swatch.displayName)

    fun selectCustomColor(s: LightSettings, argb: Int): LightSettings {
        val opaque = argb or (0xFF shl 24)
        val name = Swatches.entries.firstOrNull { it.argb == opaque }?.displayName ?: "Custom"
        return s.copy(colorArgb = opaque, colorName = name)
    }

    fun applyTheme(s: LightSettings, theme: ThemeItem) = s.copy(
        colorArgb = theme.accentArgb,
        colorName = theme.name,
        groundArgb = theme.groundArgb,
        themeId = theme.id,
    )

    /** Capture the current look as a preset. */
    fun presetFrom(s: LightSettings, name: String, createdAt: Long = 0): Preset = Preset(
        name = name,
        mode = s.mode,
        colorArgb = s.colorArgb,
        colorName = s.colorName,
        groundArgb = s.groundArgb,
        themeId = s.themeId,
        brightness = s.brightness,
        beamWidth = s.beamWidth,
        glowRadius = s.glowRadius,
        speedTenths = s.speedTenths,
        createdAt = createdAt,
    )

    /** Applying a preset always switches the light on. Returns null if its mode is locked. */
    fun applyPreset(s: LightSettings, p: Preset, premium: Boolean): LightSettings? {
        if (PremiumRules.isModeLocked(p.mode, premium)) return null
        return s.copy(
            isOn = true,
            mode = p.mode,
            colorArgb = p.colorArgb,
            colorName = p.colorName,
            groundArgb = p.groundArgb,
            themeId = p.themeId,
            brightness = p.brightness,
            beamWidth = p.beamWidth,
            glowRadius = p.glowRadius,
            speedTenths = p.speedTenths,
        )
    }

    /** True when the light is on and currently shows exactly this preset's look. */
    fun isPresetActive(s: LightSettings, p: Preset): Boolean =
        s.isOn && s.mode == p.mode && s.colorArgb == p.colorArgb && s.brightness == p.brightness &&
            s.beamWidth == p.beamWidth && s.glowRadius == p.glowRadius && s.speedTenths == p.speedTenths

    /** Default look. Presets and themes are kept by the caller. */
    fun reset(): LightSettings = LightSettings()

    /**
     * True when moving to [next] would start a flashing mode (Strobe, SOS) the user hasn't yet
     * accepted the photosensitivity warning for.
     */
    fun needsFlashWarning(next: LightSettings, warningAccepted: Boolean): Boolean =
        next.mode.flashes && !warningAccepted

    /** After the warning is declined: keep everything but swap a flashing mode for the steady glow. */
    fun withoutFlashing(s: LightSettings): LightSettings =
        if (s.mode.flashes) s.copy(mode = LightMode.UV_GLOW) else s

    /** If the applied theme was deleted, keep the colors but drop the reference. */
    fun onThemeDeleted(s: LightSettings, deletedId: String): LightSettings =
        if (s.themeId == deletedId) s.copy(themeId = null) else s

    /** Trimmed, length-limited name; [fallback] when blank. */
    fun cleanName(raw: String, fallback: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).ifBlank { fallback }

    fun defaultPresetName(existingCount: Int) = "Preset ${existingCount + 1}"

    fun newCustomTheme(s: LightSettings, name: String): ThemeItem = ThemeItem(
        id = "",
        name = name,
        accentArgb = s.colorArgb,
        groundArgb = ColorMath.groundFor(s.colorArgb),
        isCustom = true,
    )

    /** Short summary shown on preset cards: "Violet · 45% · 0.6×". */
    fun presetMeta(p: Preset): String =
        "${p.colorName} · ${p.brightness}% · ${formatSpeed(p.speedTenths)}"

    fun formatSpeed(tenths: Int): String = "${tenths / 10}.${tenths % 10}×"
}
