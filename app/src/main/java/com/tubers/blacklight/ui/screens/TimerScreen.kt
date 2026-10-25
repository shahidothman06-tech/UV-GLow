package com.tubers.blacklight.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.TimerLogic
import com.tubers.blacklight.domain.model.TimerState
import com.tubers.blacklight.domain.model.TimerStatus
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.components.Callout
import com.tubers.blacklight.ui.components.GhostButton
import com.tubers.blacklight.ui.components.Note
import com.tubers.blacklight.ui.components.ReadableWidth
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.MonoFamily
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvType

@Composable
fun TimerScreen(state: StudioUiState, vm: StudioViewModel) {
    val timer = state.timer
    val fraction by animateFloatAsState(
        TimerLogic.fractionRemaining(timer),
        tween(durationMillis = if (timer.status == TimerStatus.RUNNING) 300 else 200, easing = LinearEasing),
        label = "ring",
    )
    val timeText = TimerLogic.format(timer.remainingMs)
    val status = when (timer.status) {
        TimerStatus.RUNNING -> "Light turns off in"
        TimerStatus.PAUSED -> "Paused"
        TimerStatus.IDLE -> "Ready"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideBySide = maxWidth >= 560.dp && maxWidth > maxHeight
        val ringMax = (maxHeight - 72.dp).coerceAtLeast(160.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalFadeEdges()
                .verticalScroll(rememberScrollState())
                .readableWidth(if (sideBySide) 900.dp else ReadableWidth)
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            Text("The light switches off on its own when time runs out.", style = UvType.lede, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    // In landscape the ring is sized by the height left over, so it never pushes the buttons off screen.
                    CountdownRing(fraction, timeText, status, Modifier.weight(1f).widthIn(max = ringMax))
                    Column(Modifier.weight(1f)) { TimerControls(state, vm) }
                }
            } else {
                CountdownRing(fraction, timeText, status, Modifier.fillMaxWidth())
                TimerControls(state, vm)
            }
        }
    }
}

/** Countdown ring: scales down on narrow screens instead of being cut. */
@Composable
private fun CountdownRing(fraction: Float, timeText: String, status: String, modifier: Modifier) {
    val accent = LocalAccent.current
    Box(
        modifier.padding(top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2f + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(Color(0xFF221B2D), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (fraction > 0f) {
                    drawArc(accent.color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$status $timeText"
                    // Announce state changes (start/pause), not every second.
                    liveRegion = LiveRegionMode.Polite
                },
            ) {
                Text(timeText, fontFamily = MonoFamily, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp, color = UvColors.Text)
                Text(status, fontSize = 12.sp, color = UvColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun TimerControls(state: StudioUiState, vm: StudioViewModel) {
    val timer = state.timer
    val accent = LocalAccent.current
    SectionLabel("Duration")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimerState.DURATION_CHOICES_MIN.forEach { min ->
            val selected = timer.durationMinutes == min
            val enabled = !timer.isActive
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .alpha(if (enabled) 1f else 0.4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UvColors.Surface)
                    .border(1.dp, if (selected) accent.color else UvColors.Border, RoundedCornerShape(12.dp))
                    .selectable(selected = selected, enabled = enabled, role = Role.RadioButton) { vm.selectTimerDuration(min) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$min min", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) accent.text else UvColors.Text)
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (timer.status) {
            TimerStatus.IDLE -> AccentButton("Start", vm::startTimer, Modifier.weight(1f), icon = UvIcons.Play)
            TimerStatus.RUNNING -> GhostButton("Pause", vm::pauseTimer, Modifier.weight(1f), icon = UvIcons.Pause)
            TimerStatus.PAUSED -> AccentButton("Resume", vm::resumeTimer, Modifier.weight(1f), icon = UvIcons.Play)
        }
        if (timer.isActive) GhostButton("Cancel", vm::cancelTimer, Modifier.weight(1f), icon = UvIcons.Close)
    }

    if (!state.light.isOn && !timer.isActive) {
        Callout(null) {
            Text("The light is off. Starting the timer turns it back on.", fontSize = 12.sp, color = UvColors.TextBody)
        }
    }
    Note("The countdown keeps running while you use other screens.", icon = null)
}
