package com.tubers.blacklight.domain

import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.logic.ModeAnimation
import com.tubers.blacklight.domain.model.LightMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectsAndColorTest {

    @Test
    fun strobe_neverExceedsThreeFlashesPerSecond() {
        for (tenths in 5..20) {
            assertTrue(ModeAnimation.flashesPerSecond(LightMode.STROBE, tenths) <= 3f)
        }
    }

    @Test
    fun cycleSeconds_scaleWithSpeed() {
        assertEquals(6f, ModeAnimation.cycleSeconds(LightMode.UV_GLOW, 10), 0.001f)
        assertEquals(3f, ModeAnimation.cycleSeconds(LightMode.UV_GLOW, 20), 0.001f)
        assertEquals(12f, ModeAnimation.cycleSeconds(LightMode.UV_GLOW, 5), 0.001f)
    }

    @Test
    fun alpha_staysInRange_forEveryMode() {
        LightMode.entries.forEach { mode ->
            var p = 0f
            while (p <= 2f) {
                val a = ModeAnimation.alpha(mode, p)
                assertTrue("$mode at $p = $a", a in 0f..1f)
                p += 0.01f
            }
        }
    }

    @Test
    fun alpha_shapes() {
        assertEquals(1f, ModeAnimation.alpha(LightMode.PULSE, 0f), 0.001f)
        assertEquals(0.28f, ModeAnimation.alpha(LightMode.PULSE, 0.5f), 0.001f)
        assertEquals(1f, ModeAnimation.alpha(LightMode.STROBE, 0.2f), 0.001f)
        assertEquals(0.1f, ModeAnimation.alpha(LightMode.STROBE, 0.7f), 0.001f)
        assertEquals(1f, ModeAnimation.alpha(LightMode.SOS, 0.01f), 0.001f)   // first dot
        assertEquals(0.12f, ModeAnimation.alpha(LightMode.SOS, 0.9f), 0.001f) // pause
    }

    @Test
    fun colorMath_mixAndHex() {
        assertEquals(0xFF000000.toInt(), ColorMath.mix(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0f))
        assertEquals(0xFFFFFFFF.toInt(), ColorMath.mix(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 1f))
        assertEquals("#A855F7", ColorMath.toHex(0xFFA855F7.toInt()))
    }

    @Test
    fun colorMath_hsvRoundTrip() {
        listOf(0xFFA855F7, 0xFF4C7DFF, 0xFF22D3EE, 0xFF4ADE80, 0xFFF472D0).forEach { c ->
            val argb = c.toInt()
            val hsv = ColorMath.argbToHsv(argb)
            val back = ColorMath.hsvToArgb(hsv[0], hsv[1], hsv[2])
            assertTrue("round trip ${ColorMath.toHex(argb)} -> ${ColorMath.toHex(back)}",
                kotlin.math.abs(ColorMath.red(back) - ColorMath.red(argb)) <= 1 &&
                    kotlin.math.abs(ColorMath.green(back) - ColorMath.green(argb)) <= 1 &&
                    kotlin.math.abs(ColorMath.blue(back) - ColorMath.blue(argb)) <= 1)
        }
    }

    @Test
    fun onAccent_keepsButtonTextReadable() {
        // Text on every built-in accent meets WCAG AA for large/bold UI text (3:1).
        listOf(0xFFA855F7, 0xFF4C7DFF, 0xFF22D3EE, 0xFF4ADE80, 0xFFF472D0).forEach { c ->
            val accent = c.toInt()
            assertTrue(ColorMath.contrastRatio(accent, ColorMath.onAccent(accent)) >= 3.0)
        }
        // Very dark custom accents are lifted for text on the dark background.
        val dark = 0xFF1A0A30.toInt()
        assertTrue(ColorMath.contrastRatio(ColorMath.accentText(dark), 0xFF07050C.toInt()) > ColorMath.contrastRatio(dark, 0xFF07050C.toInt()))
    }
}
