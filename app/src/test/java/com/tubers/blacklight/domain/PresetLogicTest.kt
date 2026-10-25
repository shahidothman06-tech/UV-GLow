package com.tubers.blacklight.domain

import com.tubers.blacklight.domain.logic.StudioLogic
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Swatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetLogicTest {

    private val look = LightSettings(
        isOn = true,
        mode = LightMode.LIGHT_RAYS,
        colorArgb = Swatches.PINK.argb,
        colorName = "Pink",
        groundArgb = 0xFF0B0509.toInt(),
        themeId = "bloom",
        brightness = 90,
        beamWidth = 85,
        glowRadius = 80,
        speedTenths = 14,
    )

    @Test
    fun presetFrom_capturesEveryLookField() {
        val p = StudioLogic.presetFrom(look, "Party rays", createdAt = 7)
        assertEquals("Party rays", p.name)
        assertEquals(LightMode.LIGHT_RAYS, p.mode)
        assertEquals(Swatches.PINK.argb, p.colorArgb)
        assertEquals("Pink", p.colorName)
        assertEquals("bloom", p.themeId)
        assertEquals(90, p.brightness)
        assertEquals(85, p.beamWidth)
        assertEquals(80, p.glowRadius)
        assertEquals(14, p.speedTenths)
        assertEquals(7, p.createdAt)
    }

    @Test
    fun applyPreset_restoresLookAndTurnsLightOn() {
        val p = StudioLogic.presetFrom(look, "Party rays")
        val start = LightSettings(isOn = false)
        val applied = StudioLogic.applyPreset(start, p, premium = false)!!
        assertTrue(applied.isOn)
        assertEquals(look, applied)
    }

    @Test
    fun applyPreset_withPremiumMode_isBlockedOnFreePlan() {
        val p = StudioLogic.presetFrom(look.copy(mode = LightMode.BLACKLIGHT), "Deep")
        assertNull(StudioLogic.applyPreset(LightSettings(), p, premium = false))
        assertEquals(LightMode.BLACKLIGHT, StudioLogic.applyPreset(LightSettings(), p, premium = true)?.mode)
    }

    @Test
    fun isPresetActive_matchesOnlyTheExactLook() {
        val p = StudioLogic.presetFrom(look, "Party rays")
        assertTrue(StudioLogic.isPresetActive(look, p))
        assertFalse(StudioLogic.isPresetActive(look.copy(isOn = false), p))
        assertFalse(StudioLogic.isPresetActive(look.copy(brightness = 89), p))
        assertFalse(StudioLogic.isPresetActive(look.copy(speedTenths = 15), p))
    }

    @Test
    fun presetMeta_formatsSummary() {
        val p = StudioLogic.presetFrom(look.copy(colorName = "Violet", brightness = 45, speedTenths = 6), "Reading nook")
        assertEquals("Violet · 45% · 0.6×", StudioLogic.presetMeta(p))
        assertEquals("1.0×", StudioLogic.formatSpeed(10))
        assertEquals("2.0×", StudioLogic.formatSpeed(20))
    }
}
