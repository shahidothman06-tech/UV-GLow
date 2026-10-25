package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.model.Swatches
import com.tubers.blacklight.domain.model.ThemeItem
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.components.ColorPickerDialog
import com.tubers.blacklight.ui.components.ConfirmDialog
import com.tubers.blacklight.ui.components.IconMiniButton
import com.tubers.blacklight.ui.components.MiniButton
import com.tubers.blacklight.ui.components.NameDialog
import com.tubers.blacklight.ui.components.PageTitle
import com.tubers.blacklight.ui.components.SectionLabel
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.UvTextField
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemesScreen(state: StudioUiState, vm: StudioViewModel) {
    val light = state.light
    var newName by rememberSaveable { mutableStateOf("") }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val isSwatch = Swatches.entries.any { it.argb == light.colorArgb }

    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        PageTitle("Themes", "Pick a light color, or save your own theme.")

        SectionLabel("Light color · ${light.colorName}", top = 8.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatches.entries.forEach { sw ->
                SwatchButton(
                    color = Color(sw.argb),
                    label = sw.displayName,
                    selected = sw.argb == light.colorArgb,
                    onClick = { vm.selectSwatch(sw) },
                )
            }
            CustomColorButton(
                current = if (isSwatch) null else Color(light.colorArgb),
                onClick = { showPicker = true },
            )
        }

        SectionLabel("Your themes")
        state.themes.forEach { theme ->
            ThemeRow(
                theme = theme,
                applied = light.themeId == theme.id,
                canUpdate = theme.isCustom && light.themeId == theme.id && theme.accentArgb != light.colorArgb,
                onApply = { vm.applyTheme(theme) },
                onUpdate = { vm.updateThemeToCurrentColor(theme) },
                onRename = { renaming = theme.id },
                onDelete = { deleting = theme.id },
            )
        }

        SectionLabel("Create a theme")
        UvPanel(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Text("Saves the current light color as a new theme.", style = UvType.lede.copy(fontSize = 12.sp), modifier = Modifier.padding(bottom = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UvTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Theme name",
                    label = "New theme name",
                    onDone = { vm.createTheme(newName); newName = "" },
                    modifier = Modifier.weight(1f),
                )
                AccentButton("Save", onClick = { vm.createTheme(newName); newName = "" }, height = 52.dp)
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initialArgb = light.colorArgb,
            onConfirm = { vm.selectCustomColor(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
    val renameTheme = state.themes.firstOrNull { it.id == renaming }
    if (renameTheme != null) {
        NameDialog(
            title = "Rename theme",
            initial = renameTheme.name,
            onConfirm = { vm.renameTheme(renameTheme, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    val deleteTheme = state.themes.firstOrNull { it.id == deleting }
    if (deleteTheme != null) {
        ConfirmDialog(
            title = "Delete theme?",
            message = "“${deleteTheme.name}” will be removed. Your current light color stays as it is.",
            confirmText = "Delete",
            onConfirm = { vm.deleteTheme(deleteTheme); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SwatchButton(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    // 48 dp touch target; the ring shows the selected color.
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .drawBehind {
                if (selected) drawCircle(UvColors.Text, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            .padding(5.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun CustomColorButton(current: Color?, onClick: () -> Unit) {
    val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Pick a custom color", onClick = onClick)
            .semantics { contentDescription = "Custom color" }
            .drawBehind {
                if (current != null) drawCircle(UvColors.Text, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            .padding(5.dp)
            .clip(CircleShape)
            .background(current ?: UvColors.Surface)
            .drawBehind {
                if (current == null) drawCircle(Color(0xFF5A4D6C), radius = size.minDimension / 2f - 1f, style = Stroke(1.dp.toPx(), pathEffect = dashed))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(UvIcons.Plus, null, tint = if (current == null) UvColors.Text else Color(ColorMath.onAccent(current.toArgb())), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ThemeRow(
    theme: ThemeItem,
    applied: Boolean,
    canUpdate: Boolean,
    onApply: () -> Unit,
    onUpdate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = Color(theme.accentArgb)
    val accentText = Color(ColorMath.accentText(theme.accentArgb))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(UvRadius.l))
            .background(UvColors.Surface)
            .border(1.dp, if (applied) accent else UvColors.BorderSoft, RoundedCornerShape(UvRadius.l))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Preview tile: glow rising from the bottom over the theme background.
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(theme.groundArgb))
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                0f to accent, 0.35f to accent.copy(alpha = 0.33f), 0.72f to Color.Transparent,
                                center = Offset(size.width / 2, size.height * 1.2f), radius = size.height,
                            ),
                            radius = size.height, center = Offset(size.width / 2, size.height * 1.2f),
                        )
                    }
                    .border(1.dp, UvColors.Border, RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(theme.name, style = UvType.bodyStrong, color = UvColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (theme.isCustom) "Custom · ${ColorMath.toHex(theme.accentArgb)}" else "Built-in", style = UvType.small)
                if (canUpdate) {
                    TextButton(onClick = onUpdate, contentPadding = PaddingValues(0.dp)) {
                        Text("Update to current color", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentText, textDecoration = TextDecoration.Underline)
                    }
                }
            }
            if (applied) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(UvIcons.Check, null, tint = accentText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("On", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentText)
                }
            } else {
                MiniButton("Apply", onApply)
            }
        }
        // Custom themes get their edit actions on a second line, so the name is never squeezed.
        if (theme.isCustom) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                IconMiniButton(UvIcons.Pencil, "Rename ${theme.name}", onRename)
                IconMiniButton(UvIcons.Trash, "Delete ${theme.name}", onDelete)
            }
        }
    }
}
