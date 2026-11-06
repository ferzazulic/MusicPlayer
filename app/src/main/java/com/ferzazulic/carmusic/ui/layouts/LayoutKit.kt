package com.ferzazulic.carmusic.ui.layouts

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

/** Raises/lowers the media volume (for layouts that have on-screen volume keys). */
@Composable
fun rememberVolumeControl(): (Int) -> Unit {
    val context = LocalContext.current
    return remember {
        { dir: Int ->
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (dir > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
        }
    }
}

enum class BarStyle { FLAT, DOTTED, CHROME, SEGMENT_GLOW }

/** Generic seek bar used by the factory layouts; tap anywhere to seek. */
@Composable
fun FactoryBar(
    fraction: Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    style: BarStyle,
    modifier: Modifier = Modifier,
    height: Dp = 22.dp,
    fill: Color? = null,
    fillEnd: Color? = null,
    track: Color? = null,
) {
    val t = LocalCarTheme.current
    val f by animateFloatAsState(fraction, tween(if (t.animate) 300 else 0), label = "bar")
    val fillC = fill ?: t.accent
    val trackC = track ?: t.text.copy(alpha = 0.25f)
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(durationMs) {
            detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
        }
    ) {
        val cy = size.height / 2
        val w = size.width
        when (style) {
            BarStyle.FLAT -> {
                val th = 4.dp.toPx()
                drawLine(trackC, Offset(0f, cy), Offset(w, cy), th)
                drawLine(
                    if (fillEnd != null) Brush.horizontalGradient(listOf(fillC, fillEnd), 0f, (w * f).coerceAtLeast(1f)) else Brush.horizontalGradient(listOf(fillC, fillC)),
                    Offset(0f, cy), Offset(w * f, cy), th,
                )
            }
            BarStyle.DOTTED -> {
                val step = 6.dp.toPx()
                var x = 0f
                while (x <= w) {
                    drawCircle(trackC, 1.4.dp.toPx(), Offset(x, cy))
                    x += step
                }
                drawLine(fillC, Offset(0f, cy), Offset(w * f, cy), 3.dp.toPx(), StrokeCap.Round)
            }
            BarStyle.CHROME -> {
                val th = 7.dp.toPx()
                drawRoundRect(trackC, Offset(0f, cy - th / 2), Size(w, th), CornerRadius(th / 2))
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White, fillC, fillEnd ?: fillC), cy - th / 2, cy + th / 2),
                    Offset(0f, cy - th / 2), Size(w * f, th), CornerRadius(th / 2),
                )
            }
            BarStyle.SEGMENT_GLOW -> {
                val th = 8.dp.toPx()
                drawRoundRect(trackC, Offset(0f, cy - th / 2), Size(w, th), CornerRadius(2f))
                drawRoundRect(Brush.horizontalGradient(listOf(fillC, fillEnd ?: fillC), 0f, (w * f).coerceAtLeast(1f)), Offset(0f, cy - th / 2), Size(w * f, th), CornerRadius(2f))
            }
        }
    }
}
