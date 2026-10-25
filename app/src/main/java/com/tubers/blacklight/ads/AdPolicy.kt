package com.tubers.blacklight.ads

import com.tubers.blacklight.domain.model.LightSettings

/**
 * Where and when a banner may appear. Pure Kotlin so the rules are unit-tested.
 *
 * - Only on screens that opt in (Effects, Themes, Presets, Settings), never on Home.
 * - Never while the light is on, in any mode: no ad next to a running light, Strobe or SOS.
 * - Never before Google's consent check allows ads and the SDK is initialised.
 *
 * Ads never affect the light itself: the strobe cap and reduced motion live in the light logic.
 */
object AdPolicy {
    fun bannerAllowed(screenAllowsBanner: Boolean, light: LightSettings, ads: AdsStatus): Boolean =
        screenAllowsBanner && !light.isOn && ads.ready
}
