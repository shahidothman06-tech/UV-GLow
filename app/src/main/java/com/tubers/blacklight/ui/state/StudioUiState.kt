package com.tubers.blacklight.ui.state

import com.tubers.blacklight.BuildConfig
import com.tubers.blacklight.billing.BillingStatus
import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.BuiltInThemes
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
    /** Demo checkout and "Preview Premium (demo)" exist only in debug builds. */
    val demoAllowed: Boolean = BuildConfig.DEBUG,
) {
    /** Premium is unlocked by a real subscription, or (debug builds only) by the labelled demo preview. */
    val isPremium: Boolean get() = billing.hasActiveSubscription || (demoAllowed && prefs.premiumPreview)
    val themes: List<ThemeItem> get() = BuiltInThemes.all + customThemes
}

/** One-off events for the UI (snackbars, navigation). */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data object OpenPremium : UiEvent
}
