package com.ferzazulic.carmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * Decorative spectrum animation (no microphone/visualizer permission needed). Each bar is a sum of
 * sines with its own frequencies, so the motion looks organic and never repeats visibly.
 */
@Composable
fun SpectrumBars(playing: Boolean, modifier: Modifier = Modifier, bars: Int = 32) {
    val t = LocalCarTheme.current
    var time by remember { mutableLongStateOf(0L) }
    val energy by animateFloatAsState(if (playing) 1f else 0.06f, tween(900), label = "energy")
    LaunchedEffect(playing, t.animate) {
        if (!t.animate) return@LaunchedEffect
        val start = time
        val origin = withFrameMillis { it }
        while (true) {
            withFrameMillis { time = start + (it - origin) }
            if (!playing && energy <= 0.07f) break
        }
    }
    val seeds = remember(bars) { List(bars) { i -> Triple(0.9f + (i * 37 % 11) / 7f, 1.7f + (i * 53 % 13) / 5f, (i * 29 % 17) / 3f) } }
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        val sec = time / 1000f
        for (i in 0 until bars) {
            val (a, b, p) = seeds[i]
            val shape = 1f - abs(i - bars / 2f) / (bars * 0.75f)
            val v = (0.55f + 0.45f * sin(sec * a * 2.2f + p)) * (0.6f + 0.4f * sin(sec * b * 1.3f + p * 2)).let { abs(it) }
            val h = (0.08f + v * shape * energy).coerceIn(0.04f, 1f) * size.height
            drawRoundRect(
                Brush.verticalGradient(listOf(t.accent, t.accent.copy(alpha = 0.25f)), size.height - h, size.height),
                Offset(i * (bw + gap), size.height - h), Size(bw, h),
                CornerRadius(if (t.corner.value < 3f) 0f else bw / 2),
            )
        }
    }
}
