package com.tubers.blacklight.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob with the User Messaging Platform (UMP) consent flow.
 *
 * Order matters for policy: consent is gathered first (Google shows its form in the EEA, UK and
 * Switzerland), and the Mobile Ads SDK is initialised only once [ConsentInformation.canRequestAds]
 * is true. With a blank [bannerAdUnitId] (release without AdMob IDs) nothing here runs at all.
 */
class GoogleAdsGateway(
    context: Context,
    override val bannerAdUnitId: String,
    private val scope: CoroutineScope,
) : AdsGateway {

    private val appContext = context.applicationContext
    private val configured = bannerAdUnitId.isNotBlank()
    private val consent: ConsentInformation? =
        if (configured) UserMessagingPlatform.getConsentInformation(appContext) else null
    private val initStarted = AtomicBoolean(false)

    private val _status = MutableStateFlow(AdsStatus(configured = configured))
    override val status: StateFlow<AdsStatus> = _status.asStateFlow()

    init {
        // Consent given in an earlier session lets ads start without waiting for the update.
        refresh()
    }

    override fun gatherConsent(activity: Activity) {
        val info = consent ?: return
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                // Shows the form only where and when Google requires it.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh() }
            },
            { refresh() }, // Offline etc.: fall back to whatever consent is already stored.
        )
    }

    override fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit) {
        if (consent == null) return onDone(null)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            refresh()
            onDone(error?.message)
        }
    }

    private fun refresh() {
        val info = consent ?: return
        val canRequest = info.canRequestAds()
        _status.update {
            it.copy(
                canRequestAds = canRequest,
                privacyOptionsRequired = info.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED,
            )
        }
        if (canRequest) initialize()
    }

    /** Once per process, off the main thread, as Google recommends. */
    private fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(appContext) { _status.update { it.copy(initialized = true) } }
        }
    }
}
