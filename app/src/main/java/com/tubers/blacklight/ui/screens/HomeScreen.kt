package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.TimerLogic
import com.tubers.blacklight.ui.components.FlashlightStage
import com.tubers.blacklight.ui.components.LabeledSlider
import com.tubers.blacklight.ui.components.Note
import com.tubers.blacklight.ui.components.PowerButton
import com.tubers.blacklight.ui.components.QuickChip
import com.tubers.blacklight.ui.components.STAGE_HEIGHT
import com.tubers.blacklight.ui.components.Tag
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.shouldAnimate
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.MonoFamily
import com.tubers.blacklight.ui.theme.UvColors

@Composable
fun HomeScreen(
    state: StudioUiState,
    vm: StudioViewModel,
    onOpenEffects: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenPresets: () -> Unit,
) {
    val light = state.light
    val animate = shouldAnimate(light.isOn, state.prefs.reducedMotion)
    val timerText = TimerLogic.format(state.timer.remainingMs)

    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // Stage spans the full width; the torch stays centered at a fixed size.
        Box(Modifier.fillMaxWidth().height(STAGE_HEIGHT.dp)) {
            FlashlightStage(
                light = light,
                animate = animate,
                contentDescription = "Flashlight illustration, ${if (light.isOn) "on" else "off"}, " +
                    "${light.mode.displayName} in ${light.colorName}",
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Tag(light.mode.displayName)
                if (state.timer.isActive) Tag("Off in $timerText")
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PowerButton(isOn = light.isOn, onClick = vm::togglePower)
            Text(
                buildAnnotatedString {
                    append("Screen light ")
                    withStyle(SpanStyle(color = UvColors.Text)) { append(if (light.isOn) "ON" else "OFF") }
                    if (light.isOn && state.prefs.ledEnabled && state.torchOn) append(" · LED on")
                },
                fontFamily = MonoFamily,
                fontSize = 12.sp,
                color = UvColors.TextSecondary,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            // Quick chips: 4 across, or 2×2 on narrow phones so labels never get cut.
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                val chips: List<@Composable (Modifier) -> Unit> = listOf(
                    { m -> QuickChip("Effect", light.mode.displayName, onOpenEffects, m) },
                    { m -> QuickChip("Color", light.colorName, onOpenThemes, m, dot = Color(light.colorArgb)) },
                    { m -> QuickChip("Auto-off", if (state.timer.isActive) timerText else "Off", onOpenTimer, m, mono = true) },
                    { m -> QuickChip("Presets", "${state.presets.size} saved", onOpenPresets, m) },
                )
                val perRow = if (maxWidth < 330.dp) 2 else 4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.chunked(perRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { chip -> chip(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            UvPanel(Modifier.padding(top = 14.dp)) {
                LabeledSlider("Brightness", "${light.brightness}%", light.brightness, 10..100, vm::setBrightness, showDivider = true)
                LabeledSlider("Beam width", "${light.beamWidth}%", light.beamWidth, 0..100, vm::setBeamWidth, showDivider = true)
                LabeledSlider("Glow radius", "${light.glowRadius}%", light.glowRadius, 0..100, vm::setGlowRadius)
            }

            Note("Screen light effect only — phones can't emit UV light.")
        }
    }
}
