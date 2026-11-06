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
import com.ferzazulic.carmusic.ui.layouts.AppShell
import com.ferzazulic.carmusic.ui.layouts.FactoryNowPlaying
import com.ferzazulic.carmusic.ui.layouts.PlayerModel
import androidx.compose.runtime.remember
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

    val layout = t.base.layout
    val queue = remember(pb.queueIds, library) { pb.queueIds.map { library.song(it) } }
    val actions = NowPlayingActions(
        vm::togglePlay, { vm.next() }, { vm.previous() }, vm::seekTo, { vm.seekBy(it) },
        { vm.toggleShuffle() }, { vm.cycleRepeat() }, { vm.playQueueIndex(it) },
    )

    CarBackground(glow = glow, playing = pb.isPlaying) {
        // Changing to a theme with another layout cross-fades the whole frame.
        AnimatedContent(
            targetState = layout,
            transitionSpec = { fadeIn(tween(t.duration(600))) togetherWith fadeOut(tween(t.duration(400))) },
            label = "layout",
        ) { currentLayout ->
            AppShell(
                layout = currentLayout,
                screen = screen,
                onSelect = { screen = it },
                shuffle = pb.shuffle,
                onShuffle = { vm.toggleShuffle() },
                mini = {
                    AnimatedVisibility(
                        visible = screen != Screen.NOW_PLAYING && pb.songId != null,
                        enter = slideInVertically(tween(t.duration(400))) { it } + fadeIn(),
                        exit = slideOutVertically(tween(t.duration(300))) { it } + fadeOut(),
                    ) {
                        MiniPlayer(pb, song, onOpen = { screen = Screen.NOW_PLAYING }, onToggle = vm::togglePlay, onNext = { vm.next() })
                    }
                },
            ) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val down = targetState.ordinal > initialState.ordinal
                        (slideInVertically(tween(t.duration(450), easing = FastOutSlowInEasing)) { if (down) it / 8 else -it / 8 } +
                            fadeIn(tween(t.duration(400))) + scaleIn(tween(t.duration(450)), initialScale = 0.97f)) togetherWith
                            (fadeOut(tween(t.duration(220))) + scaleOut(tween(t.duration(300)), targetScale = 1.02f))
                    },
                    label = "screen",
                    modifier = Modifier.fillMaxSize(),
                ) { s ->
                    when (s) {
                        Screen.NOW_PLAYING -> FactoryNowPlaying(currentLayout, PlayerModel(pb, song, queue, settings, actions))
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
