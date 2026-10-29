package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.R
import com.tubers.blacklight.billing.PlanOffer
import com.tubers.blacklight.billing.PlanPricing
import com.tubers.blacklight.billing.PlanType
import com.tubers.blacklight.domain.logic.PremiumRules
import com.tubers.blacklight.ui.components.Callout
import com.tubers.blacklight.ui.components.Note
import com.tubers.blacklight.ui.components.SwitchRow
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.findActivity
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.MonoFamily
import com.tubers.blacklight.ui.theme.UvColors

/** Full-screen paywall. [onClose] is the top-left close button (same as Back). */
@Composable
fun PremiumScreen(state: StudioUiState, vm: StudioViewModel, onClose: () -> Unit) {
    val accent = LocalAccent.current
    val context = LocalContext.current
    // Premium is sold as one monthly subscription.
    val plan = PlanType.MONTHLY
    val billing = state.billing
    // Debug builds without product IDs run a labelled demo. Release builds never do: if billing
    // isn't set up or Play can't be used, they show an error and no checkout.
    val demo = !billing.configured && state.demoAllowed
    val billingError = !demo && (!billing.configured || billing.unavailable)
    val showCheckout = !(billingError && !billing.hasActiveSubscription)
    val selectedOffer = billing.plans[plan]

    Box(
        Modifier
            .fillMaxSize()
            .background(accent.ground)
            // The violet glow behind the hero.
            .drawBehind {
                val c = Offset(size.width / 2f, size.minDimension * 0.18f)
                val r = size.minDimension * 0.85f
                drawCircle(
                    Brush.radialGradient(
                        0f to accent.color.copy(alpha = 0.34f), 0.45f to accent.color.copy(alpha = 0.1f), 1f to Color.Transparent,
                        center = c, radius = r,
                    ),
                    radius = r, center = c,
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(UvIcons.Close, "Close", onClose)
                Spacer(Modifier.weight(1f))
                if (showCheckout) {
                    Box(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(role = Role.Button, onClick = vm::restorePurchases)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Restore purchases", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = UvColors.TextBody)
                    }
                }
            }

            // Hero.
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(96.dp).drawBehind {
                        drawCircle(
                            Brush.radialGradient(0f to accent.color.copy(alpha = 0.5f), 1f to Color.Transparent, radius = size.minDimension),
                            radius = size.minDimension,
                        )
                    },
                ) {
                    Image(painterResource(R.drawable.uv_glow_logo), null, Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
                }
                Row(
                    Modifier
                        .padding(top = 22.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.color.copy(alpha = 0.14f))
                        .border(1.dp, accent.color.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(UvIcons.Crown, null, tint = accent.text, modifier = Modifier.size(13.dp))
                    Text("UV GLOW PREMIUM", fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp, color = accent.text)
                }
                Text(
                    "Unlock the full glow", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp,
                    color = UvColors.Text, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp).semantics { heading() },
                )
                Text(
                    "Every effect, unlimited presets and your own themes.",
                    fontSize = 14.sp, lineHeight = 21.sp, color = UvColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 30.dp, end = 30.dp),
                )
            }

            // What Premium adds.
            UvPanel(Modifier.padding(top = 28.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                BenefitRow(UvIcons.ModeRays, "All 6 light effects", "Strobe and Blacklight included")
                HorizontalDivider(color = UvColors.Divider)
                BenefitRow(UvIcons.NavPresets, "Unlimited presets", "Free plan keeps ${PremiumRules.FREE_PRESET_LIMIT}")
                HorizontalDivider(color = UvColors.Divider)
                BenefitRow(UvIcons.Palette, "Unlimited custom themes", "Name, save and apply any colour")
            }

            if (!showCheckout) {
                Callout(UvIcons.Info, Modifier.padding(top = 18.dp)) {
                    Text(
                        "Premium isn't available right now. Google Play Billing couldn't be reached on this device" +
                            (billing.lastError?.let { " ($it)" } ?: "") + ". Check that Google Play is installed and " +
                            "signed in, then try again.",
                        fontSize = 12.sp, color = UvColors.TextBody, lineHeight = 18.sp,
                    )
                }
            } else {
                PlanCard(
                    title = "Monthly",
                    offer = selectedOffer,
                    state = when {
                        demo -> PlanCardState.DEMO
                        plan in billing.unavailablePlans -> PlanCardState.UNAVAILABLE
                        else -> PlanCardState.LOADING
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
                selectedOffer?.let { PlanPricing.introLabel(it) }?.let { intro ->
                    Text(
                        intro, fontSize = 12.5.sp, color = accent.text, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                ContinueButton(
                    text = if (demo) "Continue · demo checkout" else "Continue",
                    enabled = demo || selectedOffer != null,
                    onClick = { context.findActivity()?.let { vm.purchase(it, plan) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                // Play policy: the renewal terms sit right next to the purchase button.
                Text(
                    RENEWAL_TERMS + (selectedOffer?.let { " ${PlanPricing.introLabel(it) ?: PlanPricing.recurringLabel(it)}." } ?: ""),
                    fontSize = 11.5.sp, color = UvColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 8.dp, end = 8.dp),
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 10.dp)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UvColors.Field)
                    .border(1.dp, UvColors.Divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Subscription status", fontSize = 12.sp, color = UvColors.TextSecondary, modifier = Modifier.weight(1f))
                Text(
                    when {
                        billing.hasActiveSubscription -> "Premium active"
                        billing.pendingPurchase -> "Payment pending"
                        state.demoAllowed && state.prefs.premiumPreview -> "Premium preview (demo)"
                        else -> "Free plan"
                    },
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text,
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
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }
}

private const val RENEWAL_TERMS = "Renews automatically until cancelled. Cancel anytime in Google Play."

private enum class PlanCardState { DEMO, LOADING, UNAVAILABLE }

/** Round 44 dp icon button used for Close and Back on the full-screen pages. */
@Composable
internal fun RoundIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(UvColors.Surface.copy(alpha = 0.7f))
            .border(1.dp, UvColors.Border, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = UvColors.TextBody, modifier = Modifier.size(18.dp)) }
}

/** The big glowing call-to-action used by the paywall and the language screen. */
@Composable
internal fun ContinueButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Box(
        modifier
            .height(56.dp)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        Brush.radialGradient(
                            0f to accent.color.copy(alpha = 0.35f), 1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height * 0.7f), radius = size.width * 0.6f,
                        ),
                        topLeft = Offset(-size.width * 0.1f, 0f),
                        size = Size(size.width * 1.2f, size.height * 1.5f),
                    )
                }
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) accent.color else UvColors.SurfaceRaised)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val content = if (enabled) accent.onColor else UvColors.TextMuted
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
            Icon(UvIcons.ArrowRight, null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent.text, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text)
            Text(subtitle, fontSize = 12.5.sp, color = UvColors.TextSecondary)
        }
        Icon(UvIcons.Check, null, tint = accent.text, modifier = Modifier.size(20.dp))
    }
}

/**
 * The plan card. The main figure is Play's localised renewing price for the plan's own period, with
 * any trial or intro price spelled out below the card.
 */
@Composable
private fun PlanCard(title: String, offer: PlanOffer?, state: PlanCardState, modifier: Modifier) {
    val accent = LocalAccent.current
    val price = offer?.recurring?.formattedPrice ?: "—"
    val detail = when {
        offer != null -> "per ${PlanPricing.periodUnit(offer.recurring.billingPeriod)}"
        state == PlanCardState.DEMO -> "Demo · price from Google Play"
        state == PlanCardState.UNAVAILABLE -> "Not available"
        else -> "Loading price…"
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .heightIn(min = 84.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.color.copy(alpha = 0.12f), UvColors.Surface)))
            .border(1.5.dp, accent.color, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(accent.color), contentAlignment = Alignment.Center) {
            Icon(UvIcons.Check, null, tint = accent.onColor, modifier = Modifier.size(13.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text)
            Text("Cancel anytime", fontSize = 12.5.sp, color = UvColors.TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(price, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = UvColors.Text)
            Text(detail, fontSize = 12.sp, color = UvColors.TextSecondary, textAlign = TextAlign.End)
        }
    }
}
