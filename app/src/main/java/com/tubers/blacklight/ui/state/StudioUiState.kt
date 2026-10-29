package com.tubers.blacklight.ui.state

import com.tubers.blacklight.BuildConfig
import com.tubers.blacklight.ads.AdPolicy
import com.tubers.blacklight.ads.AdsStatus
import com.tubers.blacklight.billing.BillingStatus
import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.BuiltInThemes
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.ThemeItem
import com.tubers.blacklight.domain.model.TimerState

/** Everything the UI renders, in one immutable snapshot. */
data class StudioUiState(
    val loaded: Boolean = false,
    val light: LightSettings = LightSettings(),
    val prefs: AppPreferences = AppPreferences(),
    val presets: List<Preset> = emptyList(),
    val customThemes: List<ThemeItem> = emptyList(),
    val timer: TimerState = TimerState(),
    val torchAvailable: Boolean = false,
    val torchOn: Boolean = false,
    val billing: BillingStatus = BillingStatus(),
    val ads: AdsStatus = AdsStatus(),
    /** Demo checkout and "Preview Premium (demo)" exist only in debug builds. */
    val demoAllowed: Boolean = BuildConfig.DEBUG,
    /**
     * Premium entry points (header crown, Premium screen, Manage subscription). Always in debug;
     * in release only when built with a Play product ID (BuildConfig.PREMIUM_ENABLED).
     */
    val premiumVisible: Boolean = BuildConfig.PREMIUM_ENABLED,
    /** Strobe/SOS is waiting on the flashing-lights warning; the dialog shows while this is set. */
    val flashWarningFor: LightMode? = null,
) {
    /** False while a flashing mode is shown but its warning hasn't been accepted: keep it still. */
    val flashingAllowed: Boolean get() = !light.mode.flashes || prefs.flashWarningAccepted

    /** Premium is unlocked by a real subscription, or (debug builds only) by the labelled demo preview. */
    val isPremium: Boolean get() = billing.hasActiveSubscription || (demoAllowed && prefs.premiumPreview)
    val themes: List<ThemeItem> get() = BuiltInThemes.all + customThemes

    /** Whether a banner may show on a screen that allows one ([AdPolicy]: light off, consent, SDK ready). */
    fun bannerAllowed(screenAllowsBanner: Boolean): Boolean = AdPolicy.bannerAllowed(screenAllowsBanner, light, ads)
}

/** One-off events for the UI (snackbars, navigation). */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data object OpenPremium : UiEvent
}
