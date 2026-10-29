package com.tubers.blacklight.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The design's 24×24 line icons, built from the same SVG path data. Tint them with Icon(tint=…).
 */
object UvIcons {

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

    private fun stroke(name: String, vararg paths: String, width: Float = 1.8f): ImageVector {
        val b = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
        )
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun filled(name: String, d: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            .build()

    val Crown = stroke("crown", "M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z")
    val Back = stroke("back", "M15 5l-7 7 7 7")
    val Power = stroke("power", "M12 3v8M6.3 6.8a8 8 0 1 0 11.4 0", width = 2.2f)
    val Info = stroke("info", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.01")
    val Plus = stroke("plus", "M12 5v14M5 12h14")
    val Check = stroke("check", "M5 12.5l4.5 4.5L19 7")
    val Pencil = stroke("pencil", "M4 20h4L19 9l-4-4L4 16z")
    val Trash = stroke("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13")
    val Play = filled("play", "M8 5v14l11-7z")
    val Pause = stroke("pause", "M8 5v14M16 5v14")
    val Close = stroke("close", "M6 6l12 12M18 6L6 18")
    val Wave = stroke("wave", "M3 12c3-6 6 6 9 0s6 6 9 0")
    val Bolt = stroke("bolt", "M13 3L5 13h6l-1 8 8-10h-6z")
    val Refresh = stroke("refresh", "M4 12a8 8 0 0 1 14-5.3L20 9M20 4v5h-5M20 12a8 8 0 0 1-14 5.3L4 15M4 20v-5h5")
    val ChevronRight = stroke("chevron", "M9 5l7 7-7 7")
    val ArrowRight = stroke("arrow_right", "M5 12h14M13 6l6 6-6 6", width = 2.2f)
    val Globe = stroke("globe", circle(12f, 12f, 9f), "M3 12h18M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9S9.5 5.6 12 3z")
    val Search = stroke("search", circle(11f, 11f, 7f), "M20 20l-4-4")
    val Palette = stroke(
        "palette",
        "M12 3a9 9 0 1 0 0 18c1.1 0 1.6-.8 1.6-1.6 0-.9-.6-1.3-.6-2.2 0-1 .8-1.7 1.8-1.7H17a4 4 0 0 0 4-4C21 6.6 17 3 12 3z",
        circle(7.5f, 11f, 1.2f), circle(10.5f, 7f, 1.2f), circle(15f, 7.5f, 1.2f),
    )
    val Shield = stroke("shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z")
    val Timer = stroke("timer", circle(12f, 13f, 8f), "M12 9v4l2.5 2.5M9 2h6")

    // Modes
    val ModeUv = stroke(
        "mode_uv", circle(12f, 12f, 3.5f),
        "M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M18.4 5.6l-1.8 1.8M7.4 16.6l-1.8 1.8",
    )
    val ModePulse = stroke("mode_pulse", circle(12f, 12f, 2.5f), circle(12f, 12f, 6f), circle(12f, 12f, 9.5f))
    val ModeRays = stroke("mode_rays", "M12 21L4 5M12 21l3-17M12 21l8-14M12 21L9 4")
    val ModeStrobe = Bolt
    val ModeSos = stroke("mode_sos", "M3 9h.01M6 9h.01M9 9h.01M3 15h4M10 15h4M17 15h4M15 9h.01M18 9h.01M21 9h.01")
    val ModeBlack = stroke("mode_black", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")

    // Navigation
    val NavHome = stroke("nav_home", "M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val NavEffects = stroke(
        "nav_effects",
        "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z",
    )
    val NavThemes = stroke("nav_themes", "M12 3c3.5 4.2 6 7.4 6 10.5a6 6 0 0 1-12 0C6 10.4 8.5 7.2 12 3z")
    val NavPresets = stroke("nav_presets", "M6 3h12v18l-6-4-6 4z")
    val NavSettings = stroke(
        "nav_settings", "M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1",
        circle(15f, 6f, 2f), circle(9f, 12f, 2f), circle(17f, 18f, 2f),
    )
}
