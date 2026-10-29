package com.tubers.blacklight.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing (Play Billing Library 8+) for the monthly Premium subscription.
 *
 * When the product ID is blank (the default), this gateway stays in demo mode: it never connects
 * and every purchase/restore returns NotConfigured, so debug builds can show an honest "demo"
 * notice and release builds an error.
 *
 * Production note: purchase tokens should also be verified on your own server before granting
 * entitlement. This client-side check is the minimum Play requires (acknowledge within 3 days).
 */
class PlayBillingGateway(
    context: Context,
    private val monthlyProductId: String,
    private val scope: CoroutineScope,
) : BillingGateway {

    private val isConfigured = monthlyProductId.isNotBlank()

    private val _status = MutableStateFlow(BillingStatus(configured = isConfigured))
    override val status: StateFlow<BillingStatus> = _status.asStateFlow()

    private val productDetails = mutableMapOf<PlanType, ProductDetails>()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch { handlePurchases(purchases.orEmpty(), fullList = false) }
            BillingResponseCode.USER_CANCELED -> Unit
            // Bought on another device or before a reinstall: just pick it up.
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refreshPurchases() }
            else -> _status.update { it.copy(lastError = messageFor(result, "Purchase failed")) }
        }
    }

    private val client: BillingClient? = if (!isConfigured) null else BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        // Required since Billing 8; subscriptions can also be pending (e.g. cash payments).
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        // Billing 8+: reconnects on its own when a call finds the service disconnected.
        .enableAutoServiceReconnection()
        .build()

    override fun connect() {
        val c = client ?: return
        if (c.isReady) return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingResponseCode.OK
                _status.update {
                    it.copy(
                        connected = ok,
                        unavailable = !ok && result.responseCode in UNAVAILABLE_CODES,
                        lastError = if (ok) null else messageFor(result, "Couldn't connect to Google Play"),
                    )
                }
                // Restore on launch: owned subscriptions are re-read every time the app connects.
                if (ok) scope.launch {
                    loadProducts()
                    refreshPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _status.update { it.copy(connected = false) }
            }
        })
    }

    private fun idFor(plan: PlanType) = when (plan) {
        PlanType.MONTHLY -> monthlyProductId
    }

    private fun planFor(productId: String) = PlanType.entries.firstOrNull { idFor(it) == productId }

    private suspend fun loadProducts() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PlanType.entries.map { plan ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(idFor(plan))
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()
        val (result, details) = c.queryProductDetailsFull(params)
        if (result.responseCode != BillingResponseCode.OK) {
            _status.update {
                it.copy(
                    unavailable = it.unavailable || result.responseCode in UNAVAILABLE_CODES,
                    lastError = messageFor(result, "Couldn't load plans from Google Play"),
                )
            }
            return
        }
        val plans = mutableMapOf<PlanType, PlanOffer>()
        details.productDetailsList.forEach { pd ->
            val plan = planFor(pd.productId) ?: return@forEach
            val offer = PlanPricing.pickOffer(pd.subscriptionOfferDetails.orEmpty().map { it.toPlanOffer(pd.productId) })
            if (offer != null) {
                productDetails[plan] = pd
                plans[plan] = offer
            }
        }
        // Billing 8+: products Play couldn't return are listed instead of silently missing.
        val unfetched = details.unfetchedProductList.mapNotNull { planFor(it.productId) }.toSet() +
            PlanType.entries.filter { it !in plans }
        _status.update { it.copy(plans = plans, unavailablePlans = unfetched) }
    }

    /** Callback API, because billing-ktx's wrapper drops the unfetched-product list. */
    private suspend fun BillingClient.queryProductDetailsFull(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, QueryProductDetailsResult> = suspendCancellableCoroutine { cont ->
        queryProductDetailsAsync(params) { result, details -> if (cont.isActive) cont.resume(result to details) }
    }

    /** Re-reads owned subscriptions from Play. Used at launch and for "Restore purchases". */
    private suspend fun refreshPurchases(): Boolean? {
        val c = client ?: return null
        val result = c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            _status.update { it.copy(lastError = messageFor(result.billingResult, "Couldn't check your purchases")) }
            return null
        }
        return handlePurchases(result.purchasesList, fullList = true)
    }

    /**
     * [fullList] is true for queryPurchasesAsync (everything the user owns), false for listener
     * updates (only what just changed), which must never revoke an existing subscription.
     */
    private suspend fun handlePurchases(purchases: List<Purchase>, fullList: Boolean): Boolean {
        val c = client ?: return false
        val ours = purchases.filter { p -> p.products.any { planFor(it) != null } }
        val active = ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = ours.any { it.purchaseState == Purchase.PurchaseState.PENDING }

        var ackError: String? = null
        active.filter { !it.isAcknowledged }.forEach { p ->
            val ack = c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
            // Not fatal: the next launch retries, well inside Play's 3-day window.
            if (ack.responseCode != BillingResponseCode.OK) ackError = messageFor(ack, "Couldn't confirm the purchase with Google Play")
        }

        val activeId = active.firstNotNullOfOrNull { p -> p.products.firstOrNull { planFor(it) != null } }
        _status.update {
            val has = if (fullList) activeId != null else it.hasActiveSubscription || activeId != null
            it.copy(
                hasActiveSubscription = has,
                activeProductId = activeId ?: it.activeProductId.takeIf { _ -> has },
                pendingPurchase = pending && activeId == null,
                lastError = ackError,
            )
        }
        return _status.value.hasActiveSubscription
    }

    override suspend fun purchase(activity: Activity, plan: PlanType): PurchaseOutcome {
        val c = client ?: return PurchaseOutcome.NotConfigured
        if (!c.isReady) {
            connect()
            return PurchaseOutcome.Failed("Connecting to Google Play… try again in a moment.")
        }
        if (productDetails[plan] == null) loadProducts()
        val details = productDetails[plan]
            ?: return PurchaseOutcome.Failed("This plan isn't available in Google Play right now.")
        val offer = _status.value.plans[plan]
            ?: return PurchaseOutcome.Failed("No offer is set up for this plan.")
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            ).build()
        val result = c.launchBillingFlow(activity, flow)
        return when (result.responseCode) {
            BillingResponseCode.OK -> PurchaseOutcome.Launched
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refreshPurchases()
                PurchaseOutcome.Failed("You already have Premium — it's been restored.")
            }
            else -> PurchaseOutcome.Failed(messageFor(result, "Couldn't open Google Play"))
        }
    }

    override suspend fun restore(): RestoreOutcome {
        val c = client ?: return RestoreOutcome.NotConfigured
        if (!c.isReady) {
            connect()
            return RestoreOutcome.Failed("Connecting to Google Play… try again in a moment.")
        }
        return when (refreshPurchases()) {
            true -> RestoreOutcome.Restored
            false -> RestoreOutcome.NothingFound
            null -> RestoreOutcome.Failed(_status.value.lastError ?: "Couldn't check your purchases")
        }
    }

    private fun ProductDetails.SubscriptionOfferDetails.toPlanOffer(productId: String) = PlanOffer(
        productId = productId,
        offerToken = offerToken,
        phases = pricingPhases.pricingPhaseList.map { p ->
            PricePhase(
                formattedPrice = p.formattedPrice,
                priceMicros = p.priceAmountMicros,
                currencyCode = p.priceCurrencyCode,
                billingPeriod = p.billingPeriod,
                cycles = p.billingCycleCount,
                renewsForever = p.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING,
            )
        },
    )

    private companion object {
        val UNAVAILABLE_CODES = setOf(BillingResponseCode.BILLING_UNAVAILABLE, BillingResponseCode.FEATURE_NOT_SUPPORTED)

        /** Plain-language errors; Play's debug messages are for developers, not people. */
        fun messageFor(result: BillingResult, fallback: String): String = when (result.responseCode) {
            BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing isn't available on this device or account."
            BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "Subscriptions aren't supported by this version of Google Play."
            BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.NETWORK_ERROR ->
                "Can't reach Google Play. Check your connection and try again."
            BillingResponseCode.ITEM_UNAVAILABLE -> "This plan isn't available in your country."
            else -> fallback
        }
    }
}
