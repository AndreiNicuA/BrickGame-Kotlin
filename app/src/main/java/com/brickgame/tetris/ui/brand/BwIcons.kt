package com.brickgame.tetris.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Line icons of the design system, drawn on a 24×24 grid (no icon library dependency). */
enum class BwIconKind { CHEVRON_RIGHT, CHEVRON_LEFT, CHEVRON_DOWN, PLAY, SETTINGS, PLUS, CHECK, CHART, SWORDS, HELP, CUBE }

@Composable
fun BwIcon(kind: BwIconKind, color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val k = this.size.width / 24f
        fun p(x: Float, y: Float) = Offset(x * k, y * k)
        val stroke = Stroke(width = 2.2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(vararg pts: Offset) {
            val path = Path().apply { moveTo(pts[0].x, pts[0].y); for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y) }
            drawPath(path, color, style = stroke)
        }
        when (kind) {
            BwIconKind.CHEVRON_RIGHT -> line(p(9f, 6f), p(15f, 12f), p(9f, 18f))
            BwIconKind.CHEVRON_LEFT -> line(p(15f, 6f), p(9f, 12f), p(15f, 18f))
            BwIconKind.CHEVRON_DOWN -> line(p(6f, 9f), p(12f, 15f), p(18f, 9f))
            BwIconKind.PLAY -> drawPath(Path().apply {
                moveTo(7f * k, 4.5f * k); lineTo(20f * k, 12f * k); lineTo(7f * k, 19.5f * k); close()
            }, color)
            BwIconKind.SETTINGS -> {
                drawCircle(color, 3.2f * k, p(12f, 12f), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = kotlin.math.cos(a).toFloat(); val s = kotlin.math.sin(a).toFloat()
                    line(p(12f + c * 6.5f, 12f + s * 6.5f), p(12f + c * 9.5f, 12f + s * 9.5f))
                }
                drawCircle(color, 6.5f * k, p(12f, 12f), style = stroke)
            }
            BwIconKind.PLUS -> { line(p(12f, 5f), p(12f, 19f)); line(p(5f, 12f), p(19f, 12f)) }
            BwIconKind.CHECK -> line(p(5f, 12.5f), p(10f, 17f), p(19f, 7f))
            BwIconKind.CHART -> { line(p(4f, 20f), p(4f, 11f)); line(p(10f, 20f), p(10f, 4f)); line(p(16f, 20f), p(16f, 13f)); line(p(2f, 20f), p(22f, 20f)) }
            BwIconKind.SWORDS -> { line(p(4f, 4f), p(16f, 16f)); line(p(20f, 4f), p(8f, 16f)); line(p(13f, 19f), p(19f, 13f)); line(p(5f, 13f), p(11f, 19f)) }
            BwIconKind.HELP -> {
                drawCircle(color, 9.5f * k, p(12f, 12f), style = stroke)
                line(p(9.5f, 9.5f), p(10.5f, 7.8f), p(13.5f, 7.6f), p(14.6f, 9.6f), p(12f, 12.5f), p(12f, 13.5f))
                drawCircle(color, 1.1f * k, p(12f, 16.8f))
            }
            BwIconKind.CUBE -> {
                line(p(12f, 2.5f), p(21f, 7.5f), p(21f, 16.5f), p(12f, 21.5f), p(3f, 16.5f), p(3f, 7.5f), p(12f, 2.5f))
                line(p(3f, 7.5f), p(12f, 12.5f), p(21f, 7.5f)); line(p(12f, 12.5f), p(12f, 21.5f))
            }
        }
    }
}
