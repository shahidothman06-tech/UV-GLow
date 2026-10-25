package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.R
import com.tubers.blacklight.billing.PlanOffer
import com.tubers.blacklight.billing.PlanPricing
import com.tubers.blacklight.billing.PlanType
import com.tubers.blacklight.domain.logic.PremiumRules
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.components.Callout
import com.tubers.blacklight.ui.components.GhostButton
import com.tubers.blacklight.ui.components.Note
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.SwitchRow
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.findActivity
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType

@Composable
fun PremiumScreen(state: StudioUiState, vm: StudioViewModel) {
    val accent = LocalAccent.current
    val context = LocalContext.current
    var plan by rememberSaveable { mutableStateOf(PlanType.YEARLY) }
    val billing = state.billing
    // Debug builds without product IDs run a labelled demo. Release builds never do: if billing
    // isn't set up or Play can't be used, they show an error and no checkout.
    val demo = !billing.configured && state.demoAllowed
    val billingError = !demo && (!billing.configured || billing.unavailable)
    val selectedOffer = billing.plans[plan]

    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        // Hero (extra top room so the icon glow is never clipped).
        Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).drawBehind {
                    drawCircle(
                        Brush.radialGradient(0f to accent.color.copy(alpha = 0.45f), 1f to Color.Transparent, radius = size.minDimension),
                        radius = size.minDimension,
                    )
                },
            ) {
                Image(painterResource(R.drawable.uv_glow_logo), null, Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)))
            }
            Text("UV Glow Premium", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp, color = UvColors.Text, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Text("Every effect, unlimited presets and themes.", style = UvType.lede, textAlign = TextAlign.Center)
        }

        SectionLabel("Free vs Premium")
        UvPanel(contentPadding = PaddingValues(0.dp)) {
            CompareRow("Feature", "Free", "Premium", header = true, premiumColor = accent.text)
            CompareRow("Light effects", "4", "All 6")
            CompareRow("Strobe & Blacklight", "—", "Included")
            CompareRow("Saved presets", "${PremiumRules.FREE_PRESET_LIMIT}", "Unlimited")
            CompareRow("Custom themes", "${PremiumRules.FREE_CUSTOM_THEME_LIMIT}", "Unlimited")
            CompareRow("Auto-off timer", "Yes", "Yes", last = true)
        }

        if (billingError && !billing.hasActiveSubscription) {
            Callout(UvIcons.Info) {
                Text(
                    "Premium isn't available right now. Google Play Billing couldn't be reached on this device" +
                        (billing.lastError?.let { " ($it)" } ?: "") + ". Check that Google Play is installed and " +
                        "signed in, then try again.",
                    fontSize = 12.sp, color = UvColors.TextBody, lineHeight = 18.sp,
                )
            }
        } else {
            SectionLabel("Choose a plan")
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanType.entries.forEach { type ->
                    PlanCard(
                        title = if (type == PlanType.MONTHLY) "Monthly" else "Yearly",
                        offer = billing.plans[type],
                        state = when {
                            demo -> PlanCardState.DEMO
                            type in billing.unavailablePlans -> PlanCardState.UNAVAILABLE
                            else -> PlanCardState.LOADING
                        },
                        badge = if (type == PlanType.YEARLY) "BEST VALUE" else null,
                        selected = plan == type,
                        onClick = { plan = type },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            AccentButton(
                text = if (demo) "Continue · demo checkout" else "Continue",
                onClick = { context.findActivity()?.let { vm.purchase(it, plan) } },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                enabled = demo || selectedOffer != null,
            )
            // Play policy: the renewal terms sit right next to the purchase button.
            Text(
                RENEWAL_TERMS + (selectedOffer?.let { " ${PlanPricing.introLabel(it) ?: PlanPricing.recurringLabel(it)}." } ?: ""),
                fontSize = 11.sp, color = UvColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            GhostButton("Restore purchases", vm::restorePurchases, Modifier.fillMaxWidth().padding(top = 10.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Subscription status", fontSize = 12.sp, color = UvColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(
                when {
                    billing.hasActiveSubscription -> "Premium active"
                    billing.pendingPurchase -> "Payment pending"
                    state.demoAllowed && state.prefs.premiumPreview -> "Premium preview (demo)"
                    else -> "Free plan"
                },
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = UvColors.Text,
            )
        }
        if (billing.pendingPurchase) {
            Note("Your purchase is waiting for payment. Premium unlocks as soon as Google Play confirms it.", icon = null)
        }

        if (state.demoAllowed) {
            UvPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                SwitchRow(
                    icon = null,
                    title = "Preview Premium (demo)",
                    subtitle = "Debug builds only. Unlocks Premium features on this device so you can try them. Nothing is bought.",
                    checked = state.prefs.premiumPreview,
                    onCheckedChange = vm::setPremiumPreview,
                )
            }
        }

        Text(
            if (demo) {
                "Demo — billing isn't connected and no payment is taken. Real prices load from Google Play once " +
                    "product IDs are set in gradle.properties."
            } else {
                "Payment is handled by Google Play. Manage or cancel anytime in Google Play › Subscriptions, " +
                    "or from Settings › Manage subscription."
            },
            fontSize = 11.sp, color = UvColors.TextMuted, textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )
    }
}

private const val RENEWAL_TERMS = "Renews automatically until cancelled. Cancel anytime in Google Play."

private enum class PlanCardState { DEMO, LOADING, UNAVAILABLE }

@Composable
private fun CompareRow(feature: String, free: String, premium: String, header: Boolean = false, last: Boolean = false, premiumColor: Color = UvColors.Text) {
    val style = if (header) UvType.label.copy(fontSize = 10.sp, letterSpacing = 1.2.sp) else UvType.body.copy(fontSize = 12.sp)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (header) 38.dp else 44.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (header) feature.uppercase() else feature, style = style, color = if (header) UvColors.TextSecondary else UvColors.Text, modifier = Modifier.weight(1f))
            Text(if (header) free.uppercase() else free, style = style, color = UvColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
            Text(if (header) premium.uppercase() else premium, style = style, color = if (header) premiumColor else UvColors.Text, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
        }
        if (!last) HorizontalDivider(color = UvColors.Divider)
    }
}

/**
 * One plan. The main figure is Play's localised renewing price for that plan's own period (so the
 * yearly card leads with the yearly price), with any trial or intro price spelled out below it.
 */
@Composable
private fun PlanCard(
    title: String,
    offer: PlanOffer?,
    state: PlanCardState,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val price = offer?.recurring?.formattedPrice ?: "—"
    val detail = when {
        offer != null -> "per ${PlanPricing.periodUnit(offer.recurring.billingPeriod)}"
        state == PlanCardState.DEMO -> "Demo · price from Google Play"
        state == PlanCardState.UNAVAILABLE -> "Not available"
        else -> "Loading price…"
    }
    val intro = offer?.let { PlanPricing.introLabel(it) }
    val accent = LocalAccent.current
    Column(
        modifier
            .heightIn(min = 112.dp)
            .clip(RoundedCornerShape(UvRadius.l))
            .background(UvColors.Surface)
            .border(1.dp, if (selected) accent.color else UvColors.Border, RoundedCornerShape(UvRadius.l))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Badge sits on its own line so it never overlaps the title on narrow phones.
        if (badge != null) {
            Text(
                badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accent.onColor,
                modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(5.dp)).background(accent.color).padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Text(title, style = UvType.bodyStrong, color = UvColors.Text)
        Text(price, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, color = UvColors.Text, modifier = Modifier.padding(top = 6.dp))
        Text(detail, style = UvType.small)
        if (intro != null) {
            Text(intro, style = UvType.small, color = accent.text, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
