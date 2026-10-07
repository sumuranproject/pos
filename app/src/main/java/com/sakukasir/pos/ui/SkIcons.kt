package com.sakukasir.pos.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Stroke icons (24x24, round caps) with the same paths as the web reference (stroke-width 1.75). */
object SkIcons {
    private fun circ(cx: Int, cy: Int, r: Int) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun build(name: String, width: Float = 1.75f, vararg d: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        d.forEach {
            b.addPath(
                pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
        return b.build()
    }

    val Dashboard by lazy { build("dashboard", 1.75f, "M3 3h7v9H3z", "M14 3h7v5h-7z", "M14 12h7v9h-7z", "M3 16h7v5H3z") }
    val Pos by lazy { build("pos", 1.75f, "M3 3h18v18H3z", "M9 3v18", "M3 9h18") }
    val Receipt by lazy { build("receipt", 1.75f, "M5 3h14a2 2 0 0 1 2 2v16l-3-2-3 2-3-2-3 2-3-2V5a2 2 0 0 1 2-2z", "M9 8h6", "M9 12h6", "M9 16h4") }
    val Clock by lazy { build("clock", 1.75f, circ(12, 12, 10), "M12 6v6l4 2") }
    val Chart by lazy { build("chart", 1.75f, "M3 3v18h18", "m7 15 4-4 3 3 5-7") }
    val Search by lazy { build("search", 1.75f, circ(11, 11, 7), "m21 21-4.3-4.3") }
    val Printer by lazy { build("printer", 1.75f, "M6 9V2h12v7", "M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2", "M6 14h12v8H6z") }
    val Bell by lazy { build("bell", 1.75f, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0") }
    val Moon by lazy { build("moon", 1.75f, "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z") }
    val Sun by lazy { build("sun", 1.75f, circ(12, 12, 4), "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41") }
    val ChevronDown by lazy { build("chevronDown", 2f, "m6 9 6 6 6-6") }
    val Close by lazy { build("close", 2f, "M18 6 6 18", "M6 6l12 12") }
    val Logout by lazy { build("logout", 2f, "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "m16 17 5-5-5-5", "M21 12H9") }
    val Edit by lazy { build("edit", 1.75f, "M12 20h9", "M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z") }
    val Trash by lazy { build("trash", 1.75f, "M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2") }
    val Plus by lazy { build("plus", 2f, "M12 5v14", "M5 12h14") }
    val Check by lazy { build("check", 3f, "M20 6 9 17l-5-5") }
    val Camera by lazy { build("camera", 1.75f, "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z", circ(12, 13, 4)) }
    val AlertCircle by lazy { build("alert", 2f, circ(12, 12, 10), "M12 8v4", "M12 16h.01") }
    val Undo by lazy { build("undo", 2f, "M3 12a9 9 0 1 0 9-9", "M3 4v5h5") }
    val Block by lazy { build("block", 2f, circ(12, 12, 10), "M4.93 4.93l14.14 14.14") }
    val CheckCircle by lazy { build("checkCircle", 1.75f, circ(12, 12, 10), "m9 12 2 2 4-4") }
    val Lock by lazy { build("lock", 1.75f, "M5 11h14v10H5z", "M8 11V7a4 4 0 0 1 8 0v4") }
    val Lines by lazy { build("list", 1.5f, "M4 6h16", "M4 12h16", "M4 18h10") }
    val Eye by lazy { build("eye", 1.75f, "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z", circ(12, 12, 3)) }
}

@Composable
fun SkIcon(icon: ImageVector, size: Dp = 18.dp, tint: Color = Sk.c.text, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
}
