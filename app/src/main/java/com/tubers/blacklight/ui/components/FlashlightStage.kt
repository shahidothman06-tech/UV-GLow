package com.tubers.blacklight.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.logic.ModeAnimation
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import kotlin.math.cos
import kotlin.math.sin

/** Colors derived from the light color; Blacklight mode deepens it toward indigo. */
data class BeamPalette(val beam: Color, val light: Color, val dark: Color) {
    companion object {
        fun from(colorArgb: Int, mode: LightMode): BeamPalette {
            val beam = if (mode == LightMode.BLACKLIGHT) ColorMath.mix(colorArgb, 0xFF5B21B6.toInt(), 0.45f) else colorArgb
            return BeamPalette(
                beam = Color(beam),
                light = Color(ColorMath.mix(beam, 0xFFFFFFFF.toInt(), 0.45f)),
                dark = Color(ColorMath.mix(beam, 0xFF000000.toInt(), 0.55f)),
            )
        }
    }
}

private val MetalStops = arrayOf(
    0f to Color(0xFF15111C), 0.16f to Color(0xFF433A50), 0.30f to Color(0xFF9A90A8), 0.42f to Color(0xFF4A4157),
    0.75f to Color(0xFF1B1623), 0.92f to Color(0xFF3A3246), 1f to Color(0xFF16121D),
)
private val MetalDarkStops = arrayOf(
    0f to Color(0xFF0E0B13), 0.3f to Color(0xFF5E5470), 0.6f to Color(0xFF1A1522), 1f to Color(0xFF0E0B13),
)

/** Stage height in design units (= dp). The torch is centered at x = 195 in a 390-wide design. */
const val STAGE_HEIGHT = 336f
private const val CX = 195f
private const val LENS_Y = 190f

/**
 * The Home screen's flashlight and beam, drawn with Canvas so every setting changes the light
 * immediately: power, color, brightness, beam width, glow radius, mode and speed.
 *
 * Drawing happens in design units (1 unit = 1 dp) with the torch centered horizontally, so the
 * torch keeps its size on every phone width; only the glow at the edges is cropped.
 */
@Composable
fun FlashlightStage(
    light: LightSettings,
    animate: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val on = light.isOn
    val b = light.brightness / 100f
    val anim = tween<Float>(450)

    val beamOp by animateFloatAsState(if (on) 0.25f + 0.75f * b else 0f, anim, label = "beamOp")
    val ambOp by animateFloatAsState(if (on) 0.1f + 0.3f * b * (0.5f + light.glowRadius / 200f) else 0f, anim, label = "ambOp")
    val haloOp by animateFloatAsState(if (on) 0.3f + 0.55f * b else 0f, anim, label = "haloOp")
    val lensGlowOp by animateFloatAsState(if (on) 0.2f + 0.5f * b else 0f, anim, label = "lensGlow")
    val width by animateFloatAsState(light.beamWidth.toFloat(), tween(300), label = "width")
    val glow by animateFloatAsState(light.glowRadius.toFloat(), tween(300), label = "glow")
    val raysOp by animateFloatAsState(if (light.mode == LightMode.LIGHT_RAYS) 1f else 0f, anim, label = "rays")
    val lensOn by animateFloatAsState(if (on) 1f else 0f, anim, label = "lensOn")

    val palette = BeamPalette.from(light.colorArgb, light.mode)
    val beamColor by animateColorAsState(palette.beam, tween(400), label = "beamColor")
    val beamLight by animateColorAsState(palette.light, tween(400), label = "beamLight")
    val beamDark by animateColorAsState(palette.dark, tween(400), label = "beamDark")
    val ground by animateColorAsState(Color(light.groundArgb), tween(400), label = "ground")
    val accent by animateColorAsState(Color(light.colorArgb), tween(400), label = "accent")

    val clock = rememberAnimationClock(animate)
    val cycle = ModeAnimation.cycleSeconds(light.mode, light.speedTenths)
    val raysCycle = 14f / light.speed
    val mode = light.mode
    val specksVisible = animate && (mode == LightMode.UV_GLOW || mode == LightMode.BLACKLIGHT || mode == LightMode.LIGHT_RAYS)

    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val t = clock.floatValue // read in draw scope: invalidates drawing only
        val modeAlpha = if (animate) ModeAnimation.alpha(mode, t / cycle) else 1f
        val rayAngle = if (animate) -14f * cos(2f * Math.PI.toFloat() * (t / raysCycle)) else 0f
        val u = density // px per design unit (dp)

        translate(left = size.width / 2f - CX * u) {
            scale(u, u, pivot = Offset.Zero) {
                // Ambient wash behind everything.
                drawEllipseGlow(Offset(CX, 150f), 300f, 190f, beamColor.copy(alpha = 0.55f * ambOp))

                // Beam group (fades with the mode's rhythm).
                val groupAlpha = beamOp * modeAlpha
                if (groupAlpha > 0.001f) {
                    drawBeam(width, glow, beamColor, beamLight, groupAlpha)
                    if (raysOp > 0.01f) drawRays(rayAngle, beamLight, groupAlpha * raysOp)
                    if (specksVisible) drawSpecks(t, beamLight, groupAlpha)
                }

                // Halo around the lens.
                val haloRx = 44f + glow * 0.7f
                val haloRy = 12f + glow * 0.22f
                drawEllipseGlow(Offset(CX, LENS_Y), haloRx, haloRy, beamLight.copy(alpha = 0.9f * haloOp), beamColor.copy(alpha = 0.45f * haloOp))

                // Fade the ambient glow into the page before drawing the torch, so the torch is never faded.
                drawRect(
                    brush = Brush.verticalGradient(0f to ground.copy(alpha = 0f), 1f to ground, startY = 270f, endY = STAGE_HEIGHT),
                    topLeft = Offset(-1000f, 270f),
                    size = Size(2400f, STAGE_HEIGHT - 270f),
                )
                drawEllipseGlow(Offset(CX, 326f), 40f, 7f, Color.Black.copy(alpha = 0.5f))

                drawTorch(on, accent, beamColor, beamLight, beamDark, lensOn, lensGlowOp)
            }
        }
    }
}

/** Soft elliptical glow: a radial gradient squashed vertically. */
private fun DrawScope.drawEllipseGlow(center: Offset, rx: Float, ry: Float, inner: Color, mid: Color? = null) {
    if (inner.alpha <= 0.001f || rx <= 0f) return
    scale(1f, ry / rx, pivot = center) {
        val stops = if (mid != null) {
            arrayOf(0f to inner, 0.45f to mid, 1f to mid.copy(alpha = 0f))
        } else {
            arrayOf(0f to inner, 1f to inner.copy(alpha = 0f))
        }
        drawCircle(Brush.radialGradient(*stops, center = center, radius = rx), radius = rx, center = center)
    }
}

private fun trapezoid(bottomHalf: Float, topHalf: Float, topY: Float): Path = Path().apply {
    moveTo(CX - bottomHalf, LENS_Y)
    lineTo(CX + bottomHalf, LENS_Y)
    lineTo(CX + topHalf, topY)
    lineTo(CX - topHalf, topY)
    close()
}

/**
 * The cone of light. Softness comes from stacking wider, fainter copies (cheaper than blur and
 * works on every API level); the glow radius controls how far they spread.
 */
private fun DrawScope.drawBeam(width: Float, glow: Float, beam: Color, beamLight: Color, alpha: Float) {
    val top = 24f + width * 1.9f
    val coreTop = top * 0.42f
    val spread = 6f + glow * 0.22f
    fun beamBrush(a: Float) = Brush.verticalGradient(
        0f to beam.copy(alpha = 0f), 0.5f to beam.copy(alpha = 0.38f * a), 1f to beam.copy(alpha = 0.95f * a),
        startY = -10f, endY = LENS_Y,
    )
    for (i in 3 downTo 1) {
        val extra = spread * i
        drawPath(trapezoid(36f + extra * 0.35f, top + extra, -10f), beamBrush(alpha * 0.16f))
    }
    drawPath(trapezoid(36f, top, -10f), beamBrush(alpha * 0.85f))
    val coreBrush = Brush.verticalGradient(
        0f to beamLight.copy(alpha = 0f), 0.65f to beamLight.copy(alpha = 0.45f * alpha), 1f to Color.White.copy(alpha = 0.85f * alpha),
        startY = -10f, endY = LENS_Y,
    )
    drawPath(trapezoid(18f + spread * 0.3f, coreTop + spread, -10f), coreBrush, alpha = 0.35f)
    drawPath(trapezoid(18f, coreTop, -10f), coreBrush)
}

/** Seven thin rays that sweep side to side in Light Rays mode. */
private fun DrawScope.drawRays(angleDeg: Float, color: Color, alpha: Float) {
    val brush = Brush.verticalGradient(
        0f to color.copy(alpha = 0f), 0.65f to color.copy(alpha = 0.45f * alpha), 1f to Color.White.copy(alpha = 0.8f * alpha),
        startY = LENS_Y - 270f, endY = LENS_Y,
    )
    rotate(angleDeg, pivot = Offset(CX, LENS_Y)) {
        listOf(-42f, -28f, -14f, 0f, 14f, 28f, 42f).forEach { a ->
            val r1 = Math.toRadians((a - 2.4f).toDouble())
            val r2 = Math.toRadians((a + 2.4f).toDouble())
            val len = 270f
            val p = Path().apply {
                moveTo(CX, LENS_Y)
                lineTo(CX + len * sin(r1).toFloat(), LENS_Y - len * cos(r1).toFloat())
                lineTo(CX + len * sin(r2).toFloat(), LENS_Y - len * cos(r2).toFloat())
                close()
            }
            drawPath(p, brush)
        }
    }
}

/** Specks of light drifting up through the beam (three staggered groups on a 5 s cycle). */
private val SpeckGroups = listOf(
    listOf(Triple(-30f, 150f, 1.6f), Triple(24f, 120f, 1.2f), Triple(-8f, 90f, 1.4f)),
    listOf(Triple(38f, 165f, 1.3f), Triple(-44f, 125f, 1.1f), Triple(10f, 140f, 1.8f)),
    listOf(Triple(-18f, 170f, 1.5f), Triple(30f, 100f, 1.2f), Triple(-36f, 95f, 1.3f)),
)

private fun DrawScope.drawSpecks(t: Float, color: Color, alpha: Float) {
    SpeckGroups.forEachIndexed { gi, group ->
        val p = (((t - gi * 1.7f) / 5f) % 1f + 1f) % 1f
        val a = when {
            p < 0.2f -> p / 0.2f * 0.9f
            else -> 0.9f * (1f - (p - 0.2f) / 0.8f)
        } * alpha
        val dy = -120f * p * p // ease-in rise
        group.forEach { (dx, y, r) ->
            drawCircle(color.copy(alpha = a.coerceIn(0f, 1f)), radius = r, center = Offset(CX + dx, y + dy))
        }
    }
}

/** The metal torch: head, collar, grip body, tail cap, switch and glowing lens. */
private fun DrawScope.drawTorch(
    on: Boolean,
    accent: Color,
    beam: Color,
    beamLight: Color,
    beamDark: Color,
    lensOn: Float,
    lensGlowOp: Float,
) {
    // Head (tapered).
    val head = Path().apply {
        moveTo(155f, 190f); lineTo(235f, 190f); lineTo(224f, 236f); lineTo(166f, 236f); close()
    }
    drawPath(head, Brush.horizontalGradient(*MetalStops, startX = 155f, endX = 235f))
    val headShade = Path().apply {
        moveTo(155f, 190f); lineTo(235f, 190f); lineTo(233f, 200f); lineTo(157f, 200f); close()
    }
    drawPath(headShade, Color.Black.copy(alpha = 0.28f))
    drawRect(Color.Black.copy(alpha = 0.35f), Offset(166f, 232f), Size(58f, 4f))

    // Collar.
    drawRect(Brush.horizontalGradient(*MetalDarkStops, startX = 168f, endX = 222f), Offset(168f, 236f), Size(54f, 11f))
    drawRect(Color(0xFFB4AAC2).copy(alpha = 0.45f), Offset(168f, 238f), Size(54f, 1.5f))

    // Body with grip rings.
    drawRoundRect(
        Brush.horizontalGradient(*MetalStops, startX = 172f, endX = 218f),
        Offset(172f, 247f), Size(46f, 70f), CornerRadius(5f, 5f),
    )
    listOf(272f, 279f, 286f, 293f).forEach { y -> drawRect(Color(0xFF0B0810).copy(alpha = 0.6f), Offset(172f, y), Size(46f, 2f)) }
    drawRect(Color.White.copy(alpha = 0.1f), Offset(183f, 247f), Size(3f, 70f))

    // Tail cap closes the handle.
    drawRoundRect(
        Brush.horizontalGradient(*MetalDarkStops, startX = 170f, endX = 220f),
        Offset(170f, 313f), Size(50f, 10f), CornerRadius(4f, 4f),
    )
    drawRect(Color(0xFFB4AAC2).copy(alpha = 0.35f), Offset(172f, 314.5f), Size(46f, 1.2f))

    // Power switch with indicator dot.
    val off = Color(0xFF3A3048)
    drawRoundRect(Color(0xFF0E0B14), Offset(185f, 253f), Size(20f, 11f), CornerRadius(5.5f, 5.5f))
    drawRoundRect(if (on) accent else off, Offset(185f, 253f), Size(20f, 11f), CornerRadius(5.5f, 5.5f), style = Stroke(1.5f))
    drawCircle(if (on) beamLight else off, radius = 2.2f, center = Offset(195f, 258.5f))

    // Lens rim, lens and reflection.
    val rim = Rect(Offset(155f, 180f), Size(80f, 20f))
    drawOval(Color(0xFF0D0A12), rim.topLeft, rim.size)
    drawOval(Color(0xFFABA1B9), rim.topLeft, rim.size, style = Stroke(2f))
    val lensTopLeft = Offset(195f - 35.5f, 190f - 7.6f)
    val lensSize = Size(71f, 15.2f)
    drawOval(Color(0xFF1B1624), lensTopLeft, lensSize)
    if (lensOn > 0.01f) {
        scale(1f, 7.6f / 35.5f, pivot = Offset(CX, LENS_Y)) {
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White, 0.3f to beamLight, 0.7f to beam, 1f to beamDark,
                    center = Offset(CX - 3.5f, LENS_Y - 1.5f), radius = 46f,
                ),
                radius = 35.5f, center = Offset(CX, LENS_Y), alpha = lensOn,
            )
        }
    }
    drawOval(Color.White.copy(alpha = 0.35f), Offset(173f, 186f), Size(20f, 4f))

    // Bloom over the lens.
    drawEllipseGlow(Offset(CX, LENS_Y), 52f, 18f, beamLight.copy(alpha = lensGlowOp))
}

/**
 * Side-on preview used on the Effects screen (design: 350 × 120). Anchored left so the torch is
 * never cropped; the beam simply fades out on the right.
 */
@Composable
fun BeamPreview(
    light: LightSettings,
    animate: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val on = light.isOn
    val beamOp by animateFloatAsState(if (on) 0.25f + 0.75f * light.brightness / 100f else 0f, tween(450), label = "pBeam")
    val width by animateFloatAsState(light.beamWidth.toFloat(), tween(300), label = "pWidth")
    val palette = BeamPalette.from(light.colorArgb, light.mode)
    val beam by animateColorAsState(palette.beam, tween(400), label = "pBeamColor")
    val beamLight by animateColorAsState(palette.light, tween(400), label = "pBeamLight")
    val clock = rememberAnimationClock(animate)
    val cycle = ModeAnimation.cycleSeconds(light.mode, light.speedTenths)
    val mode = light.mode

    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val t = clock.floatValue
        val modeAlpha = if (animate) ModeAnimation.alpha(mode, t / cycle) else 1f
        val u = size.height / 120f
        scale(u, u, pivot = Offset.Zero) {
            val a = beamOp * modeAlpha
            if (a > 0.001f) {
                val half = 10f + width * 0.5f
                val far = size.width / u + 10f
                val brush = Brush.horizontalGradient(
                    0f to beam.copy(alpha = 0.95f * a), 0.55f to beam.copy(alpha = 0.3f * a), 1f to beam.copy(alpha = 0f),
                    startX = 58f, endX = far,
                )
                for (i in 2 downTo 0) {
                    val grow = i * 6f
                    val p = Path().apply {
                        moveTo(58f, 36f - grow * 0.4f); lineTo(58f, 84f + grow * 0.4f)
                        lineTo(far, 60f + half + grow); lineTo(far, 60f - half - grow); close()
                    }
                    drawPath(p, brush, alpha = if (i == 0) 1f else 0.25f)
                }
                drawEllipseGlow(Offset(62f, 60f), 14f, 32f, beamLight.copy(alpha = 0.6f * a))
            }
            val metal = Brush.verticalGradient(
                0f to Color(0xFF15111C), 0.3f to Color(0xFF9A90A8), 0.55f to Color(0xFF2A2335), 1f to Color(0xFF15111C),
                startY = 44f, endY = 76f,
            )
            drawRoundRect(metal, Offset(9f, 46f), Size(7f, 28f), CornerRadius(2.5f, 2.5f))
            drawRoundRect(metal, Offset(14f, 48f), Size(28f, 24f), CornerRadius(3f, 3f))
            val head = Path().apply { moveTo(40f, 44f); lineTo(58f, 34f); lineTo(58f, 86f); lineTo(40f, 76f); close() }
            drawPath(head, Brush.verticalGradient(0f to Color(0xFF15111C), 0.3f to Color(0xFF9A90A8), 0.55f to Color(0xFF2A2335), 1f to Color(0xFF15111C), startY = 34f, endY = 86f))
            drawOval(if (on) beamLight else Color(0xFF1B1624), Offset(54f, 34f), Size(8f, 52f))
            drawOval(Color(0xFFABA1B9), Offset(54f, 34f), Size(8f, 52f), style = Stroke(1.5f))
        }
    }
}
