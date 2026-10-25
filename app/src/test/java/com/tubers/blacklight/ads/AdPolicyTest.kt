package com.tubers.blacklight.ads

import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    private val ready = AdsStatus(configured = true, canRequestAds = true, initialized = true)
    private val lightOff = LightSettings(isOn = false)

    @Test fun bannerShowsOnAnAdScreen_whenLightOffAndAdsReady() {
        assertTrue(AdPolicy.bannerAllowed(screenAllowsBanner = true, light = lightOff, ads = ready))
    }

    @Test fun noAdsWhileLightActive_inAnyMode() {
        LightMode.entries.forEach { mode ->
            val on = LightSettings(isOn = true, mode = mode)
            assertFalse("light on in $mode", AdPolicy.bannerAllowed(true, on, ready))
        }
        // Strobe and SOS specifically, the flashing modes.
        assertFalse(AdPolicy.bannerAllowed(true, LightSettings(isOn = true, mode = LightMode.STROBE), ready))
        assertFalse(AdPolicy.bannerAllowed(true, LightSettings(isOn = true, mode = LightMode.SOS), ready))
    }

    @Test fun noAdsWithoutConsent() {
        val noConsent = ready.copy(canRequestAds = false)
        assertFalse(AdPolicy.bannerAllowed(true, lightOff, noConsent))
        // Consent alone isn't enough until the SDK (initialised only after consent) is ready.
        assertFalse(AdPolicy.bannerAllowed(true, lightOff, ready.copy(initialized = false)))
    }

    @Test fun noAdsWhenNotConfigured_orOnScreensWithoutABannerSlot() {
        assertFalse(AdPolicy.bannerAllowed(true, lightOff, ready.copy(configured = false)))
        assertFalse(AdPolicy.bannerAllowed(screenAllowsBanner = false, light = lightOff, ads = ready))
        assertFalse(AdsStatus().ready)
    }
}
