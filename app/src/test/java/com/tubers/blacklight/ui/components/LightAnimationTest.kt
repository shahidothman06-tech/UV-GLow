package com.tubers.blacklight.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the light's motion effects may run. */
class LightAnimationTest {

    @Test
    fun animates_whenOnVisibleAndNothingAsksForLessMotion() {
        assertTrue(animationAllowed(isOn = true, reducedMotion = false, animatorScale = 1f, visible = true))
        assertTrue("slowed system animations still animate", animationAllowed(true, false, animatorScale = 0.5f, visible = true))
    }

    @Test
    fun stops_forEachReason() {
        assertFalse("light off", animationAllowed(isOn = false, reducedMotion = false, animatorScale = 1f, visible = true))
        assertFalse("in-app reduced motion", animationAllowed(isOn = true, reducedMotion = true, animatorScale = 1f, visible = true))
        assertFalse("system Remove animations", animationAllowed(isOn = true, reducedMotion = false, animatorScale = 0f, visible = true))
        assertFalse("not on screen", animationAllowed(isOn = true, reducedMotion = false, animatorScale = 1f, visible = false))
    }
}
