package com.tubers.blacklight.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tubers.blacklight.domain.model.BuiltInThemes
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.ThemeItem

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val modeId: String,
    val colorArgb: Int,
    val colorName: String,
    val groundArgb: Int,
    val themeId: String?,
    val brightness: Int,
    val beamWidth: Int,
    val glowRadius: Int,
    val speedTenths: Int,
    val createdAt: Long,
)

@Entity(tableName = "custom_themes")
data class ThemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accentArgb: Int,
    val groundArgb: Int,
    val createdAt: Long,
)

fun PresetEntity.toDomain() = Preset(
    id = id,
    name = name,
    mode = LightMode.fromId(modeId),
    colorArgb = colorArgb,
    colorName = colorName,
    groundArgb = groundArgb,
    themeId = themeId,
    brightness = brightness,
    beamWidth = beamWidth,
    glowRadius = glowRadius,
    speedTenths = speedTenths,
    createdAt = createdAt,
)

fun Preset.toEntity() = PresetEntity(
    id = id,
    name = name,
    modeId = mode.id,
    colorArgb = colorArgb,
    colorName = colorName,
    groundArgb = groundArgb,
    themeId = themeId,
    brightness = brightness,
    beamWidth = beamWidth,
    glowRadius = glowRadius,
    speedTenths = speedTenths,
    createdAt = createdAt,
)

fun ThemeEntity.toDomain() = ThemeItem(
    id = BuiltInThemes.customId(id),
    name = name,
    accentArgb = accentArgb,
    groundArgb = groundArgb,
    isCustom = true,
)
