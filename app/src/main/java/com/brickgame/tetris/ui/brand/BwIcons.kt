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
        // Polyline through (x, y) pairs on the 24×24 grid (Offset is a value class, so no vararg Offset)
        fun line(vararg xy: Float) {
            val path = Path().apply {
                moveTo(xy[0] * k, xy[1] * k)
                var i = 2
                while (i < xy.size) { lineTo(xy[i] * k, xy[i + 1] * k); i += 2 }
            }
            drawPath(path, color, style = stroke)
        }
        when (kind) {
            BwIconKind.CHEVRON_RIGHT -> line(9f, 6f, 15f, 12f, 9f, 18f)
            BwIconKind.CHEVRON_LEFT -> line(15f, 6f, 9f, 12f, 15f, 18f)
            BwIconKind.CHEVRON_DOWN -> line(6f, 9f, 12f, 15f, 18f, 9f)
            BwIconKind.PLAY -> drawPath(Path().apply {
                moveTo(7f * k, 4.5f * k); lineTo(20f * k, 12f * k); lineTo(7f * k, 19.5f * k); close()
            }, color)
            BwIconKind.SETTINGS -> {
                drawCircle(color, 3.2f * k, p(12f, 12f), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = kotlin.math.cos(a).toFloat(); val s = kotlin.math.sin(a).toFloat()
                    line(12f + c * 6.5f, 12f + s * 6.5f, 12f + c * 9.5f, 12f + s * 9.5f)
                }
                drawCircle(color, 6.5f * k, p(12f, 12f), style = stroke)
            }
            BwIconKind.PLUS -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            BwIconKind.CHECK -> line(5f, 12.5f, 10f, 17f, 19f, 7f)
            BwIconKind.CHART -> { line(4f, 20f, 4f, 11f); line(10f, 20f, 10f, 4f); line(16f, 20f, 16f, 13f); line(2f, 20f, 22f, 20f) }
            BwIconKind.SWORDS -> { line(4f, 4f, 16f, 16f); line(20f, 4f, 8f, 16f); line(13f, 19f, 19f, 13f); line(5f, 13f, 11f, 19f) }
            BwIconKind.HELP -> {
                drawCircle(color, 9.5f * k, p(12f, 12f), style = stroke)
                line(9.5f, 9.5f, 10.5f, 7.8f, 13.5f, 7.6f, 14.6f, 9.6f, 12f, 12.5f, 12f, 13.5f)
                drawCircle(color, 1.1f * k, p(12f, 16.8f))
            }
            BwIconKind.CUBE -> {
                line(12f, 2.5f, 21f, 7.5f, 21f, 16.5f, 12f, 21.5f, 3f, 16.5f, 3f, 7.5f, 12f, 2.5f)
                line(3f, 7.5f, 12f, 12.5f, 21f, 7.5f); line(12f, 12.5f, 12f, 21.5f)
            }
        }
    }
}
