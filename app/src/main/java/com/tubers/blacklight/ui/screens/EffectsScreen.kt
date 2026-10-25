package com.tubers.blacklight.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.PremiumRules
import com.tubers.blacklight.domain.logic.StudioLogic
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.ui.components.BeamPreview
import com.tubers.blacklight.ui.components.Callout
import com.tubers.blacklight.ui.components.LabeledSlider
import com.tubers.blacklight.ui.components.PageTitle
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.shouldAnimate
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType

fun LightMode.icon(): ImageVector = when (this) {
    LightMode.UV_GLOW -> UvIcons.ModeUv
    LightMode.PULSE -> UvIcons.ModePulse
    LightMode.LIGHT_RAYS -> UvIcons.ModeRays
    LightMode.STROBE -> UvIcons.ModeStrobe
    LightMode.SOS -> UvIcons.ModeSos
    LightMode.BLACKLIGHT -> UvIcons.ModeBlack
}

@Composable
fun EffectsScreen(state: StudioUiState, vm: StudioViewModel, onOpenSettings: () -> Unit) {
    val light = state.light
    val animate = shouldAnimate(light.isOn, state.prefs.reducedMotion)
    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        PageTitle("Effects", "Choose how your light moves.")

        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(UvRadius.xl))
                .background(Color(0xFF0A0710))
                .border(1.dp, UvColors.BorderSoft, RoundedCornerShape(UvRadius.xl)),
        ) {
            BeamPreview(light, animate, "${light.mode.displayName} preview", Modifier.fillMaxSize())
        }

        SectionLabel("Modes")
        LightMode.entries.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { mode ->
                    ModeTile(
                        mode = mode,
                        selected = mode == light.mode,
                        locked = PremiumRules.isModeLocked(mode, state.isPremium),
                        onClick = { vm.selectMode(mode) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }

        UvPanel(Modifier.padding(top = 4.dp)) {
            LabeledSlider(
                "Animation speed", StudioLogic.formatSpeed(light.speedTenths), light.speedTenths,
                LightSettings.SPEED_MIN_TENTHS..LightSettings.SPEED_MAX_TENTHS, vm::setSpeed,
            )
        }

        if (state.prefs.reducedMotion) {
            Callout(UvIcons.Wave) {
                Text("Reduced motion is on, so beams stay steady.", fontSize = 12.sp, color = UvColors.TextBody)
                TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        "Change in Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = LocalAccent.current.text, textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
        if (light.mode == LightMode.STROBE) {
            Callout(UvIcons.Shield) {
                Text(
                    "Strobe is capped at 3 flashes per second. Skip it if flashing light bothers you.",
                    fontSize = 12.sp, color = UvColors.TextBody,
                )
            }
        }
    }
}

@Composable
private fun ModeTile(mode: LightMode, selected: Boolean, locked: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val accent = LocalAccent.current
    val border by animateColorAsState(if (selected) accent.color else UvColors.Border, label = "tileBorder")
    val bg by animateColorAsState(
        if (selected) accent.color.copy(alpha = 0.14f).compositeOverSurface() else UvColors.Surface,
        label = "tileBg",
    )
    Box(
        modifier
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(UvRadius.l))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(UvRadius.l))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { if (locked) stateDescription = "Premium, locked" }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(UvRadius.s)).background(UvColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(mode.icon(), null, tint = if (selected) accent.text else UvColors.TextSoft, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(mode.displayName, style = UvType.bodyStrong, color = UvColors.Text)
            Text(mode.description, style = UvType.small)
        }
        if (locked) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(UvColors.LockBg)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(UvIcons.Crown, null, tint = UvColors.LockText, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Premium", fontSize = 10.sp, color = UvColors.LockText)
            }
        }
    }
}

/** Blend a translucent color over the surface color so animations interpolate opaque values. */
private fun Color.compositeOverSurface(): Color {
    val a = alpha
    return Color(
        red = red * a + UvColors.Surface.red * (1 - a),
        green = green * a + UvColors.Surface.green * (1 - a),
        blue = blue * a + UvColors.Surface.blue * (1 - a),
        alpha = 1f,
    )
}
