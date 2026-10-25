package com.tubers.blacklight.domain.logic

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Small ARGB color helpers, kept free of Android types so they can be unit tested. */
object ColorMath {

    fun red(argb: Int) = (argb shr 16) and 0xFF
    fun green(argb: Int) = (argb shr 8) and 0xFF
    fun blue(argb: Int) = argb and 0xFF

    fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Linear blend from [a] to [b]; t = 0 gives a, t = 1 gives b. */
    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return rgb(ch(red(a), red(b)), ch(green(a), green(b)), ch(blue(a), blue(b)))
    }

    /** WCAG relative luminance, 0..1. */
    fun luminance(argb: Int): Double {
        fun lin(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(red(argb)) + 0.7152 * lin(green(argb)) + 0.0722 * lin(blue(argb))
    }

    fun contrastRatio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Text/icon color to place on top of an accent-filled button. */
    fun onAccent(accent: Int): Int =
        if (luminance(accent) > 0.18) 0xFF0B0712.toInt() else 0xFFFFFFFF.toInt()

    /** Accent used for text on the dark background: very dark accents are lifted for contrast. */
    fun accentText(accent: Int): Int =
        if (luminance(accent) < 0.12) mix(accent, 0xFFFFFFFF.toInt(), 0.5f) else accent

    /** Background tint used for a custom theme made from [accent]. */
    fun groundFor(accent: Int): Int = mix(0xFF050308.toInt(), accent, 0.06f)

    fun withAlpha(argb: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or (argb and 0x00FFFFFF)

    fun toHex(argb: Int): String =
        "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

    /** h in 0..360, s and v in 0..1. */
    fun hsvToArgb(h: Float, s: Float, v: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1 - abs((hh / 60f) % 2 - 1))
        val m = v - c
        val (r, g, b) = when {
            hh < 60 -> Triple(c, x, 0f)
            hh < 120 -> Triple(x, c, 0f)
            hh < 180 -> Triple(0f, c, x)
            hh < 240 -> Triple(0f, x, c)
            hh < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return rgb(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** Returns [h (0..360), s (0..1), v (0..1)]. */
    fun argbToHsv(argb: Int): FloatArray {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }.let { if (it < 0) it + 360f else it }
        val s = if (mx == 0f) 0f else d / mx
        return floatArrayOf(h, s, mx)
    }
}
