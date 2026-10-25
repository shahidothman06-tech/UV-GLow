package com.tubers.blacklight.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutesTest {

    @Test fun subScreensGetABackHeaderTitle() {
        assertEquals("Auto-off timer", Routes.subScreenTitle(Routes.TIMER))
        assertEquals("Premium", Routes.subScreenTitle(Routes.PREMIUM))
        assertEquals("Privacy policy", Routes.subScreenTitle(Routes.PRIVACY))
    }

    @Test fun tabsAndIntroAreNotSubScreens() {
        listOf(Routes.HOME, Routes.EFFECTS, Routes.THEMES, Routes.PRESETS, Routes.SETTINGS, Routes.INTRO, null)
            .forEach { assertNull(it, Routes.subScreenTitle(it)) }
    }
}
