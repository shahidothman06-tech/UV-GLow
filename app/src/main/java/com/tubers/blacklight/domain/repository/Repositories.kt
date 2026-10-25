package com.tubers.blacklight.domain.repository

import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.ThemeItem
import kotlinx.coroutines.flow.Flow

/** Current look + app preferences (DataStore in production, in-memory fakes in tests). */
interface SettingsRepository {
    val light: Flow<LightSettings>
    val preferences: Flow<AppPreferences>
    suspend fun updateLight(transform: (LightSettings) -> LightSettings)
    suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences)
}

/** Saved presets, newest first. */
interface PresetRepository {
    val presets: Flow<List<Preset>>
    suspend fun insert(preset: Preset): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
}

/** User-created themes. Built-in themes are not stored. */
interface ThemeRepository {
    val customThemes: Flow<List<ThemeItem>>
    /** Returns the new theme's id (e.g. "custom-12"). */
    suspend fun insert(theme: ThemeItem): String
    suspend fun rename(themeId: String, name: String)
    suspend fun updateColors(themeId: String, accentArgb: Int, groundArgb: Int)
    suspend fun delete(themeId: String)
}
