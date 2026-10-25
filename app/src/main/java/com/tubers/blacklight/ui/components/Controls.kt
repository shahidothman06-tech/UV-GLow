package com.tubers.blacklight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType
import kotlin.math.roundToInt

/** Soft fade at the top and bottom of a scroll area instead of a hard cut under the bars. */
fun Modifier.verticalFadeEdges(top: Dp = 14.dp, bottom: Dp = 18.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        if (h <= 0f) return@drawWithContent
        val t = (top.toPx() / h).coerceIn(0f, 0.5f)
        val b = (bottom.toPx() / h).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent, t to Color.Black, 1f - b to Color.Black, 1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun PageTitle(title: String, lede: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = UvType.pageTitle, color = UvColors.Text, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        if (lede != null) Text(lede, style = UvType.lede, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, top: Dp = 22.dp) {
    Text(
        text.uppercase(),
        style = UvType.label,
        modifier = modifier.padding(top = top, bottom = 10.dp).semantics { contentDescription = text },
    )
}

/** Rounded dark panel used for grouped controls. */
@Composable
fun UvPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    borderColor: Color = UvColors.BorderSoft,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UvRadius.xl))
            .background(UvColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(UvRadius.xl))
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = UvType.mono.copy(fontSize = 11.sp, color = UvColors.TextSoft),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0B0810))
            .border(1.dp, UvColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier, icon: ImageVector? = UvIcons.Info) {
    Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = UvColors.TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = UvType.small.copy(color = UvColors.TextMuted))
    }
}

@Composable
fun Callout(icon: ImageVector?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(UvRadius.m))
            .background(UvColors.SurfaceRaised)
            .border(1.dp, UvColors.Border, RoundedCornerShape(UvRadius.m))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = UvColors.TextBody, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), content = content)
    }
}

/** Filled accent button (48 dp tall, 14 dp corners). */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    container: Color = LocalAccent.current.color,
    content: Color = LocalAccent.current.onColor,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(UvRadius.m),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Dark outlined button. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(UvRadius.m),
        border = BorderStroke(1.dp, UvColors.Border),
        colors = ButtonDefaults.buttonColors(containerColor = UvColors.SurfaceRaised, contentColor = UvColors.Text),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Small secondary button ("Apply", "Cancel"). */
@Composable
fun MiniButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(RoundedCornerShape(UvRadius.s))
            .background(UvColors.SurfaceRaised)
            .border(1.dp, UvColors.Border, RoundedCornerShape(UvRadius.s))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = UvColors.Text)
    }
}

/** 44 dp square icon button with a visible outline (minimum touch target). */
@Composable
fun IconMiniButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = UvColors.Text) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(UvRadius.s))
            .background(UvColors.SurfaceRaised)
            .border(1.dp, UvColors.Border, RoundedCornerShape(UvRadius.s))
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Labelled slider row: "Brightness ........ 75%". Integer values only. */
@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    val accent = LocalAccent.current
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, color = Color(0xFFE4DDEE), modifier = Modifier.weight(1f))
                Text(valueText, style = UvType.mono.copy(color = UvColors.TextSecondary))
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = UvColors.Text,
                    activeTrackColor = accent.color,
                    inactiveTrackColor = UvColors.Border,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .semantics {
                        contentDescription = label
                        stateDescription = valueText
                    },
            )
        }
        if (showDivider) HorizontalDivider(color = UvColors.Divider, thickness = 1.dp)
    }
}

@Composable
fun UvSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = UvColors.Text,
            checkedTrackColor = accent.color,
            checkedBorderColor = accent.color,
            uncheckedThumbColor = UvColors.Text,
            uncheckedTrackColor = UvColors.Track,
            uncheckedBorderColor = UvColors.Track,
        ),
    )
}

/** Rounded square icon holder used at the start of settings rows. */
@Composable
fun RowIcon(icon: ImageVector) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(UvRadius.s)).background(UvColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = UvColors.TextSoft, modifier = Modifier.size(18.dp)) }
}

/** Title + subtitle block that can wrap to two lines without being cut. */
@Composable
fun RowScope.RowText(title: String, subtitle: String?, titleMaxLines: Int = 2) {
    Column(Modifier.weight(1f)) {
        Text(title, style = UvType.bodyStrong, color = UvColors.Text, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, style = UvType.small)
    }
}

/** A setting with a switch. The whole row toggles, and it is announced as a switch. */
@Composable
fun SwitchRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) RowIcon(icon)
        RowText(title, subtitle)
        // The row handles the toggle, so the switch itself is decorative for touch and accessibility.
        UvSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A tappable settings row ending in a chevron. */
@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit, trailingRotation: Float = 0f) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowIcon(icon)
        RowText(title, subtitle)
        Icon(
            UvIcons.ChevronRight, null, tint = UvColors.TextSoft,
            modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = trailingRotation },
        )
    }
}

/** Text field matching the dark design. */
@Composable
fun UvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String = placeholder,
    onDone: () -> Unit = {},
) {
    val accent = LocalAccent.current
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(32)) },
        placeholder = { Text(placeholder, color = UvColors.Placeholder, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = UvType.body.copy(color = UvColors.Text, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.color,
            unfocusedBorderColor = UvColors.Border,
            focusedContainerColor = UvColors.Field,
            unfocusedContainerColor = UvColors.Field,
            cursorColor = accent.color,
            focusedTextColor = UvColors.Text,
            unfocusedTextColor = UvColors.Text,
        ),
        modifier = modifier.semantics { contentDescription = label },
    )
}

/** Small color dot. */
@Composable
fun ColorDot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
