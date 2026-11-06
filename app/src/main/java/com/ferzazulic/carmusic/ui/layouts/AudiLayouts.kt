package com.ferzazulic.carmusic.ui.layouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.ferzazulic.carmusic.ui.Screen
import com.ferzazulic.carmusic.ui.components.Clock
import com.ferzazulic.carmusic.ui.components.SpectrumBars
import com.ferzazulic.carmusic.ui.components.formatTime
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.PlayerLayout
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================================ MMI 3G+
// Black screen; four corner function-key captions in amber with curved underlines; amber centre title;
// list in the middle with the current entry framed; scroll arc at the left; status line with clock.

@Composable
fun MmiShell(screen: Screen, onSelect: (Screen) -> Unit, m: PlayerModel, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Box(Modifier.fillMaxWidth().height(54.dp)) {
            MmiCaption("Media", top = true, start = true, active = screen == Screen.NOW_PLAYING, modifier = Modifier.align(Alignment.CenterStart)) { onSelect(Screen.NOW_PLAYING) }
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Text(screen.factoryTitle(PlayerLayout.MMI), color = t.accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.background(t.accent).padding(horizontal = 3.dp)) {
                    Text("SD", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            MmiCaption("Lista", top = true, start = false, active = screen == Screen.LIBRARY, modifier = Modifier.align(Alignment.CenterEnd)) { onSelect(Screen.LIBRARY) }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) { body() }
        Box(Modifier.fillMaxWidth().height(50.dp)) {
            MmiCaption(if (m.pb.shuffle) "Mix: uklj." else "Functions", top = false, start = true, active = m.pb.shuffle, modifier = Modifier.align(Alignment.CenterStart), onClick = m.a.toggleShuffle)
            MmiCaption("Settings", top = false, start = false, active = screen == Screen.SETTINGS, modifier = Modifier.align(Alignment.CenterEnd)) { onSelect(Screen.SETTINGS) }
        }
        // status line
        Box(Modifier.fillMaxWidth().height(34.dp)) {
            Row(Modifier.align(Alignment.CenterStart), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MmiTag("MP3", true)
                MmiTag("RPT", m.pb.repeatMode != Player.REPEAT_MODE_OFF)
            }
            Clock(Modifier.align(Alignment.Center))
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                MmiMiniKey(Icons.Rounded.SkipPrevious, m.a.previous)
                MmiMiniKey(if (m.pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay)
                MmiMiniKey(Icons.Rounded.SkipNext, m.a.next)
            }
        }
    }
}

@Composable
private fun MmiTag(text: String, on: Boolean) {
    val t = LocalCarTheme.current
    Box(Modifier.border(1.dp, if (on) t.text else t.textMuted.copy(alpha = 0.4f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
        Text(text, color = if (on) t.text else t.textMuted.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MmiMiniKey(icon: ImageVector, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Box(Modifier.size(width = 52.dp, height = 34.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = t.text, modifier = Modifier.size(24.dp))
    }
}

/** Corner caption with the characteristic amber underline that curves towards the screen centre. */
@Composable
private fun MmiCaption(label: String, top: Boolean, start: Boolean, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val glow by animateFloatAsState(if (active) 1f else 0.75f, tween(t.duration(250)), label = "mmiCap")
    Box(
        modifier
            .width(230.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .drawBehind {
                val y = if (top) size.height - 6.dp.toPx() else 6.dp.toPx()
                val len = size.width * 0.9f
                val r = 10.dp.toPx()
                val p = Path()
                if (start) {
                    p.moveTo(0f, y)
                    p.lineTo(len - r, y)
                    p.quadraticBezierTo(len, y, len, y + if (top) -r else r)
                } else {
                    p.moveTo(size.width, y)
                    p.lineTo(size.width - len + r, y)
                    p.quadraticBezierTo(size.width - len, y, size.width - len, y + if (top) -r else r)
                }
                drawPath(
                    p,
                    Brush.horizontalGradient(
                        if (start) listOf(t.accent.copy(alpha = glow), t.accent.copy(alpha = glow * 0.6f))
                        else listOf(t.accent.copy(alpha = glow * 0.6f), t.accent.copy(alpha = glow)),
                    ),
                    style = Stroke(2.5.dp.toPx()),
                )
            }
            .padding(horizontal = 6.dp),
        contentAlignment = if (start) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(label, color = t.accent, style = MaterialTheme.typography.titleLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun MmiNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Memorija", color = t.text, style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Rounded.SubdirectoryArrowLeft, null, tint = t.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Folder, null, tint = t.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(pb.album.ifBlank { "Muzika" }, color = t.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().drawBehind {
                // scroll arc on the left
                val p = Path()
                val x = 30.dp.toPx()
                p.moveTo(x + 20.dp.toPx(), 8.dp.toPx())
                p.quadraticBezierTo(x - 14.dp.toPx(), size.height / 2, x + 20.dp.toPx(), size.height - 8.dp.toPx())
                drawPath(p, t.text.copy(alpha = 0.55f), style = Stroke(2.dp.toPx()))
                val fy = if (pb.count > 1) pb.index.toFloat() / (pb.count - 1) else 0.5f
                drawCircle(t.accent, 5.dp.toPx(), Offset(x + 1.dp.toPx(), 12.dp.toPx() + fy * (size.height - 24.dp.toPx())))
            },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = pb.index,
                transitionSpec = {
                    val d = if (targetState >= initialState) 1 else -1
                    (slideInVertically(tween(t.duration(320))) { it / 6 * d } + fadeIn(tween(t.duration(260)))) togetherWith
                        (slideOutVertically(tween(t.duration(320))) { -it / 6 * d } + fadeOut(tween(t.duration(180))))
                },
                label = "mmiList",
            ) { current ->
                Column(Modifier.fillMaxWidth().padding(start = 70.dp, end = 40.dp)) {
                    for (offset in -2..2) {
                        val i = current + offset
                        val s = m.queue.getOrNull(i)
                        if (i < 0 || i >= m.queue.size) Spacer(Modifier.height(48.dp))
                        else MmiRow(s?.let { "${it.artist} - ${it.title}" } ?: "—", offset == 0, pb.isPlaying) {
                            if (offset == 0) m.a.togglePlay() else m.a.playIndex(i)
                        }
                    }
                }
            }
        }
        FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.FLAT, Modifier.padding(horizontal = 70.dp), height = 12.dp, track = t.text.copy(alpha = 0.15f))
    }
}

@Composable
private fun MmiRow(text: String, current: Boolean, playing: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.fillMaxWidth().height(48.dp)
            .then(
                if (current) Modifier
                    .background(Brush.verticalGradient(listOf(t.accent.copy(alpha = 0.25f), Color.Transparent, t.accent.copy(alpha = 0.18f))), shape)
                    .border(2.dp, Brush.horizontalGradient(listOf(t.accent, t.accentSecondary, t.accent)), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (current) text.uppercase() else text,
                color = t.text,
                style = if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (current) SpectrumBars(playing, Modifier.size(24.dp, 18.dp), bars = 4)
        }
    }
}

// ===================================================================================== VIRTUAL COCKPIT
// Tab strip on top with the active tab framed by an amber trapezoid; amber source caption; text block
// with icons, slim amber progress, cover on the right; round instruments in the lower corners;
// clock in the bottom centre.

@Composable
fun VcShell(screen: Screen, onSelect: (Screen) -> Unit, m: PlayerModel, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            VcTab(Icons.Rounded.Settings, if (screen == Screen.SETTINGS) "Settings" else null) { onSelect(Screen.SETTINGS) }
            VcSeparator()
            VcTab(Icons.Rounded.PlayCircleOutline, if (screen == Screen.NOW_PLAYING) m.pb.album.ifBlank { m.title }.uppercase() else null) { onSelect(Screen.NOW_PLAYING) }
            VcSeparator()
            VcTab(Icons.AutoMirrored.Rounded.QueueMusic, if (screen == Screen.LIBRARY) "Lista" else null) { onSelect(Screen.LIBRARY) }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) { body() }
    }
}

@Composable
private fun VcSeparator() {
    val t = LocalCarTheme.current
    Box(Modifier.padding(bottom = 10.dp).width(1.dp).height(26.dp).background(t.textMuted.copy(alpha = 0.5f)))
}

@Composable
private fun VcTab(icon: ImageVector, activeLabel: String?, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val active = activeLabel != null
    Box(
        Modifier
            .height(50.dp)
            .widthIn(min = 76.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (active) {
                    val skew = 16.dp.toPx()
                    val p = Path().apply {
                        moveTo(-20.dp.toPx(), size.height)
                        lineTo(0f, size.height)
                        lineTo(skew, 0f)
                        lineTo(size.width - skew, 0f)
                        lineTo(size.width, size.height)
                        lineTo(size.width + 20.dp.toPx(), size.height)
                    }
                    drawPath(p, t.accent, style = Stroke(2.dp.toPx()))
                }
            }
            .padding(horizontal = if (active) 30.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = t.text, modifier = Modifier.size(28.dp))
            if (activeLabel != null) {
                Spacer(Modifier.width(10.dp))
                Text(activeLabel, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 320.dp))
            }
        }
    }
}

@Composable
fun VcNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dial = minOf(maxHeight * 0.55f, 220.dp)
        val coverSize = minOf(maxHeight * 0.42f, 210.dp)
        Column(Modifier.fillMaxSize().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Medij · Memorija", color = t.accent, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(0.62f), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    VcLine(Icons.Rounded.MusicNote, m.title, bold = true)
                    VcLine(Icons.Rounded.Mic, m.artist)
                    VcLine(Icons.Rounded.Album, pb.album.ifBlank { "—" })
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(pb.positionMs), color = t.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(56.dp))
                        Box(
                            Modifier.weight(1f).border(1.dp, t.textMuted.copy(alpha = 0.5f), RoundedCornerShape(3.dp)).padding(2.dp)
                        ) {
                            FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.SEGMENT_GLOW, height = 10.dp, fill = t.accent, fillEnd = t.accentSecondary, track = Color(0xFF1E2024))
                        }
                        Text("-" + formatTime(m.remaining), color = t.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.width(36.dp))
                Box(Modifier.size(coverSize)) { AnimatedCover(m, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VcKey(Icons.Rounded.SkipPrevious, m.a.previous)
                VcKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay)
                VcKey(Icons.Rounded.SkipNext, m.a.next)
            }
            HLine(t.textMuted.copy(alpha = 0.35f), Modifier.fillMaxWidth(0.5f))
            Clock(Modifier.padding(vertical = 6.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 8.dp).size(dial)) {
            VcDial(if (pb.count > 1) pb.index.toFloat() / (pb.count - 1) else 0f, segmented = true)
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pb.count > 0) "${pb.index + 1}" else "–", color = t.text, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text("PJESMA", color = t.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 8.dp).size(dial)) {
            VcDial(m.fraction, segmented = false)
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(m.remaining / 60_000)}", color = t.text, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text("min", color = t.text, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VcLine(icon: ImageVector, text: String, bold: Boolean = false) {
    val t = LocalCarTheme.current
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = t.text, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        AnimatedContent(text, transitionSpec = { fadeIn(tween(t.duration(350))) togetherWith fadeOut(tween(t.duration(180))) }, label = "vcLine") {
            Text(
                if (bold) it.uppercase() else it, color = t.text,
                style = if (bold) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VcKey(icon: ImageVector, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Box(Modifier.size(width = 80.dp, height = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = t.textMuted, modifier = Modifier.size(30.dp))
    }
}

/** Round instrument: chrome bezel, tick ring and white needle (or a segmented arc). */
@Composable
private fun VcDial(fraction: Float, segmented: Boolean) {
    val t = LocalCarTheme.current
    val f by animateFloatAsState(fraction, tween(500), label = "vcDial")
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val r = size.minDimension / 2
        drawCircle(Brush.sweepGradient(listOf(Color(0xFF9DA3AA), Color(0xFF3A3D42), Color(0xFFD7DBE0), Color(0xFF3A3D42), Color(0xFF9DA3AA)), c), r, c, style = Stroke(r * 0.06f))
        drawCircle(Color(0xFF0B0C0E), r * 0.94f, c)
        drawCircle(Color(0xFF8E9399), r * 0.55f, c, style = Stroke(r * 0.03f))
        val start = 135f
        val sweep = 270f
        for (i in 0..30) {
            val a = Math.toRadians((start + sweep * i / 30).toDouble())
            val major = i % 5 == 0
            val r1 = r * 0.74f
            val r2 = r * if (major) 0.88f else 0.83f
            val color = when {
                !segmented -> Color.White.copy(alpha = if (major) 0.95f else 0.6f)
                i / 30f <= f -> Color(0xFF3DDC5A)
                else -> Color.White.copy(alpha = 0.3f)
            }
            drawLine(color, Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()), Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()), if (major) 4f else 2.5f)
        }
        if (segmented) {
            drawArc(t.accent, start + sweep * 0.85f, sweep * 0.15f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.04f))
        }
        val a = Math.toRadians((start + sweep * f).toDouble())
        drawLine(
            Color.White,
            Offset(c.x + r * 0.58f * cos(a).toFloat(), c.y + r * 0.58f * sin(a).toFloat()),
            Offset(c.x + r * 0.9f * cos(a).toFloat(), c.y + r * 0.9f * sin(a).toFloat()),
            r * 0.035f, cap = StrokeCap.Round,
        )
    }
}
