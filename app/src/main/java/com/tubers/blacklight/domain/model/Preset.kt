package com.tubers.blacklight.domain.model

/** A saved look: color, mode, brightness, beam width, glow, speed and theme. */
data class Preset(
    val id: Long = 0,
    val name: String,
    val mode: LightMode,
    val colorArgb: Int,
    val colorName: String,
    val groundArgb: Int,
    val themeId: String?,
    val brightness: Int,
    val beamWidth: Int,
    val glowRadius: Int,
    val speedTenths: Int,
    val createdAt: Long = 0,
)
