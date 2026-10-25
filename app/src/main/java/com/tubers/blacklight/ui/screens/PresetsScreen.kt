package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.logic.StudioLogic
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.components.Callout
import com.tubers.blacklight.ui.components.ConfirmDialog
import com.tubers.blacklight.ui.components.IconMiniButton
import com.tubers.blacklight.ui.components.NameDialog
import com.tubers.blacklight.ui.components.PageTitle
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.Tag
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.UvTextField
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType

@Composable
fun PresetsScreen(state: StudioUiState, vm: StudioViewModel) {
    val light = state.light
    var newName by rememberSaveable { mutableStateOf("") }
    var renaming by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        PageTitle("Presets", "Save the current look and bring it back in one tap.")

        UvPanel(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Save current settings", fontSize = 13.sp, color = Color(0xFFE4DDEE), modifier = Modifier.weight(1f))
                Text("${light.mode.displayName} · ${light.brightness}%", style = UvType.mono.copy(color = UvColors.TextSecondary))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UvTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Preset name",
                    label = "New preset name",
                    onDone = { vm.savePreset(newName); newName = "" },
                    modifier = Modifier.weight(1f),
                )
                AccentButton("Save", onClick = { vm.savePreset(newName); newName = "" }, icon = UvIcons.Plus, height = 52.dp)
            }
        }

        SectionLabel("Saved · ${state.presets.size}")
        if (state.presets.isEmpty()) {
            Callout(null) {
                Text("No presets yet. Set up a look on Home, then save it here.", fontSize = 12.sp, color = UvColors.TextBody)
            }
        }
        state.presets.forEach { preset ->
            PresetCard(
                preset = preset,
                active = StudioLogic.isPresetActive(light, preset),
                onApply = { vm.applyPreset(preset) },
                onRename = { renaming = preset.id },
                onDelete = { deleting = preset.id },
            )
        }
    }

    val renamePreset = state.presets.firstOrNull { it.id == renaming }
    if (renamePreset != null) {
        NameDialog(
            title = "Rename preset",
            initial = renamePreset.name,
            onConfirm = { vm.renamePreset(renamePreset, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    val deletePreset = state.presets.firstOrNull { it.id == deleting }
    if (deletePreset != null) {
        ConfirmDialog(
            title = "Delete preset?",
            message = "“${deletePreset.name}” will be removed from this device.",
            confirmText = "Delete",
            onConfirm = { vm.deletePreset(deletePreset); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PresetCard(preset: Preset, active: Boolean, onApply: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val color = Color(preset.colorArgb)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(UvRadius.xl))
            .background(UvColors.Surface)
            .border(1.dp, UvColors.BorderSoft, RoundedCornerShape(UvRadius.xl)),
    ) {
        // Preview: the preset's light rising from below over its background.
        Box(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(Color(preset.groundArgb))
                .drawBehind {
                    val c = Offset(size.width / 2, size.height * 1.25f)
                    val r = 170.dp.toPx()
                    drawCircle(
                        Brush.radialGradient(0f to color, 0.4f to color.copy(alpha = 0.25f), 0.78f to Color.Transparent, center = c, radius = r),
                        radius = r, center = c,
                    )
                },
        ) {
            Tag(preset.mode.displayName, Modifier.padding(start = 12.dp, top = 10.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            val narrow = maxWidth < 300.dp
            val info: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Text(preset.name, style = UvType.bodyStrong, color = UvColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(StudioLogic.presetMeta(preset), style = UvType.mono.copy(fontSize = 11.sp, color = UvColors.TextSecondary), modifier = Modifier.padding(top = 2.dp))
                }
            }
            val actions: @Composable (Modifier) -> Unit = { m ->
                Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccentButton(
                        text = if (active) "Active" else "Apply",
                        onClick = onApply,
                        height = 40.dp,
                        container = if (active) UvColors.BorderSoft else color,
                        content = if (active) UvColors.TextSoft else Color(ColorMath.onAccent(preset.colorArgb)),
                        modifier = if (narrow) Modifier.weight(1f) else Modifier,
                    )
                    IconMiniButton(UvIcons.Pencil, "Rename ${preset.name}", onRename)
                    IconMiniButton(UvIcons.Trash, "Delete ${preset.name}", onDelete)
                }
            }
            if (narrow) {
                // Narrow phones: actions move under the name so nothing is cut.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    info(Modifier.fillMaxWidth())
                    actions(Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    info(Modifier.weight(1f))
                    actions(Modifier)
                }
            }
        }
    }
}
