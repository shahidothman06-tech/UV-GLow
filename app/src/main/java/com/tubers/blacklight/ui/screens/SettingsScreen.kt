package com.tubers.blacklight.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.AppLinks
import com.tubers.blacklight.BuildConfig
import com.tubers.blacklight.domain.model.AppLanguages
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.components.MiniButton
import com.tubers.blacklight.ui.components.NavRow
import com.tubers.blacklight.ui.components.PageTitle
import com.tubers.blacklight.ui.components.RowIcon
import com.tubers.blacklight.ui.components.RowText
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.SwitchRow
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.findActivity
import com.tubers.blacklight.ui.components.openExternal
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.UvColors

@Composable
fun SettingsScreen(
    state: StudioUiState,
    vm: StudioViewModel,
    onReplayIntro: () -> Unit,
    onOpenPrivacy: () -> Unit,
    /** Null hides the Language row (release builds until translations exist). */
    onOpenLanguage: (() -> Unit)?,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val groupPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
    val context = LocalContext.current
    // Back (including predictive back) closes the inline confirmation before leaving the tab.
    BackHandler(enabled = confirmReset) { confirmReset = false }

    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        PageTitle("Settings", "Comfort, hardware and app options.")

        SectionLabel("Accessibility", top = 8.dp)
        UvPanel(contentPadding = groupPadding) {
            SwitchRow(
                icon = UvIcons.Wave,
                title = "Reduced motion",
                subtitle = "Keeps beams steady and turns off animated effects.",
                checked = state.prefs.reducedMotion,
                onCheckedChange = vm::setReducedMotion,
            )
        }

        SectionLabel("Hardware")
        UvPanel(contentPadding = groupPadding) {
            SwitchRow(
                icon = UvIcons.Bolt,
                title = "Phone flashlight (LED)",
                subtitle = if (state.torchAvailable) {
                    "Also turns on your camera LED. It gives ordinary white light, not UV."
                } else {
                    "Not available — this phone has no flashlight LED."
                },
                checked = state.prefs.ledEnabled && state.torchAvailable,
                onCheckedChange = vm::setLedEnabled,
                enabled = state.torchAvailable,
            )
        }

        // Shown with Premium itself (release builds need a product ID).
        if (state.premiumVisible) {
            SectionLabel("Subscription")
            UvPanel(contentPadding = groupPadding) {
                NavRow(
                    UvIcons.Crown,
                    "Manage subscription",
                    if (state.billing.hasActiveSubscription) "Change plan or cancel in Google Play" else "See your subscriptions in Google Play",
                    {
                        val url = AppLinks.manageSubscriptionUrl(state.billing.activeProductId)
                        if (!context.openExternal(url)) vm.showMessage("Couldn’t open Google Play")
                    },
                )
            }
        }

        SectionLabel("App")
        UvPanel(contentPadding = groupPadding) {
            if (onOpenLanguage != null) {
                NavRow(
                    UvIcons.Globe, "Language",
                    (AppLanguages.find(state.prefs.languageTag) ?: AppLanguages.all.first()).nativeName, onOpenLanguage,
                )
            }
            NavRow(UvIcons.Refresh, "Replay splash screen", "See the welcome screen again", onReplayIntro)
            HorizontalDivider(color = UvColors.Divider)
            if (!confirmReset) {
                NavRow(UvIcons.Trash, "Reset settings", "Restore default light and comfort settings", { confirmReset = true })
            } else {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RowIcon(UvIcons.Trash)
                    RowText("Reset all settings?", "Presets and themes are kept.")
                    MiniButton("Cancel", { confirmReset = false })
                    AccentButton(
                        "Reset", { vm.resetSettings(); confirmReset = false },
                        height = 40.dp, container = UvColors.Danger, content = UvColors.OnDanger,
                    )
                }
            }
            HorizontalDivider(color = UvColors.Divider)
            val rotation by animateFloatAsState(if (showAbout) 90f else 0f, label = "about")
            NavRow(UvIcons.Info, "About & safety", "Version ${BuildConfig.VERSION_NAME}", { showAbout = !showAbout }, trailingRotation = rotation)
            AnimatedVisibility(showAbout) {
                Column {
                    Text(
                        "UV Glow draws light effects on your screen. A phone screen can't emit ultraviolet light, " +
                            "so the app can't reveal stains, check banknotes or disinfect anything. The optional LED is an " +
                            "ordinary white camera flash. For real UV work, use certified UV hardware and never look " +
                            "straight into the source.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = UvColors.TextBody,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    HorizontalDivider(color = UvColors.Divider)
                    NavRow(
                        UvIcons.Shield,
                        "Privacy policy",
                        "What the app stores and how ads use data",
                        onOpenPrivacy,
                    )
                    // Required by Google's consent rules wherever the consent form applies (EEA, UK, Switzerland).
                    if (state.ads.privacyOptionsRequired) {
                        HorizontalDivider(color = UvColors.Divider)
                        NavRow(
                            UvIcons.Info,
                            "Privacy & ad choices",
                            "Change your consent for personalised ads",
                            { context.findActivity()?.let(vm::openAdChoices) },
                        )
                    }
                }
            }
        }
    }
}
