package com.ferzazulic.carmusic.ui.layouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.ferzazulic.carmusic.ui.Screen
import com.ferzazulic.carmusic.ui.components.Clock
import com.ferzazulic.carmusic.ui.components.formatTime
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.PlayerLayout

// ======================================================================================== RCD / RNS 510
// Light screen; row of red-framed source tabs on top; "ARTIST - Title" line; thin silver progress with
// elapsed time; centred transport keys; four large soft keys along the bottom.

@Composable
private fun RcdKey(
    modifier: Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(4.dp)
    val body = when {
        active -> listOf(Color(0xFF9B1217), Color(0xFF5E070B))
        pressed -> listOf(Color(0xFFCFCFCF), Color(0xFFA9A9A9))
        else -> listOf(Color(0xFFFFFFFF), Color(0xFFE2E2E2), Color(0xFFC6C6C6))
    }
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(body))
            .border(2.dp, t.accent.copy(alpha = 0.9f), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RcdShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Screen.entries.forEach { s ->
                val active = s == screen
                RcdKey(Modifier.weight(1f).fillMaxHeight(), active = active, onClick = { onSelect(s) }) {
                    Text(
                        s.factoryTitle(PlayerLayout.RCD), color = if (active) Color.White else Color(0xFF111111),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        body()
    }
}

@Composable
fun RcdNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.weight(0.6f))
        MarqueeTitle(
            "${m.artist.uppercase()} - ${m.title}",
            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), t.text,
        )
        if (pb.album.isNotBlank()) Text(pb.album, color = t.textMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Spacer(Modifier.weight(0.4f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(pb.positionMs), color = t.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(76.dp))
            Box(Modifier.weight(0.55f)) {
                FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.CHROME, fill = Color(0xFFE6E6E6), fillEnd = Color(0xFF9A9A9A), track = Color(0xFFBDBDBD))
            }
            Spacer(Modifier.weight(0.45f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.Center) {
            RcdKey(Modifier.width(110.dp).fillMaxHeight(), onClick = m.a.previous) { KeyIcon(Icons.Rounded.SkipPrevious, Color(0xFF222222), 36.dp) }
            Spacer(Modifier.width(6.dp))
            RcdKey(Modifier.width(220.dp).fillMaxHeight(), onClick = m.a.togglePlay) {
                AnimatedContent(pb.isPlaying, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "rcdPlay") {
                    KeyIcon(if (it) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, Color(0xFF222222), 38.dp)
                }
            }
            Spacer(Modifier.width(6.dp))
            RcdKey(Modifier.width(110.dp).fillMaxHeight(), onClick = m.a.next) { KeyIcon(Icons.Rounded.SkipNext, Color(0xFF222222), 36.dp) }
        }
        Spacer(Modifier.weight(0.5f))
        Row(Modifier.fillMaxWidth().height(70.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RcdSoftKey("Fav", m.isFavorite, m.a.toggleFavorite)
            RcdSoftKey("Mix", pb.shuffle, m.a.toggleShuffle)
            RcdSoftKey(if (pb.repeatMode == Player.REPEAT_MODE_ONE) "Repeat 1" else "Repeat", pb.repeatMode != Player.REPEAT_MODE_OFF, m.a.cycleRepeat)
            RcdSoftKey("Selection", false) { m.navigate(Screen.LIBRARY) }
        }
    }
}

@Composable
private fun RowScope.RcdSoftKey(label: String, on: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    RcdKey(Modifier.weight(1f).fillMaxHeight(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(on, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                Text("▶ ", color = t.accent, style = MaterialTheme.typography.titleMedium)
            }
            Text(label, color = Color(0xFF111111), style = MaterialTheme.typography.titleLarge)
        }
    }
}

// =========================================================================================== MIB3
// Black screen; slim text menu down the left (MENU / HOME / … / volume); status bar with centred
// clock; right-aligned artist / album / title next to a square cover; dotted progress line;
// small transport keys; glowing blue divider and an icon bar along the bottom.

@Composable
fun Mib3Shell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    val volume = rememberVolumeControl()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(96.dp).fillMaxHeight().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Mib3MenuItem("MENU", screen == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
            Mib3MenuItem("HOME", screen == Screen.NOW_PLAYING) { onSelect(Screen.NOW_PLAYING) }
            Mib3MenuItem("LISTA", screen == Screen.LIBRARY) { onSelect(Screen.LIBRARY) }
            Spacer(Modifier.weight(1f))
            Mib3MenuIcon(Icons.Rounded.Add) { volume(1) }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = t.textMuted, modifier = Modifier.size(22.dp))
            Mib3MenuIcon(Icons.Rounded.Remove) { volume(-1) }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp)) {
                Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = t.text, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CarMusic", color = t.text, style = MaterialTheme.typography.labelLarge)
                }
                Clock(Modifier.align(Alignment.Center))
            }
            body()
        }
    }
}

@Composable
private fun Mib3MenuItem(label: String, active: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val line by animateColorAsState(if (active) t.accent else t.text.copy(alpha = 0.55f), tween(t.duration(300)), label = "mib3")
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = t.text, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.62f).height(2.dp).background(line))
    }
}

@Composable
private fun Mib3MenuIcon(icon: ImageVector, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Box(Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = t.text, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun Mib3NowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(m.artist, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                Text(pb.album, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                Spacer(Modifier.height(4.dp))
                AnimatedContent(m.title, transitionSpec = { (fadeIn(tween(t.duration(400))) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(t.duration(200))) }, label = "mib3Title") {
                    Text(it, color = t.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.width(28.dp))
            Box(Modifier.fillMaxHeight(0.8f).aspectRatio(1f)) { AnimatedCover(m, Modifier.fillMaxSize()) }
            Spacer(Modifier.width(12.dp))
        }
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(pb.positionMs), color = t.text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
            Box(Modifier.weight(1f)) { FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.DOTTED, track = t.text.copy(alpha = 0.55f)) }
            Text("-" + formatTime(m.remaining), color = t.text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        }
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 24.dp)) {
            Row(Modifier.align(Alignment.CenterStart)) {
                Mib3Key(repeatIcon(pb.repeatMode), pb.repeatMode != Player.REPEAT_MODE_OFF, m.a.cycleRepeat)
                Mib3Key(Icons.Rounded.Shuffle, pb.shuffle, m.a.toggleShuffle)
            }
            Row(Modifier.align(Alignment.Center)) {
                Mib3Key(Icons.Rounded.ChevronLeft, false, m.a.previous, 40.dp)
                Spacer(Modifier.width(18.dp))
                Mib3Key(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, false, m.a.togglePlay, 40.dp)
                Spacer(Modifier.width(18.dp))
                Mib3Key(Icons.Rounded.ChevronRight, false, m.a.next, 40.dp)
            }
        }
        // glowing blue divider
        Box(
            Modifier.fillMaxWidth().height(10.dp).drawBehind {
                val cy = size.height / 2
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, t.accent.copy(alpha = 0.35f), t.accent.copy(alpha = 0.35f), Color.Transparent)),
                    Offset(0f, cy), Offset(size.width, cy), 8f,
                )
                drawLine(
                    Brush.horizontalGradient(listOf(t.accent.copy(alpha = 0.2f), t.accent, t.accent, t.accent.copy(alpha = 0.2f))),
                    Offset(0f, cy), Offset(size.width, cy), 2.5f,
                )
            }
        )
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Mib3Key(Icons.AutoMirrored.Rounded.QueueMusic, false, { m.navigate(Screen.LIBRARY) })
            Spacer(Modifier.weight(1f))
            Mib3Key(Icons.Rounded.LibraryMusic, false, { m.navigate(Screen.LIBRARY) })
            Spacer(Modifier.weight(1f))
            Mib3Key(if (m.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, m.isFavorite, m.a.toggleFavorite)
            Spacer(Modifier.weight(1f))
            Mib3Key(Icons.Rounded.MusicNote, true, { })
            Spacer(Modifier.weight(2f))
            Mib3Key(Icons.Rounded.Settings, false, { m.navigate(Screen.SETTINGS) })
        }
    }
}

@Composable
private fun Mib3Key(icon: ImageVector, active: Boolean, onClick: () -> Unit, size: Dp = 32.dp) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    Box(
        Modifier.size(size + 22.dp).graphicsLayer { scaleX = sc; scaleY = sc }
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (active) t.accent else t.text, modifier = Modifier.size(size))
    }
}
