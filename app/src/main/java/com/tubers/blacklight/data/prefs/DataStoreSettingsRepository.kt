package com.tubers.blacklight.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.uvGlowDataStore: DataStore<Preferences> by preferencesDataStore(name = "uv_glow_settings")

/** Persists the current look and app preferences with Jetpack DataStore. */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val store = context.applicationContext.uvGlowDataStore

    private object Keys {
        val ON = booleanPreferencesKey("light_on")
        val MODE = stringPreferencesKey("mode")
        val COLOR = intPreferencesKey("color_argb")
        val COLOR_NAME = stringPreferencesKey("color_name")
        val GROUND = intPreferencesKey("ground_argb")
        val THEME_ID = stringPreferencesKey("theme_id")
        val BRIGHTNESS = intPreferencesKey("brightness")
        val BEAM_WIDTH = intPreferencesKey("beam_width")
        val GLOW = intPreferencesKey("glow_radius")
        val SPEED = intPreferencesKey("speed_tenths")

        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LED = booleanPreferencesKey("led_enabled")
        val PREMIUM_PREVIEW = booleanPreferencesKey("premium_preview_demo")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
    }

    /** A disk read error must not crash the app: fall back to defaults. */
    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val light: Flow<LightSettings> = safeData.map { it.toLight() }.distinctUntilChanged()

    override val preferences: Flow<AppPreferences> = safeData.map { it.toAppPreferences() }.distinctUntilChanged()

    override suspend fun updateLight(transform: (LightSettings) -> LightSettings) {
        store.edit { p -> p.writeLight(transform(p.toLight())) }
    }

    override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        store.edit { p -> p.writeAppPreferences(transform(p.toAppPreferences())) }
    }

    private fun Preferences.toLight(): LightSettings {
        val d = LightSettings()
        // Missing key → default theme; empty string → "no theme applied".
        val themeId = when (val raw = this[Keys.THEME_ID]) {
            null -> d.themeId
            "" -> null
            else -> raw
        }
        return LightSettings(
            isOn = this[Keys.ON] ?: d.isOn,
            mode = LightMode.fromId(this[Keys.MODE]),
            colorArgb = this[Keys.COLOR] ?: d.colorArgb,
            colorName = this[Keys.COLOR_NAME] ?: d.colorName,
            groundArgb = this[Keys.GROUND] ?: d.groundArgb,
            themeId = themeId,
            brightness = (this[Keys.BRIGHTNESS] ?: d.brightness)
                .coerceIn(LightSettings.BRIGHTNESS_MIN, LightSettings.BRIGHTNESS_MAX),
            beamWidth = (this[Keys.BEAM_WIDTH] ?: d.beamWidth).coerceIn(0, 100),
            glowRadius = (this[Keys.GLOW] ?: d.glowRadius).coerceIn(0, 100),
            speedTenths = (this[Keys.SPEED] ?: d.speedTenths)
                .coerceIn(LightSettings.SPEED_MIN_TENTHS, LightSettings.SPEED_MAX_TENTHS),
        )
    }

    private fun MutablePreferences.writeLight(s: LightSettings) {
        this[Keys.ON] = s.isOn
        this[Keys.MODE] = s.mode.id
        this[Keys.COLOR] = s.colorArgb
        this[Keys.COLOR_NAME] = s.colorName
        this[Keys.GROUND] = s.groundArgb
        this[Keys.THEME_ID] = s.themeId ?: ""
        this[Keys.BRIGHTNESS] = s.brightness
        this[Keys.BEAM_WIDTH] = s.beamWidth
        this[Keys.GLOW] = s.glowRadius
        this[Keys.SPEED] = s.speedTenths
    }

    private fun Preferences.toAppPreferences() = AppPreferences(
        reducedMotion = this[Keys.REDUCED_MOTION] ?: false,
        ledEnabled = this[Keys.LED] ?: false,
        premiumPreview = this[Keys.PREMIUM_PREVIEW] ?: false,
        introSeen = this[Keys.INTRO_SEEN] ?: false,
    )

    private fun MutablePreferences.writeAppPreferences(a: AppPreferences) {
        this[Keys.REDUCED_MOTION] = a.reducedMotion
        this[Keys.LED] = a.ledEnabled
        this[Keys.PREMIUM_PREVIEW] = a.premiumPreview
        this[Keys.INTRO_SEEN] = a.introSeen
    }
}
