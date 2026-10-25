package com.tubers.blacklight.domain.logic

import com.tubers.blacklight.domain.model.LightMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/**
 * How bright the beam is at a point in its animation cycle. Shared by the Home stage and the
 * Effects preview so both always agree. Phase runs 0..1 over one cycle.
 */
object ModeAnimation {

    /** Shortest strobe cycle: one flash per 0.34 s keeps it under 3 flashes per second. */
    const val MIN_STROBE_PERIOD_SECONDS = 0.34f

    /** Seconds per cycle for [mode] at [speedTenths] (10 = 1.0×). */
    fun cycleSeconds(mode: LightMode, speedTenths: Int): Float {
        val speed = (speedTenths.coerceAtLeast(1)) / 10f
        val raw = mode.baseSeconds / speed
        return if (mode == LightMode.STROBE) max(MIN_STROBE_PERIOD_SECONDS, raw) else raw
    }

    fun flashesPerSecond(mode: LightMode, speedTenths: Int): Float =
        if (mode == LightMode.STROBE) 1f / cycleSeconds(mode, speedTenths) else 0f

    /** Beam opacity multiplier, 0..1. */
    fun alpha(mode: LightMode, phase: Float): Float {
        val p = ((phase % 1f) + 1f) % 1f
        return when (mode) {
            LightMode.UV_GLOW, LightMode.LIGHT_RAYS -> wave(p, low = 0.82f)
            LightMode.PULSE -> wave(p, low = 0.28f)
            LightMode.STROBE -> if (p < 0.5f) 1f else 0.1f
            LightMode.SOS -> sos(p)
            LightMode.BLACKLIGHT -> shimmer(p)
        }
    }

    /** Smooth 1 → low → 1 cosine wave. */
    private fun wave(p: Float, low: Float): Float {
        val k = (1 - cos(2 * PI * p).toFloat()) / 2f // 0 at ends, 1 at middle
        return 1f - (1f - low) * k
    }

    private fun shimmer(p: Float): Float = when {
        p < 0.35f -> lerp(0.92f, 1f, p / 0.35f)
        p < 0.70f -> lerp(1f, 0.78f, (p - 0.35f) / 0.35f)
        else -> lerp(0.78f, 0.92f, (p - 0.70f) / 0.30f)
    }

    /**
     * ··· ––– ··· timed on a 7 s cycle (at 1×): dots 3% on, dashes ~9% on, gaps 3%,
     * letter gaps ~9%, then a long pause before repeating.
     */
    private val SOS_ON: List<ClosedFloatingPointRange<Float>> = listOf(
        0.000f..0.029f, 0.059f..0.088f, 0.118f..0.147f,          // S
        0.235f..0.323f, 0.353f..0.441f, 0.471f..0.558f,          // O
        0.647f..0.676f, 0.706f..0.735f, 0.765f..0.794f,          // S
    )

    private fun sos(p: Float): Float = if (SOS_ON.any { p in it }) 1f else 0.12f

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
