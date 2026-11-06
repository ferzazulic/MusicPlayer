package com.ferzazulic.carmusic.ui.layouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.settings.AppSettings
import com.ferzazulic.carmusic.ui.PlaybackUi
import com.ferzazulic.carmusic.ui.components.ArtworkImage
import com.ferzazulic.carmusic.ui.components.CarProgressBar
import com.ferzazulic.carmusic.ui.components.PlayPauseButton
import com.ferzazulic.carmusic.ui.components.SpectrumBars
import com.ferzazulic.carmusic.ui.components.formatTime
import com.ferzazulic.carmusic.ui.nowplaying.NowPlayingActions
import com.ferzazulic.carmusic.ui.nowplaying.NowPlayingScreen
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.PlayerLayout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PlayerModel(
    val pb: PlaybackUi,
    val song: Song?,
    /** Queue in playlist order; null where a song is no longer in the library. */
    val queue: List<Song?>,
    val settings: AppSettings,
    val a: NowPlayingActions,
    /** Navigate to another app screen (for layouts whose Now Playing view carries navigation keys). */
    val navigate: (com.ferzazulic.carmusic.ui.Screen) -> Unit = {},
) {
    val title get() = pb.title.ifBlank { "Ništa ne svira" }
    val artist get() = pb.artist.ifBlank { "Odaberi pjesmu iz biblioteke" }
    val remaining get() = (pb.durationMs - pb.positionMs).coerceAtLeast(0)
    val isFavorite get() = song?.id?.toString()?.let { it in settings.favorites } == true
    val fraction get() = if (pb.durationMs > 0) (pb.positionMs.toFloat() / pb.durationMs).coerceIn(0f, 1f) else 0f
}

@Composable
fun FactoryNowPlaying(layout: PlayerLayout, m: PlayerModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < maxHeight) {
            NowPlayingScreen(m.pb, m.song, m.settings, m.a)
        } else when (layout) {
            PlayerLayout.RCD -> RcdNowPlaying(m)
            PlayerLayout.MIB3 -> Mib3NowPlaying(m)
            PlayerLayout.AMUNDSEN -> AmundsenNowPlaying(m)
            PlayerLayout.SEAT -> SeatNowPlaying(m)
            PlayerLayout.CUPRA -> CupraNowPlaying(m)
            PlayerLayout.MMI -> MmiNowPlaying(m)
            PlayerLayout.VC -> VcNowPlaying(m)
            PlayerLayout.PCM -> PcmNowPlaying(m)
            PlayerLayout.CID -> CidNowPlaying(m)
            PlayerLayout.COCKPIT -> CockpitNowPlaying(m)
            PlayerLayout.DIALS -> DialsNowPlaying(m)
        }
    }
}

/**
 * Cover that slides in from the direction of travel through the queue. Swipe left/right on it to
 * change track; the heart in the corner adds the song to favourites.
 */
@Composable
internal fun AnimatedCover(m: PlayerModel, modifier: Modifier, shape: Shape? = null, artSize: Int = 720) {
    val t = LocalCarTheme.current
    val direction = m.pb.direction
    var drag by remember { mutableFloatStateOf(0f) }
    val dragAnim by animateFloatAsState(drag, spring(dampingRatio = 0.7f), label = "coverDrag")
    Box(
        modifier
            .graphicsLayer { translationX = dragAnim * 0.35f; rotationZ = dragAnim / 90f }
            .pointerInput(m.settings.swipeOnCover) {
                if (!m.settings.swipeOnCover) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = size.width * 0.18f
                        when {
                            drag < -threshold -> m.a.next()
                            drag > threshold -> m.a.previous()
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag += amount
                }
            }
    ) {
        AnimatedContent(
            targetState = m.song,
            contentKey = { it?.id },
            transitionSpec = {
                if (!t.animate) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (slideInHorizontally(tween(t.duration(480))) { w -> direction * w / 3 } + fadeIn(tween(t.duration(400))) +
                    scaleIn(tween(t.duration(480)), initialScale = 0.86f)) togetherWith
                    (slideOutHorizontally(tween(t.duration(480))) { w -> -direction * w / 3 } + fadeOut(tween(t.duration(260))) +
                        scaleOut(tween(t.duration(480)), targetScale = 0.86f))
            },
            modifier = Modifier.fillMaxSize(),
            label = "cover",
        ) { s -> ArtworkImage(s, Modifier.fillMaxSize(), size = artSize, shape = shape) }
        if (m.song != null && shape != CircleShape) {
            FavoriteBadge(m.isFavorite, m.a.toggleFavorite, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
    }
}

@Composable
fun FavoriteBadge(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalCarTheme.current
    val scale by animateFloatAsState(if (active) 1.15f else 1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "fav")
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (active) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (active) "Ukloni iz omiljenih" else "Dodaj u omiljene",
            tint = if (active) t.accent else Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
internal fun repeatIcon(mode: Int) = if (mode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat

@Composable
internal fun pressScale(source: MutableInteractionSource): Float {
    val t = LocalCarTheme.current
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed && t.animate) 0.9f else 1f, spring(dampingRatio = 0.5f), label = "press")
    return s
}

// =========================================================================================== COCKPIT

@Composable
private fun CockpitNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val size = minOf(maxHeight * 0.96f, maxWidth * 0.34f)
            val side = size * 0.7f
            val shift = size * 0.5f + side * 0.45f
            // Neighbouring covers in 3D perspective (cover flow)
            CoverFlowSide(m.queue.getOrNull(pb.prevIndex), side, -shift, 38f) { m.a.previous() }
            CoverFlowSide(m.queue.getOrNull(pb.nextIndex), side, shift, -38f) { m.a.next() }
            val glow by animateFloatAsState(if (pb.isPlaying) 1f else 0.3f, tween(800), label = "vcGlow")
            Box(
                Modifier.size(size)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(t.accent.copy(alpha = 0.35f * glow), Color.Transparent), center, this.size.maxDimension * 0.75f),
                            this.size.maxDimension * 0.75f,
                        )
                    }
                    .shadow(24.dp, RoundedCornerShape(t.corner))
            ) { AnimatedCover(m, Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.height(10.dp))
        MarqueeTitle(m.title, MaterialTheme.typography.headlineMedium, t.text, center = true)
        Text(
            buildString { append(m.artist); if (pb.album.isNotBlank()) append("  ·  ").append(pb.album) },
            color = t.textMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
            Box(Modifier.weight(1f)) { CarProgressBar(pb.positionMs, pb.durationMs, m.a.seekTo) }
            Text("-" + formatTime(m.remaining), color = t.textMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            RoundKey(Icons.Rounded.Shuffle, m.a.toggleShuffle, active = pb.shuffle, size = 52.dp)
            RoundKey(Icons.Rounded.SkipPrevious, m.a.previous, size = 64.dp)
            PlayPauseButton(pb.isPlaying, m.a.togglePlay, size = if (m.settings.largeControls) 84.dp else 72.dp)
            RoundKey(Icons.Rounded.SkipNext, m.a.next, size = 64.dp)
            RoundKey(repeatIcon(pb.repeatMode), m.a.cycleRepeat, active = pb.repeatMode != Player.REPEAT_MODE_OFF, size = 52.dp)
        }
    }
}

@Composable
private fun CoverFlowSide(song: Song?, size: Dp, offsetX: Dp, rotation: Float, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    if (song == null) return
    AnimatedContent(
        targetState = song,
        contentKey = { it.id },
        transitionSpec = { fadeIn(tween(t.duration(500))) togetherWith fadeOut(tween(t.duration(300))) },
        label = "flow",
        modifier = Modifier.offset(x = offsetX).size(size),
    ) { s ->
        ArtworkImage(
            s,
            Modifier.fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                    alpha = 0.5f
                }
                .clickable(onClick = onClick),
            size = 320,
        )
    }
}

@Composable
internal fun RoundKey(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, size: Dp = 60.dp) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    Box(
        Modifier.size(size)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(CircleShape)
            .border(1.5.dp, if (active) t.accent else t.text.copy(alpha = 0.3f), CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) t.accent else t.text, modifier = Modifier.size(size * 0.5f)) }
}


// ============================================================================================= DIALS

/**
 * Three-dial digital cockpit (sport instrument cluster structure): small dial left (queue position),
 * large centre dial with a numbered scale (song progress in minutes, elapsed time in the middle),
 * information dial right (title / artist / album). Honeycomb fill when the theme uses it.
 */
@Composable
private fun DialsNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    val animated by animateFloatAsState(m.fraction, tween(if (t.animate) 400 else 0), label = "dial")
    val sweepIn = remember { Animatable(if (t.animate) 0f else 1f) }
    LaunchedEffect(Unit) { sweepIn.animateTo(1f, tween(t.duration(1200), easing = FastOutSlowInEasing)) }
    val frac = animated * sweepIn.value
    val queueFrac = if (pb.count > 1) pb.index.toFloat() / (pb.count - 1) else 0f

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("\u266A ${if (pb.count > 0) "${pb.index + 1}/${pb.count}" else "—"}", color = t.text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            if (pb.shuffle) Text("MIX", color = t.accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val big = minOf(maxHeight, maxWidth * 0.42f)
            val small = big * 0.68f
            // red glow between the dials
            Canvas(Modifier.fillMaxSize()) {
                if (t.base.glow) {
                    listOf(0.29f, 0.71f).forEach { x ->
                        drawCircle(
                            Brush.radialGradient(listOf(t.accent.copy(alpha = 0.5f), Color.Transparent), Offset(size.width * x, size.height * 0.5f), size.height * 0.12f),
                            size.height * 0.12f, Offset(size.width * x, size.height * 0.5f),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(big * 0.06f)) {
                // left: queue position dial
                Box(Modifier.size(small), contentAlignment = Alignment.Center) {
                    DialFace(queueFrac, ticks = false, labels = emptyList(), startAngle = 135f, sweep = 180f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (pb.count > 0) "${pb.index + 1}" else "0", color = t.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Text("pjesma", color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
                // centre: song progress dial
                Box(
                    Modifier.size(big).pointerInput(pb.durationMs) {
                        detectTapGestures { o ->
                            val c = Offset(size.width / 2f, size.height / 2f)
                            var ang = Math.toDegrees(atan2((o.y - c.y).toDouble(), (o.x - c.x).toDouble())).toFloat()
                            ang = (ang - 135f + 720f) % 360f
                            if (ang <= 270f && pb.durationMs > 0) m.a.seekTo((ang / 270f * pb.durationMs).toLong())
                            else m.a.togglePlay()
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    val minutes = ((pb.durationMs / 60_000L).toInt() + 1).coerceIn(2, 12)
                    DialFace(frac, ticks = true, labels = (0..minutes).map { it.toString() }, startAngle = 135f, sweep = 270f, needle = true)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(pb.isPlaying, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "dialState") {
                            Icon(if (it) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = t.accent, modifier = Modifier.size(40.dp))
                        }
                        Text(formatTime(pb.positionMs), color = t.text, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                        Text("min", color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
                // right: info dial
                Box(Modifier.size(small), contentAlignment = Alignment.Center) {
                    DialFace(0f, ticks = false, labels = emptyList(), startAngle = 0f, sweep = 0f)
                    Column(Modifier.fillMaxWidth(0.66f), horizontalAlignment = Alignment.CenterHorizontally) {
                        DialInfoRow(Icons.Rounded.MusicNote, m.title)
                        HLine(t.text.copy(alpha = 0.4f), Modifier.padding(vertical = 4.dp))
                        DialInfoRow(Icons.Rounded.Person, m.artist)
                        HLine(t.text.copy(alpha = 0.4f), Modifier.padding(vertical = 4.dp))
                        DialInfoRow(Icons.Rounded.Album, m.pb.album.ifBlank { "—" })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SlantKey(Icons.Rounded.Shuffle, m.a.toggleShuffle, active = pb.shuffle)
            Spacer(Modifier.width(6.dp))
            SlantKey(repeatIcon(pb.repeatMode), m.a.cycleRepeat, active = pb.repeatMode != Player.REPEAT_MODE_OFF)
            Spacer(Modifier.weight(1f))
            SlantKey(Icons.Rounded.SkipPrevious, m.a.previous)
            Spacer(Modifier.width(6.dp))
            SlantKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay, filled = true)
            Spacer(Modifier.width(6.dp))
            SlantKey(Icons.Rounded.SkipNext, m.a.next)
            Spacer(Modifier.weight(1f))
            Text("-" + formatTime(m.remaining), color = t.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DialInfoRow(icon: ImageVector, text: String) {
    val t = LocalCarTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = t.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Dial: dark face (honeycomb if the theme has it), outer arc, tick ring with numbers, red progress arc. */
@Composable
private fun DialFace(
    fraction: Float,
    ticks: Boolean,
    labels: List<String>,
    startAngle: Float,
    sweep: Float,
    needle: Boolean = false,
) {
    val t = LocalCarTheme.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.headlineSmall.copy(color = t.text, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val outer = size.minDimension / 2f
        // face
        drawCircle(Brush.radialGradient(listOf(t.surfaceHigh, t.bgBottom), c, outer), outer, c)
        if (t.base.pattern == com.ferzazulic.carmusic.ui.theme.BgPattern.HONEYCOMB) {
            val r = outer * 0.055f
            val hexH = kotlin.math.sqrt(3f) * r
            val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, outer * 0.62f)) }
            clipPath(clip) {
                var y = c.y - outer
                var row = 0
                while (y < c.y + outer) {
                    var x = c.x - outer + if (row % 2 == 0) 0f else r * 1.5f
                    while (x < c.x + outer) {
                        val p = Path()
                        for (i in 0..5) {
                            val ang = Math.toRadians((60 * i).toDouble())
                            val px = x + r * cos(ang).toFloat()
                            val py = y + r * sin(ang).toFloat()
                            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        p.close()
                        drawPath(p, t.accent.copy(alpha = 0.35f), style = Stroke(1.2f))
                        x += r * 3f
                    }
                    y += hexH / 2
                    row++
                }
            }
        }
        // bezel
        drawCircle(t.text.copy(alpha = 0.25f), outer - 2f, c, style = Stroke(3f))
        drawCircle(t.text.copy(alpha = 0.08f), outer * 0.62f, c, style = Stroke(2f))
        if (sweep > 0f) {
            val arcR = outer * 0.93f
            val stroke = outer * 0.05f
            val tl = Offset(c.x - arcR, c.y - arcR)
            val sz = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2)
            drawArc(t.text.copy(alpha = 0.12f), startAngle, sweep, false, tl, sz, style = Stroke(stroke))
            if (t.base.glow) drawArc(t.accent.copy(alpha = 0.3f), startAngle, sweep * fraction, false, tl, sz, style = Stroke(stroke * 2.6f))
            drawArc(t.accent, startAngle, sweep * fraction, false, tl, sz, style = Stroke(stroke))
        }
        if (ticks) {
            val n = (labels.size - 1).coerceAtLeast(1) * 5
            for (i in 0..n) {
                val a = Math.toRadians((startAngle + sweep * i / n).toDouble())
                val major = i % 5 == 0
                val r1 = outer * 0.66f
                val r2 = r1 + if (major) outer * 0.07f else outer * 0.035f
                val lit = i.toFloat() / n <= fraction
                drawLine(
                    if (lit) t.accent else t.text.copy(alpha = 0.7f),
                    Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                    Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                    if (major) 4f else 2f,
                )
            }
            labels.forEachIndexed { i, label ->
                val a = Math.toRadians((startAngle + sweep * i / (labels.size - 1).coerceAtLeast(1)).toDouble())
                val r = outer * 0.81f
                val layout = measurer.measure(label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        c.x + r * cos(a).toFloat() - layout.size.width / 2f,
                        c.y + r * sin(a).toFloat() - layout.size.height / 2f,
                    ),
                )
            }
        }
        if (needle) {
            val a = Math.toRadians((startAngle + sweep * fraction).toDouble())
            val r1 = outer * 0.6f
            val r2 = outer * 0.97f
            drawLine(
                Color.White,
                Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                6f, cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun SlantKey(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, filled: Boolean = false) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    val shape = slantShape(12.dp)
    Box(
        Modifier.size(width = if (filled) 96.dp else 72.dp, height = 56.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(if (filled) t.accent else t.surface)
            .border(1.dp, if (active || filled) t.accent else t.outline, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(icon, transitionSpec = { scaleIn(tween(t.duration(200))) togetherWith scaleOut(tween(t.duration(150))) }, label = "slant") {
            Icon(it, null, tint = if (filled) t.onAccent else if (active) t.accent else t.text, modifier = Modifier.size(30.dp))
        }
    }
}
