package com.tubers.blacklight.domain

import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.logic.PremiumRules
import com.tubers.blacklight.domain.logic.StudioLogic
import com.tubers.blacklight.domain.model.BuiltInThemes
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Swatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** State management: every user action on the light goes through these functions. */
class StudioLogicTest {

    private val base = LightSettings()

    @Test
    fun togglePower_flipsState() {
        assertFalse(StudioLogic.togglePower(base).isOn)
        assertTrue(StudioLogic.togglePower(StudioLogic.togglePower(base)).isOn)
    }

    @Test
    fun sliders_areClampedToTheirRanges() {
        assertEquals(10, StudioLogic.setBrightness(base, 0).brightness)
        assertEquals(100, StudioLogic.setBrightness(base, 250).brightness)
        assertEquals(0, StudioLogic.setBeamWidth(base, -5).beamWidth)
        assertEquals(100, StudioLogic.setGlowRadius(base, 101).glowRadius)
        assertEquals(5, StudioLogic.setSpeed(base, 1).speedTenths)
        assertEquals(20, StudioLogic.setSpeed(base, 99).speedTenths)
    }

    @Test
    fun premiumModes_areLockedOnFreePlan() {
        assertNull(StudioLogic.selectMode(base, LightMode.STROBE, premium = false))
        assertNull(StudioLogic.selectMode(base, LightMode.BLACKLIGHT, premium = false))
        assertEquals(LightMode.PULSE, StudioLogic.selectMode(base, LightMode.PULSE, premium = false)?.mode)
        assertEquals(LightMode.STROBE, StudioLogic.selectMode(base, LightMode.STROBE, premium = true)?.mode)
    }

    @Test
    fun freePlanLimits() {
        assertTrue(PremiumRules.canSavePreset(4, premium = false))
        assertFalse(PremiumRules.canSavePreset(5, premium = false))
        assertTrue(PremiumRules.canSavePreset(500, premium = true))
        assertTrue(PremiumRules.canCreateCustomTheme(0, premium = false))
        assertFalse(PremiumRules.canCreateCustomTheme(1, premium = false))
        assertTrue(PremiumRules.canCreateCustomTheme(3, premium = true))
    }

    @Test
    fun swatchAndCustomColor_setNames() {
        val pink = StudioLogic.selectSwatch(base, Swatches.PINK)
        assertEquals(Swatches.PINK.argb, pink.colorArgb)
        assertEquals("Pink", pink.colorName)

        val custom = StudioLogic.selectCustomColor(base, 0x123456)
        assertEquals(0xFF123456.toInt(), custom.colorArgb) // alpha forced opaque
        assertEquals("Custom", custom.colorName)

        // Picking a value that equals a swatch keeps the friendly name.
        assertEquals("Cyan", StudioLogic.selectCustomColor(base, Swatches.CYAN.argb).colorName)
    }

    @Test
    fun applyTheme_copiesAccentGroundAndId() {
        val ocean = BuiltInThemes.all.first { it.id == "ocean" }
        val s = StudioLogic.applyTheme(base, ocean)
        assertEquals(ocean.accentArgb, s.colorArgb)
        assertEquals(ocean.groundArgb, s.groundArgb)
        assertEquals("ocean", s.themeId)
        assertEquals("Deep Ocean", s.colorName)
    }

    @Test
    fun onThemeDeleted_onlyClearsMatchingTheme() {
        val s = base.copy(themeId = "custom-3")
        assertNull(StudioLogic.onThemeDeleted(s, "custom-3").themeId)
        assertEquals("custom-3", StudioLogic.onThemeDeleted(s, "custom-9").themeId)
    }

    @Test
    fun reset_restoresDefaults() {
        val changed = base.copy(isOn = false, mode = LightMode.SOS, brightness = 12, speedTenths = 20)
        assertEquals(LightSettings(), StudioLogic.reset())
        assertTrue(StudioLogic.reset() != changed)
    }

    @Test
    fun cleanName_trimsCollapsesAndLimits() {
        assertEquals("My preset", StudioLogic.cleanName("  My    preset ", "x"))
        assertEquals("Fallback", StudioLogic.cleanName("   ", "Fallback"))
        assertEquals(StudioLogic.MAX_NAME_LENGTH, StudioLogic.cleanName("a".repeat(80), "x").length)
        assertEquals("Preset 4", StudioLogic.defaultPresetName(3))
    }

    @Test
    fun newCustomTheme_usesCurrentColorAndTintedGround() {
        val s = StudioLogic.selectSwatch(base, Swatches.GREEN)
        val t = StudioLogic.newCustomTheme(s, "Lime")
        assertTrue(t.isCustom)
        assertEquals(Swatches.GREEN.argb, t.accentArgb)
        assertEquals(ColorMath.groundFor(Swatches.GREEN.argb), t.groundArgb)
    }

    @Test
    fun customThemeIds_roundTrip() {
        assertEquals("custom-42", BuiltInThemes.customId(42))
        assertEquals(42L, BuiltInThemes.customDbId("custom-42"))
        assertNull(BuiltInThemes.customDbId("ocean"))
        assertNotNull(LightMode.fromId("rays"))
        assertEquals(LightMode.UV_GLOW, LightMode.fromId("unknown"))
    }
}
