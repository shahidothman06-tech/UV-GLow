package com.tubers.blacklight.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

enum class PlanType { MONTHLY, YEARLY }

data class BillingStatus(
    /** True only when real product IDs are configured (see gradle.properties). */
    val configured: Boolean = false,
    val connected: Boolean = false,
    /**
     * Google Play Billing can't be used here (no Play Store, unsupported account or country, or
     * setup failed). Release builds show an error instead of any demo.
     */
    val unavailable: Boolean = false,
    /** A real, verified-by-Play active subscription. Demo preview is NOT reflected here. */
    val hasActiveSubscription: Boolean = false,
    /** Product ID of the active subscription, for the Play "Manage subscription" deep link. */
    val activeProductId: String? = null,
    /** A purchase is waiting for payment (e.g. cash at a shop); Premium unlocks once it clears. */
    val pendingPurchase: Boolean = false,
    /** The offer Play returned for each plan, with localised prices and periods. */
    val plans: Map<PlanType, PlanOffer> = emptyMap(),
    /** Plans Play couldn't return (not published, wrong ID, or not sold in this country). */
    val unavailablePlans: Set<PlanType> = emptySet(),
    val lastError: String? = null,
)

sealed interface PurchaseOutcome {
    /** Play's purchase sheet is showing; the result arrives via [BillingGateway.status]. */
    data object Launched : PurchaseOutcome
    /** Billing isn't set up in this build: show the demo notice, never fake a purchase. */
    data object NotConfigured : PurchaseOutcome
    data class Failed(val message: String) : PurchaseOutcome
}

sealed interface RestoreOutcome {
    data object Restored : RestoreOutcome
    data object NothingFound : RestoreOutcome
    data object NotConfigured : RestoreOutcome
    data class Failed(val message: String) : RestoreOutcome
}

interface BillingGateway {
    val status: StateFlow<BillingStatus>
    /** Connects to Play, loads the plans and restores owned subscriptions. Safe to call again. */
    fun connect()
    suspend fun purchase(activity: Activity, plan: PlanType): PurchaseOutcome
    suspend fun restore(): RestoreOutcome
}
