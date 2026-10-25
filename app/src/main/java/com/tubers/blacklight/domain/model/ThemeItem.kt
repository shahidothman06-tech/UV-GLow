package com.tubers.blacklight.domain.model

/** A named accent + background pairing. Built-in themes live in code; custom ones in Room. */
data class ThemeItem(
    val id: String,
    val name: String,
    val accentArgb: Int,
    val groundArgb: Int,
    val isCustom: Boolean,
)

object BuiltInThemes {
    const val MIDNIGHT_ID = "midnight"
    const val CUSTOM_PREFIX = "custom-"

    val all: List<ThemeItem> = listOf(
        ThemeItem(MIDNIGHT_ID, "Midnight Violet", 0xFFA855F7.toInt(), 0xFF07050C.toInt(), false),
        ThemeItem("ocean", "Deep Ocean", 0xFF4C7DFF.toInt(), 0xFF04060E.toInt(), false),
        ThemeItem("aurora", "Aurora", 0xFF22D3EE.toInt(), 0xFF03090B.toInt(), false),
        ThemeItem("glowstick", "Glowstick", 0xFF4ADE80.toInt(), 0xFF040A06.toInt(), false),
        ThemeItem("bloom", "Neon Bloom", 0xFFF472D0.toInt(), 0xFF0B0509.toInt(), false),
    )

    fun customId(dbId: Long): String = CUSTOM_PREFIX + dbId

    fun customDbId(themeId: String): Long? =
        if (themeId.startsWith(CUSTOM_PREFIX)) themeId.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null
}
