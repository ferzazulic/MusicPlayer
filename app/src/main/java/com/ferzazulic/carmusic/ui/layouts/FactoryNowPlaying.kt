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
import androidx.compose.ui.text.font.FontWeight
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
            PlayerLayout.MIB -> MibNowPlaying(m)
            PlayerLayout.RNS -> RnsNowPlaying(m)
            PlayerLayout.MMI -> MmiNowPlaying(m)
            PlayerLayout.COCKPIT -> CockpitNowPlaying(m)
            PlayerLayout.GAUGE -> GaugeNowPlaying(m)
        }
    }
}

/**
 * Cover that slides in from the direction of travel through the queue. Swipe left/right on it to
 * change track; the heart in the corner adds the song to favourites.
 */
@Composable
private fun AnimatedCover(m: PlayerModel, modifier: Modifier, shape: Shape? = null, artSize: Int = 720) {
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
private fun repeatIcon(mode: Int) = if (mode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val t = LocalCarTheme.current
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed && t.animate) 0.9f else 1f, spring(dampingRatio = 0.5f), label = "press")
    return s
}

// =============================================================================================== MIB

@Composable
private fun MibNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(
                Modifier.fillMaxHeight().aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(t.corner))
                    .border(1.dp, t.outline, RoundedCornerShape(t.corner))
            ) { AnimatedCover(m, Modifier.fillMaxSize()) }
            Spacer(Modifier.width(32.dp))
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(
                    "Memorija · ${if (pb.count > 0) "${pb.index + 1} / ${pb.count}" else "—"}",
                    color = t.textMuted, style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                MarqueeTitle(m.title, MaterialTheme.typography.headlineMedium, t.text)
                Spacer(Modifier.height(12.dp))
                MibInfoLine(Icons.Rounded.Person, m.artist)
                MibInfoLine(Icons.Rounded.Album, pb.album.ifBlank { "—" })
                Spacer(Modifier.weight(1f))
                CarProgressBar(pb.positionMs, pb.durationMs, m.a.seekTo)
                Row {
                    Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(pb.durationMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // MIB flat segmented control bar
        val shape = RoundedCornerShape(t.corner)
        Row(
            Modifier.fillMaxWidth().height(if (m.settings.largeControls) 78.dp else 64.dp)
                .clip(shape).background(t.surface.copy(alpha = 0.9f)).border(1.dp, t.outline, shape)
        ) {
            MibCell(Icons.Rounded.Shuffle, pb.shuffle, m.a.toggleShuffle)
            VLine(t.outline, Modifier.padding(vertical = 12.dp))
            MibCell(Icons.Rounded.SkipPrevious, false, m.a.previous)
            VLine(t.outline, Modifier.padding(vertical = 12.dp))
            MibCell(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, true, m.a.togglePlay, filled = true)
            VLine(t.outline, Modifier.padding(vertical = 12.dp))
            MibCell(Icons.Rounded.SkipNext, false, m.a.next)
            VLine(t.outline, Modifier.padding(vertical = 12.dp))
            MibCell(repeatIcon(pb.repeatMode), pb.repeatMode != Player.REPEAT_MODE_OFF, m.a.cycleRepeat)
        }
    }
}

@Composable
private fun MibInfoLine(icon: ImageVector, text: String) {
    val t = LocalCarTheme.current
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = t.textMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        AnimatedContent(text, transitionSpec = { fadeIn(tween(t.duration(400))) togetherWith fadeOut(tween(t.duration(200))) }, label = "mibLine") {
            Text(it, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RowScope.MibCell(icon: ImageVector, active: Boolean, onClick: () -> Unit, filled: Boolean = false) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(
        when {
            filled && pressed -> t.accent.copy(alpha = 0.7f)
            filled -> t.accent
            pressed -> t.surfaceHigh
            else -> Color.Transparent
        }, tween(120), label = "mibCell",
    )
    Box(
        Modifier.weight(if (filled) 1.4f else 1f).fillMaxHeight().background(bg)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(icon, transitionSpec = { scaleIn(tween(t.duration(220))) togetherWith scaleOut(tween(t.duration(160))) }, label = "mibIcon") {
            Icon(it, null, tint = if (filled) t.onAccent else if (active) t.accent else t.text, modifier = Modifier.size(36.dp))
        }
    }
}

// =============================================================================================== RNS

@Composable
private fun RnsNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Column(Modifier.fillMaxHeight().aspectRatio(0.88f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .border(2.dp, t.outline, RoundedCornerShape(t.corner))
                    .padding(4.dp)
            ) { AnimatedCover(m, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(8.dp))
            Text(
                if (pb.count > 0) "Pjesma ${pb.index + 1} od ${pb.count}" else "—",
                color = t.textMuted, style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            RnsLine(Icons.Rounded.MusicNote, m.title, highlight = true)
            RnsLine(Icons.Rounded.Person, m.artist)
            RnsLine(Icons.Rounded.Album, pb.album.ifBlank { "—" })
            RnsLine(Icons.Rounded.Timer, "${formatTime(pb.positionMs)}  /  ${formatTime(pb.durationMs)}")
            Spacer(Modifier.weight(1f))
            CarProgressBar(pb.positionMs, pb.durationMs, m.a.seekTo)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(if (m.settings.largeControls) 70.dp else 58.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), active = pb.shuffle, onClick = m.a.toggleShuffle) {
                    Text("MIX", color = if (pb.shuffle) t.accent else t.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), onClick = m.a.previous) { KeyIcon(Icons.Rounded.SkipPrevious, t.text) }
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), onClick = { m.a.seekBy(-10_000) }) { KeyIcon(Icons.Rounded.FastRewind, t.text) }
                GlossyKey(Modifier.weight(1.5f).fillMaxHeight(), active = true, onClick = m.a.togglePlay) {
                    AnimatedContent(pb.isPlaying, transitionSpec = { scaleIn(tween(t.duration(200))) togetherWith scaleOut(tween(t.duration(150))) }, label = "rnsPlay") {
                        KeyIcon(if (it) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, t.accent, 40.dp)
                    }
                }
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), onClick = { m.a.seekBy(10_000) }) { KeyIcon(Icons.Rounded.FastForward, t.text) }
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), onClick = m.a.next) { KeyIcon(Icons.Rounded.SkipNext, t.text) }
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), active = pb.repeatMode != Player.REPEAT_MODE_OFF, onClick = m.a.cycleRepeat) {
                    Text(
                        when (pb.repeatMode) { Player.REPEAT_MODE_ONE -> "RPT 1"; Player.REPEAT_MODE_ALL -> "RPT"; else -> "RPT" },
                        color = if (pb.repeatMode != Player.REPEAT_MODE_OFF) t.accent else t.text,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RnsLine(icon: ImageVector, text: String, highlight: Boolean = false) {
    val t = LocalCarTheme.current
    Column {
        Row(Modifier.fillMaxWidth().height(if (highlight) 62.dp else 50.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = t.accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            if (highlight) {
                MarqueeTitle(text, MaterialTheme.typography.headlineSmall, t.text, modifier = Modifier.weight(1f))
            } else {
                AnimatedContent(text, transitionSpec = { fadeIn(tween(t.duration(350))) togetherWith fadeOut(tween(t.duration(200))) }, label = "rnsLine", modifier = Modifier.weight(1f)) {
                    Text(it, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        HLine(t.outline.copy(alpha = 0.7f))
    }
}

// =============================================================================================== MMI

@Composable
private fun MmiNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Row(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
        // Track list window around the current song, like the MMI media list
        Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            AnimatedContent(
                targetState = pb.index,
                transitionSpec = {
                    val d = if (targetState >= initialState) 1 else -1
                    (slideInVertically(tween(t.duration(380))) { it / 7 * d } + fadeIn(tween(t.duration(300)))) togetherWith
                        (slideOutVertically(tween(t.duration(380))) { -it / 7 * d } + fadeOut(tween(t.duration(200))))
                },
                label = "mmiList",
            ) { current ->
                Column {
                    for (offset in -3..3) {
                        val i = current + offset
                        val s = m.queue.getOrNull(i)
                        if (i < 0 || i >= m.queue.size) {
                            Spacer(Modifier.height(50.dp))
                        } else {
                            MmiTrackRow(i + 1, s?.title ?: "—", s?.artist.orEmpty(), offset == 0, pb.isPlaying) { m.a.playIndex(i) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(28.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.weight(1f).aspectRatio(1f).border(1.dp, t.text.copy(alpha = 0.35f))
            ) { AnimatedCover(m, Modifier.fillMaxSize().padding(1.dp), shape = RoundedCornerShape(0.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(m.artist, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pb.album, color = t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CarProgressBar(pb.positionMs, pb.durationMs, m.a.seekTo)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                MmiKey(Icons.Rounded.SkipPrevious, m.a.previous)
                Spacer(Modifier.width(8.dp))
                MmiKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay, accent = true)
                Spacer(Modifier.width(8.dp))
                MmiKey(Icons.Rounded.SkipNext, m.a.next)
                Spacer(Modifier.weight(1f))
                Text("-" + formatTime(m.remaining), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MmiTrackRow(number: Int, title: String, artist: String, current: Boolean, playing: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .then(
                if (current) Modifier.background(Brush.horizontalGradient(listOf(t.accent, t.accent.copy(alpha = 0.55f), t.accent.copy(alpha = 0.05f))))
                else Modifier
            )
            .clickable(onClick = onClick)
            .drawBehind {
                if (!current) drawLine(t.outline.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1f)
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(number), color = if (current) Color.White else t.textMuted, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(44.dp))
            Text(
                title + if (current && artist.isNotBlank()) "  ·  $artist" else "",
                color = if (current) Color.White else t.text.copy(alpha = 0.75f),
                style = if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (current) SpectrumBars(playing, Modifier.size(26.dp, 20.dp), bars = 4)
        }
    }
}

@Composable
private fun MmiKey(icon: ImageVector, onClick: () -> Unit, accent: Boolean = false) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    Box(
        Modifier.size(width = 64.dp, height = 52.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .border(1.dp, if (accent) t.accent else t.text.copy(alpha = 0.35f))
            .background(if (accent) t.accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (accent) t.accent else t.text, modifier = Modifier.size(30.dp)) }
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
private fun RoundKey(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, size: Dp = 60.dp) {
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

// ============================================================================================= GAUGE

private const val START_ANGLE = 135f
private const val SWEEP = 270f

@Composable
private fun GaugeNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Digital readout, like the speed figure in a sport cockpit
        Column(Modifier.weight(0.75f), horizontalAlignment = Alignment.End) {
            Text(formatTime(pb.positionMs), color = t.text, fontSize = 60.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displaySmall)
            GaugeCaption("Vrijeme")
            Spacer(Modifier.height(14.dp))
            Text("-" + formatTime(m.remaining), color = t.textMuted, style = MaterialTheme.typography.headlineSmall)
            GaugeCaption("Preostalo")
            Spacer(Modifier.height(14.dp))
            Text(if (pb.count > 0) "${pb.index + 1}/${pb.count}" else "—", color = t.text, style = MaterialTheme.typography.headlineSmall)
            GaugeCaption("Pjesma")
        }
        Spacer(Modifier.width(24.dp))
        BoxWithConstraints(Modifier.fillMaxHeight().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Gauge(m)
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            MarqueeTitle(m.title.uppercase(), MaterialTheme.typography.headlineMedium, t.text)
            Text(m.artist, color = t.accent, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pb.album, color = t.textMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SlantKey(Icons.Rounded.Shuffle, m.a.toggleShuffle, active = pb.shuffle)
                SlantKey(Icons.Rounded.SkipPrevious, m.a.previous)
                SlantKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay, filled = true)
                SlantKey(Icons.Rounded.SkipNext, m.a.next)
                SlantKey(repeatIcon(pb.repeatMode), m.a.cycleRepeat, active = pb.repeatMode != Player.REPEAT_MODE_OFF)
            }
            if (m.settings.showVisualizer) {
                Spacer(Modifier.height(18.dp))
                SpectrumBars(pb.isPlaying, Modifier.fillMaxWidth().height(36.dp), bars = 24)
            }
        }
    }
}

@Composable
private fun GaugeCaption(text: String) {
    val t = LocalCarTheme.current
    Text(text.uppercase(), color = t.accent, style = MaterialTheme.typography.labelMedium, letterSpacing = 3.sp)
}

/** Tachometer-style ring: tick scale, glowing progress arc, needle tip; tap the ring to seek. */
@Composable
private fun BoxWithConstraintsScope.Gauge(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    val animated by animateFloatAsState(m.fraction, tween(if (t.animate) 400 else 0), label = "gauge")
    val sweepIn = remember { Animatable(if (t.animate) 0f else 1f) }
    LaunchedEffect(Unit) { sweepIn.animateTo(1f, tween(t.duration(1100), easing = FastOutSlowInEasing)) }
    val frac = animated * sweepIn.value
    val d = minOf(maxWidth, maxHeight)
    Canvas(
        Modifier.size(d).pointerInput(pb.durationMs) {
            detectTapGestures { o ->
                val c = Offset(size.width / 2f, size.height / 2f)
                var ang = Math.toDegrees(atan2((o.y - c.y).toDouble(), (o.x - c.x).toDouble())).toFloat()
                ang = (ang - START_ANGLE + 720f) % 360f
                if (ang <= SWEEP && pb.durationMs > 0) m.a.seekTo((ang / SWEEP * pb.durationMs).toLong())
            }
        }
    ) {
        val stroke = size.minDimension * 0.045f
        val r = size.minDimension / 2 - stroke * 2.4f
        val c = center
        val topLeft = Offset(c.x - r, c.y - r)
        val arcSize = Size(r * 2, r * 2)
        // ticks
        for (i in 0..40) {
            val a = Math.toRadians((START_ANGLE + SWEEP * i / 40f).toDouble())
            val major = i % 5 == 0
            val r1 = r + stroke * 0.9f
            val r2 = r1 + if (major) stroke * 1.2f else stroke * 0.6f
            val lit = i / 40f <= frac
            drawLine(
                if (lit) t.accent else t.text.copy(alpha = if (major) 0.55f else 0.25f),
                Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                if (major) 3f else 1.5f,
            )
        }
        drawArc(t.text.copy(alpha = 0.12f), START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        if (t.base.glow) drawArc(t.accent.copy(alpha = 0.25f), START_ANGLE, SWEEP * frac, false, topLeft, arcSize, style = Stroke(stroke * 2.4f))
        drawArc(
            Brush.sweepGradient(listOf(t.accent.copy(alpha = 0.5f), t.accent, t.accent), c),
            START_ANGLE, SWEEP * frac, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
        )
        val tip = Math.toRadians((START_ANGLE + SWEEP * frac).toDouble())
        val tipO = Offset(c.x + r * cos(tip).toFloat(), c.y + r * sin(tip).toFloat())
        drawCircle(t.accent.copy(alpha = 0.35f), stroke * 1.4f, tipO)
        drawCircle(Color.White, stroke * 0.55f, tipO)
    }
    Box(Modifier.size(d * 0.6f).clip(CircleShape).border(2.dp, t.outline, CircleShape)) {
        AnimatedCover(m, Modifier.fillMaxSize(), shape = CircleShape)
    }
}

@Composable
private fun SlantKey(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, filled: Boolean = false) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    val shape = slantShape(12.dp)
    Box(
        Modifier.size(width = if (filled) 96.dp else 72.dp, height = 60.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(if (filled) t.accent else t.surface)
            .border(1.dp, if (active || filled) t.accent else t.outline, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(icon, transitionSpec = { scaleIn(tween(t.duration(200))) togetherWith scaleOut(tween(t.duration(150))) }, label = "slant") {
            Icon(it, null, tint = if (filled) t.onAccent else if (active) t.accent else t.text, modifier = Modifier.size(32.dp))
        }
    }
}
