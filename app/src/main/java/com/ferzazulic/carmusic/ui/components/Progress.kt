package com.ferzazulic.carmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.ProgressStyle

/** Seek bar drawn in the active theme's style (line, glow, segmented like RNS/CID, thick). */
@Composable
fun CarProgressBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalCarTheme.current
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val raw = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(raw, tween(if (t.animate) 300 else 0), label = "progress")
    val fraction = dragFraction ?: animated
    val thumb by animateFloatAsState(if (dragFraction != null) 1f else 0f, tween(200), label = "thumb")
    val style = t.base.progress

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(durationMs) {
                detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFraction?.let { if (durationMs > 0) onSeek((it * durationMs).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                ) { change, _ -> dragFraction = (change.position.x / size.width).coerceIn(0f, 1f) }
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val cy = size.height / 2
            val w = size.width
            val track = t.text.copy(alpha = 0.16f)
            when (style) {
                ProgressStyle.SEGMENTED -> {
                    val n = 40
                    val gap = 3.dp.toPx()
                    val segW = (w - gap * (n - 1)) / n
                    val h = 10.dp.toPx()
                    for (i in 0 until n) {
                        val x = i * (segW + gap)
                        val on = (i + 0.5f) / n <= fraction
                        drawRoundRect(
                            if (on) t.accent else track, Offset(x, cy - h / 2), Size(segW, h),
                            CornerRadius(2.dp.toPx()),
                        )
                    }
                }
                else -> {
                    val stroke = when (style) {
                        ProgressStyle.THICK -> 8.dp.toPx()
                        ProgressStyle.GLOW -> 4.dp.toPx()
                        else -> 3.dp.toPx()
                    }
                    val cap = if (t.corner.value < 3f) StrokeCap.Butt else StrokeCap.Round
                    drawLine(track, Offset(0f, cy), Offset(w, cy), stroke, cap)
                    val end = Offset(w * fraction, cy)
                    if (style == ProgressStyle.GLOW) {
                        drawLine(t.accent.copy(alpha = 0.25f), Offset(0f, cy), end, stroke * 4, StrokeCap.Round)
                    }
                    drawLine(
                        Brush.horizontalGradient(listOf(t.accent.copy(alpha = 0.7f), t.accent), 0f, end.x.coerceAtLeast(1f)),
                        Offset(0f, cy), end, stroke, cap,
                    )
                    val r = (6.dp.toPx() + 4.dp.toPx() * thumb)
                    if (style == ProgressStyle.GLOW || thumb > 0f) {
                        drawCircle(t.accent.copy(alpha = 0.25f), r * 2f, end)
                    }
                    drawCircle(Color.White, r, end)
                }
            }
        }
    }
}
