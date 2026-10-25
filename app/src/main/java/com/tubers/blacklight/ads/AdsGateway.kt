package com.tubers.blacklight.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdsStatus(
    /** This build has a banner ad unit ID (always in debug; release only if set in gradle.properties). */
    val configured: Boolean = false,
    /** Google's consent check (UMP) allows requesting ads: consent given, or not required here. */
    val canRequestAds: Boolean = false,
    /** The Mobile Ads SDK finished initialising. It is only initialised once [canRequestAds] is true. */
    val initialized: Boolean = false,
    /** The user is in a region (EEA, UK, Switzerland) where they must be able to change their choice. */
    val privacyOptionsRequired: Boolean = false,
) {
    /** Ads may be loaded right now. */
    val ready: Boolean get() = configured && canRequestAds && initialized
}

/** AdMob behind an interface, like [com.tubers.blacklight.billing.BillingGateway], so the view model stays testable. */
interface AdsGateway {
    val status: StateFlow<AdsStatus>
    val bannerAdUnitId: String

    /** Asks for consent where required (showing Google's form), then initialises ads if allowed. Call on launch. */
    fun gatherConsent(activity: Activity)

    /** Reopens Google's consent form ("Privacy & ad choices"). [onDone] gets an error message, or null. */
    fun showPrivacyOptions(activity: Activity, onDone: (error: String?) -> Unit)
}

/** Builds without ads (and the default for tests): never consents, never shows anything. */
object NoAdsGateway : AdsGateway {
    override val status: StateFlow<AdsStatus> = MutableStateFlow(AdsStatus()).asStateFlow()
    override val bannerAdUnitId: String = ""
    override fun gatherConsent(activity: Activity) = Unit
    override fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit) = onDone(null)
}
