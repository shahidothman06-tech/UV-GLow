package com.tubers.blacklight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.R
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvRadius
import com.tubers.blacklight.ui.theme.UvType

private val HeaderHeight = 64.dp

/** Brand header for the five tab screens, with the Premium crown. */
@Composable
fun TabHeader(onPremium: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(HeaderHeight)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.uv_glow_logo), contentDescription = null,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text("UV Glow", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text, modifier = Modifier.weight(1f))
        HeaderIconButton(UvIcons.Crown, "UV Glow Premium", onPremium, tint = accent.text)
    }
}

/** Header with a back button, for Timer and Premium. */
@Composable
fun SubHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(HeaderHeight)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderIconButton(UvIcons.Back, "Back", onBack)
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = UvColors.Text) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(UvRadius.m))
            .background(Color(0xFF120E19))
            .border(1.dp, UvColors.Border, RoundedCornerShape(UvRadius.m))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Bottom navigation with the design's small accent bar above the selected tab. */
@Composable
fun BottomNav(items: List<NavItem>, selectedRoute: String?, onSelect: (NavItem) -> Unit) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth().background(UvColors.NavBackground).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = UvColors.Divider, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp)) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val color by animateColorAsState(if (selected) accent.text else UvColors.TextMuted, label = "nav")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item) }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .size(width = 20.dp, height = 3.dp)
                                .background(accent.color, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                        Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Large round power button with accent glow when on. */
@Composable
fun PowerButton(isOn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val glow by animateFloatAsState(if (isOn) 1f else 0f, label = "powerGlow")
    val border by animateColorAsState(if (isOn) accent.color else UvColors.Track, label = "powerBorder")
    val iconColor by animateColorAsState(if (isOn) Color.White else UvColors.TextMuted, label = "powerIcon")
    Box(
        modifier
            .size(76.dp)
            .drawBehind {
                if (glow > 0f) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        Brush.radialGradient(
                            0f to accent.color.copy(alpha = 0.45f * glow), 1f to Color.Transparent,
                            center = center, radius = r * 1.9f,
                        ),
                        radius = r * 1.9f,
                    )
                    drawCircle(accent.color.copy(alpha = 0.13f * glow), radius = r + 6.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
                }
            }
            .clip(CircleShape)
            .background(Color(0xFF16111F))
            .border(1.dp, border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (isOn) "Turn light off" else "Turn light on"
                stateDescription = if (isOn) "On" else "Off"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(UvIcons.Power, contentDescription = null, tint = iconColor, modifier = Modifier.size(30.dp))
    }
}

/** Quick-access chip on Home ("Effect · UV Glow"). Text wraps to two lines instead of cutting. */
@Composable
fun QuickChip(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, dot: Color? = null, mono: Boolean = false) {
    Column(
        modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(UvRadius.m))
            .background(UvColors.Surface)
            .border(1.dp, UvColors.Border, RoundedCornerShape(UvRadius.m))
            .clickable(role = Role.Button, onClickLabel = "Open $label", onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 10.sp, color = UvColors.TextSecondary, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                ColorDot(dot)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                value,
                style = if (mono) UvType.mono.copy(color = UvColors.Text) else UvType.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp),
                color = UvColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
