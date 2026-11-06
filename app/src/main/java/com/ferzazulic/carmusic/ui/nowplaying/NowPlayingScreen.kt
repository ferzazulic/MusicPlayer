@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ferzazulic.carmusic.ui.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.playback.PlayerHub
import com.ferzazulic.carmusic.settings.AppSettings
import com.ferzazulic.carmusic.ui.PlaybackUi
import com.ferzazulic.carmusic.ui.components.ArtworkImage
import com.ferzazulic.carmusic.ui.components.CarIconButton
import com.ferzazulic.carmusic.ui.components.CarProgressBar
import com.ferzazulic.carmusic.ui.components.Clock
import com.ferzazulic.carmusic.ui.components.PlayPauseButton
import com.ferzazulic.carmusic.ui.components.SectionLabel
import com.ferzazulic.carmusic.ui.components.SpectrumBars
import com.ferzazulic.carmusic.ui.components.formatTime
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import kotlinx.coroutines.delay

class NowPlayingActions(
    val togglePlay: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seekTo: (Long) -> Unit,
    val seekBy: (Long) -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeat: () -> Unit,
    val playIndex: (Int) -> Unit,
)

@Composable
fun NowPlayingScreen(pb: PlaybackUi, song: Song?, settings: AppSettings, actions: NowPlayingActions) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
        val w = maxWidth
        val h = maxHeight
        val landscape = w > h
        if (landscape) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                val artSize = minOf(h * 0.9f, w * 0.42f)
                AnimatedArtwork(pb, song, artSize)
                Spacer(Modifier.width(36.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    InfoPanel(pb, song, settings, actions)
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedArtwork(pb, song, minOf(w * 0.85f, h * 0.45f))
                Spacer(Modifier.height(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                    InfoPanel(pb, song, settings, actions)
                }
            }
        }
    }
}

@Composable
private fun AnimatedArtwork(pb: PlaybackUi, song: Song?, size: Dp) {
    val t = LocalCarTheme.current
    val scale by animateFloatAsState(
        if (pb.isPlaying || !t.animate) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "artScale",
    )
    val elevation by animateDpAsState(if (pb.isPlaying) 28.dp else 8.dp, tween(600), label = "elev")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = song,
            contentKey = { it?.id },
            transitionSpec = {
                val dir = pb.direction
                val spec: ContentTransform = if (!t.animate) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (slideInHorizontally(tween(t.duration(520))) { w -> dir * w / 2 } +
                        fadeIn(tween(t.duration(420))) +
                        scaleIn(tween(t.duration(520)), initialScale = 0.82f)) togetherWith
                        (slideOutHorizontally(tween(t.duration(520))) { w -> -dir * w / 2 } +
                            fadeOut(tween(t.duration(300))) +
                            scaleOut(tween(t.duration(520)), targetScale = 0.82f))
                }
                spec
            },
            label = "artwork",
        ) { s ->
            ArtworkImage(
                s,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        rotationY = (1f - scale) * 40f
                    }
                    .shadow(elevation, RoundedCornerShape(t.corner), ambientColor = t.accent, spotColor = t.accent),
                size = 720,
            )
        }
    }
}

@Composable
private fun ColumnScope.InfoPanel(pb: PlaybackUi, song: Song?, settings: AppSettings, actions: NowPlayingActions) {
    val t = LocalCarTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(if (pb.count > 0) "Svira sada · ${pb.index + 1}/${pb.count}" else "Svira sada")
        Spacer(Modifier.weight(1f))
        SleepTimerButton()
        if (settings.showClock) {
            Spacer(Modifier.width(12.dp))
            Clock()
        }
    }

    Column {
        AnimatedContent(
            targetState = pb.title.ifBlank { "Ništa ne svira" },
            transitionSpec = {
                (slideInVertically(tween(t.duration(450))) { it / 2 } + fadeIn(tween(t.duration(450)))) togetherWith
                    (slideOutVertically(tween(t.duration(300))) { -it / 2 } + fadeOut(tween(t.duration(250))))
            },
            label = "title",
        ) { title ->
            Text(
                title,
                style = if (settings.largeControls) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = t.text,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500),
            )
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = pb.artist to pb.album,
            transitionSpec = {
                (fadeIn(tween(t.duration(500), delayMillis = t.duration(120))) +
                    slideInVertically(tween(t.duration(500), delayMillis = t.duration(120))) { it / 3 }) togetherWith
                    fadeOut(tween(t.duration(200)))
            },
            label = "artist",
        ) { (artist, album) ->
            Column {
                Text(artist.ifBlank { "Odaberi pjesmu iz biblioteke" }, style = MaterialTheme.typography.headlineSmall, color = t.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (album.isNotBlank()) {
                    Text(
                        album + (song?.year?.takeIf { it > 0 }?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium, color = t.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    Column {
        CarProgressBar(pb.positionMs, pb.durationMs, actions.seekTo)
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text("-" + formatTime((pb.durationMs - pb.positionMs).coerceAtLeast(0)), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        val big = settings.largeControls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CarIconButton(Icons.Rounded.Shuffle, "Nasumično", actions.toggleShuffle, active = pb.shuffle, size = if (big) 60.dp else 52.dp)
            CarIconButton(Icons.Rounded.Replay10, "Nazad 10s", { actions.seekBy(-10_000) }, size = if (big) 60.dp else 52.dp)
            CarIconButton(Icons.Rounded.SkipPrevious, "Prethodna", actions.previous, size = if (big) 80.dp else 64.dp)
            PlayPauseButton(pb.isPlaying, actions.togglePlay, size = if (big) 104.dp else 84.dp)
            CarIconButton(Icons.Rounded.SkipNext, "Sljedeća", actions.next, size = if (big) 80.dp else 64.dp)
            CarIconButton(Icons.Rounded.Forward10, "Naprijed 10s", { actions.seekBy(10_000) }, size = if (big) 60.dp else 52.dp)
            CarIconButton(
                if (pb.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Ponavljanje", actions.cycleRepeat,
                active = pb.repeatMode != Player.REPEAT_MODE_OFF, size = if (big) 60.dp else 52.dp,
            )
        }
        if (settings.showVisualizer) {
            Spacer(Modifier.height(14.dp))
            SpectrumBars(pb.isPlaying, Modifier.fillMaxWidth().height(44.dp), bars = 40)
        }
    }
}

@Composable
private fun SleepTimerButton() {
    val t = LocalCarTheme.current
    val sleepAt by PlayerHub.sleepAt.collectAsState()
    var open by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepAt) {
        while (sleepAt != null) { now = System.currentTimeMillis(); delay(1000) }
    }
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            sleepAt?.let {
                Text(formatTime(it - now), color = t.accent, style = MaterialTheme.typography.labelLarge)
            }
            CarIconButton(Icons.Rounded.Bedtime, "Tajmer za spavanje", { open = true }, size = 48.dp, active = sleepAt != null)
        }
        DropdownMenu(open, { open = false }) {
            listOf<Int?>(null, 15, 30, 45, 60, 90).forEach { m ->
                DropdownMenuItem(
                    text = { Text(m?.let { "$it min" } ?: "Isključi tajmer", color = if (m == null) Color.Unspecified else t.text) },
                    onClick = { PlayerHub.setSleepTimer(m); open = false },
                )
            }
        }
    }
}
