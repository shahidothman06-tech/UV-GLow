package com.tubers.blacklight.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

enum class PlanType { MONTHLY, YEARLY }

data class BillingStatus(
    /** True only when real product IDs are configured (see gradle.properties). */
    val configured: Boolean = false,
    val connected: Boolean = false,
    /** A real, verified-by-Play active subscription. Demo preview is NOT reflected here. */
    val hasActiveSubscription: Boolean = false,
    /** Localised prices from Play, when available. */
    val prices: Map<PlanType, String> = emptyMap(),
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
    fun connect()
    suspend fun purchase(activity: Activity, plan: PlanType): PurchaseOutcome
    suspend fun restore(): RestoreOutcome
}
