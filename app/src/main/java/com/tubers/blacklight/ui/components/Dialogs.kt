package com.tubers.blacklight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvType
import kotlin.math.roundToInt

/** Rename dialog for presets and themes. */
@Composable
fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val accent = LocalAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UvColors.Surface,
        title = { Text(title, color = UvColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            UvTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Name",
                label = title,
                onDone = { onConfirm(name) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save", color = accent.text, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = UvColors.TextSoft) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val accent = LocalAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UvColors.Surface,
        title = { Text(title, color = UvColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, style = UvType.lede) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (destructive) UvColors.Danger else accent.text, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = UvColors.TextSoft) } },
    )
}

/**
 * Shown once, before Strobe or SOS first runs. Nothing flashes until "Continue"; dismissing it
 * (Cancel, back, tapping outside) keeps the light steady.
 */
@Composable
fun FlashWarningDialog(modeName: String, onContinue: () -> Unit, onCancel: () -> Unit) {
    val accent = LocalAccent.current
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = UvColors.Surface,
        title = { Text("Flashing lights", color = UvColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$modeName makes the screen flash on and off. Flashing lights can trigger seizures in people " +
                        "with photosensitive epilepsy, and can cause discomfort, headaches or dizziness.",
                    style = UvType.lede,
                )
                Text(
                    "Don't use it if you or anyone nearby has epilepsy or is sensitive to flashing light. Stop " +
                        "straight away if you feel unwell. Strobe never exceeds 3 flashes per second.",
                    style = UvType.lede,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continue", color = accent.text, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = UvColors.TextSoft) } },
    )
}

/**
 * Custom color picker: hue, saturation and brightness sliders with a live preview.
 * Brightness is kept above 35% so the beam stays visible on a dark screen.
 */
@Composable
fun ColorPickerDialog(
    initialArgb: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = remember(initialArgb) { ColorMath.argbToHsv(initialArgb) }
    // Saveable so a half-picked color survives rotation and window resizes.
    var hue by rememberSaveable { mutableFloatStateOf(start[0]) }
    var sat by rememberSaveable { mutableFloatStateOf(start[1].coerceIn(0.2f, 1f)) }
    var value by rememberSaveable { mutableFloatStateOf(start[2].coerceIn(0.35f, 1f)) }
    val argb = ColorMath.hsvToArgb(hue, sat, value)
    val accent = LocalAccent.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UvColors.Surface,
        title = { Text("Custom color", color = UvColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(Color(argb), CircleShape)
                            .border(2.dp, UvColors.Text, CircleShape)
                    )
                    Text(ColorMath.toHex(argb), style = UvType.mono.copy(fontSize = 14.sp, color = UvColors.Text))
                }
                // Hue preview strip
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                (0..6).map { Color(ColorMath.hsvToArgb(it * 60f, 1f, 1f)) }
                            ),
                            RoundedCornerShape(4.dp),
                        )
                )
                LabeledSlider("Hue", "${hue.roundToInt()}°", hue.roundToInt(), 0..359, { hue = it.toFloat() })
                LabeledSlider("Saturation", "${(sat * 100).roundToInt()}%", (sat * 100).roundToInt(), 20..100, { sat = it / 100f })
                LabeledSlider("Brightness", "${(value * 100).roundToInt()}%", (value * 100).roundToInt(), 35..100, { value = it / 100f })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(argb) }) { Text("Use color", color = accent.text, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = UvColors.TextSoft) } },
    )
}
