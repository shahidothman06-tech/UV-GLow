package com.tubers.blacklight.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Google Play Billing for the two Premium subscriptions.
 *
 * When product IDs are blank (the default), this gateway stays in demo mode: it never connects
 * and every purchase/restore returns NotConfigured, so the UI can show an honest "demo" notice.
 *
 * Production note: purchase tokens should also be verified on your own server before granting
 * entitlement. This client-side check is the minimum Play requires (acknowledge within 3 days).
 */
class PlayBillingGateway(
    context: Context,
    private val monthlyProductId: String,
    private val yearlyProductId: String,
    private val scope: CoroutineScope,
) : BillingGateway {

    private val isConfigured = monthlyProductId.isNotBlank() && yearlyProductId.isNotBlank()

    private val _status = MutableStateFlow(BillingStatus(configured = isConfigured))
    override val status: StateFlow<BillingStatus> = _status.asStateFlow()

    private val productDetails = mutableMapOf<PlanType, ProductDetails>()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { handlePurchases(purchases.orEmpty()) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _status.update { it.copy(lastError = result.debugMessage.ifBlank { "Purchase failed" }) }
        }
    }

    private val client: BillingClient? = if (!isConfigured) null else BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    override fun connect() {
        val c = client ?: return
        if (c.isReady) return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _status.update { it.copy(connected = ok, lastError = if (ok) null else result.debugMessage) }
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

    private fun idFor(plan: PlanType) = if (plan == PlanType.MONTHLY) monthlyProductId else yearlyProductId

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
        val result = c.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _status.update { it.copy(lastError = result.billingResult.debugMessage) }
            return
        }
        val prices = mutableMapOf<PlanType, String>()
        result.productDetailsList.orEmpty().forEach { details ->
            val plan = PlanType.entries.firstOrNull { idFor(it) == details.productId } ?: return@forEach
            productDetails[plan] = details
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()
                ?.formattedPrice?.let { prices[plan] = it }
        }
        _status.update { it.copy(prices = prices) }
    }

    /** Re-reads owned subscriptions from Play. Used at startup and for "Restore purchases". */
    private suspend fun refreshPurchases(): Boolean {
        val c = client ?: return false
        val result = c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _status.update { it.copy(lastError = result.billingResult.debugMessage) }
            return false
        }
        return handlePurchases(result.purchasesList)
    }

    private suspend fun handlePurchases(purchases: List<Purchase>): Boolean {
        val c = client ?: return false
        val ours = setOf(monthlyProductId, yearlyProductId)
        val active = purchases.filter { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.any { it in ours }
        }
        active.filter { !it.isAcknowledged }.forEach { p ->
            c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
        val has = active.isNotEmpty()
        _status.update { it.copy(hasActiveSubscription = has, lastError = null) }
        return has
    }

    override suspend fun purchase(activity: Activity, plan: PlanType): PurchaseOutcome {
        val c = client ?: return PurchaseOutcome.NotConfigured
        if (!c.isReady) {
            connect()
            return PurchaseOutcome.Failed("Connecting to Google Play… try again in a moment.")
        }
        if (productDetails.isEmpty()) loadProducts()
        val details = productDetails[plan]
            ?: return PurchaseOutcome.Failed("This plan isn't available in Google Play yet.")
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return PurchaseOutcome.Failed("No offer is set up for this plan.")
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).build()
        val result = c.launchBillingFlow(activity, flow)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            PurchaseOutcome.Launched
        } else {
            PurchaseOutcome.Failed(result.debugMessage.ifBlank { "Couldn't open Google Play" })
        }
    }

    override suspend fun restore(): RestoreOutcome {
        val c = client ?: return RestoreOutcome.NotConfigured
        if (!c.isReady) {
            connect()
            return RestoreOutcome.Failed("Connecting to Google Play… try again in a moment.")
        }
        return if (refreshPurchases()) RestoreOutcome.Restored else RestoreOutcome.NothingFound
    }
}
