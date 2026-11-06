package com.ferzazulic.carmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ferzazulic.carmusic.ui.theme.BgPattern
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Theme background: vertical gradient, the theme's signature pattern and two slowly drifting
 * "aurora" glows tinted by the album artwork.
 */
@Composable
fun CarBackground(
    glow: Color?,
    playing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalCarTheme.current
    val glowColor by animateColorAsState(glow ?: t.accent, tween(1200), label = "glow")
    val intensity by animateFloatAsState(if (playing) 1f else 0.55f, tween(1500), label = "intensity")
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(if (t.rich) 22_000 else 1_000_000, easing = LinearEasing)),
        label = "phase",
    )
    val pulse by transition.animateFloat(
        0.85f, 1.1f,
        infiniteRepeatable(tween(if (t.rich) 4_000 else 1_000_000), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (t.base.light) {
                drawPattern(t.base.pattern, t.text.copy(alpha = 0.045f))
                return@Canvas
            }
            val w = size.width
            val h = size.height
            val r = maxOf(w, h) * 0.55f * pulse
            val c1 = Offset(w * (0.25f + 0.12f * cos(phase)), h * (0.35f + 0.15f * sin(phase)))
            val c2 = Offset(w * (0.8f + 0.1f * sin(phase * 1.3f)), h * (0.75f + 0.12f * cos(phase * 0.7f)))
            drawCircle(
                Brush.radialGradient(listOf(glowColor.copy(alpha = 0.32f * intensity), Color.Transparent), c1, r),
                radius = r, center = c1,
            )
            drawCircle(
                Brush.radialGradient(listOf(t.accent.copy(alpha = 0.16f * intensity), Color.Transparent), c2, r * 0.8f),
                radius = r * 0.8f, center = c2,
            )
            drawPattern(t.base.pattern, t.text.copy(alpha = 0.045f))
        }
        content()
    }
}

private fun DrawScope.drawPattern(pattern: BgPattern, color: Color) {
    val w = size.width
    val h = size.height
    when (pattern) {
        BgPattern.NONE -> Unit
        BgPattern.GRID -> {
            val step = 48f
            var x = 0f
            while (x < w) { drawLine(color, Offset(x, 0f), Offset(x, h), 1f); x += step }
            var y = 0f
            while (y < h) { drawLine(color, Offset(0f, y), Offset(w, y), 1f); y += step }
        }
        BgPattern.LINES -> {
            var y = 0f
            while (y < h) { drawLine(color, Offset(0f, y), Offset(w, y), 1f); y += 6f }
        }
        BgPattern.DOTS -> {
            val step = 28f
            var y = step / 2
            while (y < h) {
                var x = step / 2
                while (x < w) { drawCircle(color.copy(alpha = color.alpha * 1.6f), 1.6f, Offset(x, y)); x += step }
                y += step
            }
        }
        BgPattern.CHEVRON -> {
            val step = 36f
            var x = -h
            while (x < w) {
                drawLine(color, Offset(x, h), Offset(x + h, 0f), 1.2f)
                x += step
            }
        }
        BgPattern.CARBON -> {
            val step = 8f
            var y = 0f
            var row = 0
            while (y < h) {
                var x = if (row % 2 == 0) 0f else step
                while (x < w) {
                    drawRect(color.copy(alpha = color.alpha * 1.4f), Offset(x, y), androidx.compose.ui.geometry.Size(step, step / 2))
                    x += step * 2
                }
                y += step / 2
                row++
            }
        }
        BgPattern.HONEYCOMB -> {
            val r = 22f
            val hexH = sqrt(3f) * r
            val stroke = Stroke(1.2f)
            var row = 0
            var y = 0f
            while (y < h + hexH) {
                var x = if (row % 2 == 0) 0f else r * 1.5f
                while (x < w + r) {
                    val p = Path()
                    for (i in 0..5) {
                        val a = Math.toRadians((60 * i).toDouble())
                        val px = x + r * cos(a).toFloat()
                        val py = y + r * sin(a).toFloat()
                        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                    }
                    p.close()
                    drawPath(p, color, style = stroke)
                    x += r * 3f
                }
                y += hexH / 2
                row++
            }
        }
    }
}
