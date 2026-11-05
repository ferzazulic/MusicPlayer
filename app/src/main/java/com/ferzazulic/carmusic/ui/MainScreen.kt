package com.ferzazulic.carmusic.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.components.ArtworkImage
import com.ferzazulic.carmusic.ui.components.CarBackground
import com.ferzazulic.carmusic.ui.components.CarIconButton
import com.ferzazulic.carmusic.ui.components.PlayPauseButton
import com.ferzazulic.carmusic.ui.components.rememberArtwork
import com.ferzazulic.carmusic.ui.library.LibraryScreen
import com.ferzazulic.carmusic.ui.nowplaying.NowPlayingActions
import com.ferzazulic.carmusic.ui.nowplaying.NowPlayingScreen
import com.ferzazulic.carmusic.ui.settings.SettingsActions
import com.ferzazulic.carmusic.ui.settings.SettingsScreen
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

enum class Screen(val label: String, val icon: ImageVector) {
    NOW_PLAYING("Svira", Icons.Rounded.MusicNote),
    LIBRARY("Muzika", Icons.Rounded.LibraryMusic),
    SETTINGS("Postavke", Icons.Rounded.Settings),
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val t = LocalCarTheme.current
    val settings by vm.settings.collectAsState()
    val pb by vm.playback.collectAsState()
    val song by vm.currentSong.collectAsState()
    val library by vm.library.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val cidStatus by vm.cidStatus.collectAsState()
    val cidPreview by vm.cidPreview.collectAsState()
    var screen by rememberSaveable { mutableStateOf(if (pb.songId != null) Screen.NOW_PLAYING else Screen.LIBRARY) }

    val art = rememberArtwork(song, 512).value
    val glow = if (settings.artworkBackground) art?.let { Color(it.vibrant) } else null

    CarBackground(glow = glow, playing = pb.isPlaying) {
        Row(Modifier.fillMaxSize()) {
            NavRail(screen) { screen = it }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val down = targetState.ordinal > initialState.ordinal
                        (slideInVertically(tween(t.duration(450), easing = FastOutSlowInEasing)) { if (down) it / 8 else -it / 8 } +
                            fadeIn(tween(t.duration(400))) + scaleIn(tween(t.duration(450)), initialScale = 0.97f)) togetherWith
                            (fadeOut(tween(t.duration(220))) + scaleOut(tween(t.duration(300)), targetScale = 1.02f))
                    },
                    label = "screen",
                    modifier = Modifier.weight(1f),
                ) { s ->
                    when (s) {
                        Screen.NOW_PLAYING -> NowPlayingScreen(
                            pb, song, settings,
                            NowPlayingActions(vm::togglePlay, { vm.next() }, { vm.previous() }, vm::seekTo, { vm.seekBy(it) }, { vm.toggleShuffle() }, { vm.cycleRepeat() }),
                        )
                        Screen.LIBRARY -> LibraryScreen(
                            library, pb, settings.sortOrder, scanning,
                            onPlay = { list, i, shuffle -> vm.play(list, i, shuffle); if (!shuffle || i < 0) screen = Screen.NOW_PLAYING },
                            onPlayQueueIndex = { vm.playQueueIndex(it) },
                            onRescan = vm::rescan,
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            settings, library, cidStatus, cidPreview,
                            SettingsActions(vm::updateSettings, vm::rescan, vm::sendCidTest),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = screen != Screen.NOW_PLAYING && pb.songId != null,
                    enter = slideInVertically(tween(t.duration(400))) { it } + fadeIn(),
                    exit = slideOutVertically(tween(t.duration(300))) { it } + fadeOut(),
                ) {
                    MiniPlayer(pb, song, onOpen = { screen = Screen.NOW_PLAYING }, onToggle = vm::togglePlay, onNext = { vm.next() })
                }
            }
        }
    }
}

@Composable
private fun NavRail(current: Screen, onSelect: (Screen) -> Unit) {
    val t = LocalCarTheme.current
    Column(
        Modifier
            .width(104.dp)
            .fillMaxHeight()
            .background(t.bgBottom.copy(alpha = 0.55f))
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Screen.entries.forEach { s ->
            val active = s == current
            val sel by animateFloatAsState(if (active) 1f else 0f, spring(dampingRatio = 0.6f), label = "rail")
            Column(
                Modifier
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(t.corner + 6.dp))
                    .clickable { onSelect(s) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(width = 64.dp, height = 44.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { scaleX = 0.6f + 0.4f * sel; alpha = sel.coerceIn(0f, 1f) }
                            .clip(RoundedCornerShape(if (t.corner >= 8.dp) 22.dp else t.corner))
                            .background(t.accent.copy(alpha = 0.22f)),
                    )
                    Icon(s.icon, s.label, tint = if (active) t.accent else t.textMuted, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(t.label(s.label), color = if (active) t.text else t.textMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MiniPlayer(pb: PlaybackUi, song: com.ferzazulic.carmusic.data.Song?, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    val t = LocalCarTheme.current
    val progress = if (pb.durationMs > 0) pb.positionMs.toFloat() / pb.durationMs else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(t.corner + 6.dp))
            .background(t.surfaceHigh.copy(alpha = 0.95f))
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(song, Modifier.size(56.dp), size = 128)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pb.title, color = t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pb.artist, color = t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayPauseButton(pb.isPlaying, onToggle, size = 56.dp)
            Spacer(Modifier.width(6.dp))
            CarIconButton(Icons.Rounded.SkipNext, "Sljedeća", onNext, size = 56.dp)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(t.text.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(t.accent))
        }
    }
}
