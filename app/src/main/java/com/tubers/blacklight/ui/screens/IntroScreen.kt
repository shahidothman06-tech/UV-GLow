package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.R
import com.tubers.blacklight.ui.components.AccentButton
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvType

/** Welcome screen: shown on first launch and from Settings › Replay splash screen. */
@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        Modifier
            .fillMaxSize()
            .background(accent.ground)
            .drawBehind {
                val c = Offset(size.width / 2f, size.height * 0.36f)
                drawCircle(
                    Brush.radialGradient(0f to accent.color.copy(alpha = 0.25f), 1f to Color.Transparent, center = c, radius = size.height * 0.62f),
                    radius = size.height * 0.62f, center = c,
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(132.dp).drawBehind {
                    drawCircle(
                        Brush.radialGradient(0f to accent.color.copy(alpha = 0.4f), 1f to Color.Transparent, radius = size.minDimension),
                        radius = size.minDimension,
                    )
                },
            ) {
                Image(
                    painterResource(R.drawable.uv_glow_logo), contentDescription = "UV Glow app icon",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                )
            }
            Text("LIGHT EFFECTS STUDIO", style = UvType.label, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            Text(
                "UV Glow", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp, color = UvColors.Text,
                modifier = Modifier.padding(bottom = 10.dp).semantics { heading() },
            )
            Text(
                "A little light. A whole new atmosphere.", fontSize = 14.sp, color = Color(0xFFB9AFC8), textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp).padding(bottom = 36.dp),
            )
            AccentButton("Get started", onGetStarted, Modifier.fillMaxWidth())
            Text(
                "Screen effect only · no real UV output", fontSize = 11.sp, color = UvColors.TextMuted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).semantics { contentDescription = "Screen effect only. No real UV output." },
            )
        }
    }
}
