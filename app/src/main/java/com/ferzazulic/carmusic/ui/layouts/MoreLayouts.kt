package com.ferzazulic.carmusic.ui.layouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
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

@Composable
private fun PlainKey(icon: ImageVector, onClick: () -> Unit, tint: Color, size: Dp = 40.dp, box: Dp = 72.dp) {
    val source = remember { MutableInteractionSource() }
    val sc = pressScale(source)
    Box(
        Modifier.size(box).graphicsLayer { scaleX = sc; scaleY = sc }
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size)) }
}

// ============================================================================================ AMUNDSEN
// Text keys with long underlines left and right of the screen; status bar with the clock in a
// centred notch; big title, artist, album; outlined transport symbols; cover right with repeat /
// shuffle under it; green-yellow progress; hatched function bar at the bottom.

@Composable
fun AmundsenShell(screen: Screen, onSelect: (Screen) -> Unit, m: PlayerModel, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    val volume = rememberVolumeControl()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(118.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            AmundsenSideKey("MEDIA", screen == Screen.NOW_PLAYING, true) { onSelect(Screen.NOW_PLAYING) }
            AmundsenSideKey("LISTA", screen == Screen.LIBRARY, true) { onSelect(Screen.LIBRARY) }
            AmundsenSideKey("MIX", m.pb.shuffle, true, m.a.toggleShuffle)
            AmundsenSideKey("RPT", m.pb.repeatMode != Player.REPEAT_MODE_OFF, true, m.a.cycleRepeat)
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(
                Modifier.fillMaxWidth().height(48.dp).drawBehind {
                    val y = size.height - 2.dp.toPx()
                    val notchW = 150.dp.toPx()
                    val depth = 10.dp.toPx()
                    val cx = size.width / 2
                    val p = Path().apply {
                        moveTo(0f, y - depth)
                        lineTo(cx - notchW / 2 - depth, y - depth)
                        lineTo(cx - notchW / 2, y)
                        lineTo(cx + notchW / 2, y)
                        lineTo(cx + notchW / 2 + depth, y - depth)
                        lineTo(size.width, y - depth)
                    }
                    drawPath(p, t.text.copy(alpha = 0.6f), style = Stroke(1.5.dp.toPx()))
                }
            ) {
                Row(Modifier.align(Alignment.CenterStart).padding(start = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = t.text, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CarMusic", color = t.text, style = MaterialTheme.typography.labelLarge)
                }
                Clock(Modifier.align(Alignment.Center).padding(top = 4.dp))
                Text(
                    if (m.isFavorite) "♥" else "",
                    color = t.accent, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp, bottom = 10.dp),
                )
            }
            body()
        }
        Column(Modifier.width(118.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            AmundsenSideKey("SETUP", screen == Screen.SETTINGS, false) { onSelect(Screen.SETTINGS) }
            AmundsenSideKey("VOL +", false, false) { volume(1) }
            AmundsenSideKey("VOL −", false, false) { volume(-1) }
            AmundsenSideKey("FAV", m.isFavorite, false, m.a.toggleFavorite)
        }
    }
}

@Composable
private fun AmundsenSideKey(label: String, active: Boolean, left: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Column(
        Modifier.fillMaxWidth().height(82.dp).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (active) t.accent else t.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (active) t.accent else t.text.copy(alpha = 0.8f)))
    }
}

@Composable
fun AmundsenNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(m.title, transitionSpec = { fadeIn(tween(t.duration(400))) togetherWith fadeOut(tween(t.duration(200))) }, label = "amTitle") {
                    Text(it, color = t.text, fontSize = 38.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(m.artist, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pb.album, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlainKey(Icons.Outlined.FastRewind, m.a.previous, t.text, 52.dp, 84.dp)
                    Spacer(Modifier.width(36.dp))
                    PlainKey(if (pb.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, m.a.togglePlay, t.text, 56.dp, 84.dp)
                    Spacer(Modifier.width(36.dp))
                    PlainKey(Icons.Outlined.FastForward, m.a.next, t.text, 52.dp, 84.dp)
                }
                Spacer(Modifier.weight(0.4f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).aspectRatio(1f)) { AnimatedCover(m, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) }
                Row {
                    PlainKey(repeatIcon(pb.repeatMode), m.a.cycleRepeat, if (pb.repeatMode != Player.REPEAT_MODE_OFF) t.accentSecondary else t.text, 30.dp, 56.dp)
                    PlainKey(Icons.Rounded.Shuffle, m.a.toggleShuffle, if (pb.shuffle) t.accentSecondary else t.text, 30.dp, 56.dp)
                }
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(pb.positionMs), color = t.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(66.dp))
            Box(Modifier.weight(1f)) {
                FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.FLAT, fill = t.accent, fillEnd = t.accentSecondary, track = t.text.copy(alpha = 0.75f))
            }
            Text("-" + formatTime(m.remaining), color = t.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
        }
        // hatched function bar
        Row(
            Modifier.fillMaxWidth().height(62.dp).drawBehind {
                var x = -size.height
                while (x < size.width) {
                    drawLine(t.text.copy(alpha = 0.18f), Offset(x, size.height), Offset(x + size.height * 0.6f, 0f), 1.5f)
                    x += 9f
                }
            }
        ) {
            AmundsenFn(Icons.Rounded.Usb, false) { m.navigate(Screen.LIBRARY) }
            VLine(t.text.copy(alpha = 0.6f), Modifier.padding(vertical = 10.dp))
            AmundsenFn(Icons.AutoMirrored.Rounded.QueueMusic, true) { m.navigate(Screen.LIBRARY) }
            VLine(t.text.copy(alpha = 0.6f), Modifier.padding(vertical = 10.dp))
            AmundsenFn(Icons.Rounded.Settings, true) { m.navigate(Screen.SETTINGS) }
        }
    }
}

@Composable
private fun RowScope.AmundsenFn(icon: ImageVector, accent: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (accent) t.accentSecondary else t.text, modifier = Modifier.size(34.dp))
    }
}

// ================================================================================ SEAT MEDIA SYSTEM
// Carbon-textured background; trapezoid tabs (MENU top centre, SELECTION top right, SETUP bottom
// centre) with red edge accents; framed player bar with source name, time and skip keys; album and
// title right-aligned next to the cover; small function keys under the text.

private fun trapezoid(top: Boolean, skew: Float) = object : Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): Outline {
        val p = Path()
        if (top) {
            p.moveTo(0f, 0f); p.lineTo(size.width, 0f); p.lineTo(size.width - skew, size.height); p.lineTo(skew, size.height)
        } else {
            p.moveTo(skew, 0f); p.lineTo(size.width - skew, 0f); p.lineTo(size.width, size.height); p.lineTo(0f, size.height)
        }
        p.close()
        return Outline.Generic(p)
    }
}

@Composable
private fun SeatTab(label: String, top: Boolean, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val shape = trapezoid(top, 40f)
    Box(
        modifier
            .drawBehind {
                // red accent strokes leading away from the tab
                val y = if (top) size.height else 0f
                val dy = if (top) 14.dp.toPx() else -14.dp.toPx()
                drawLine(t.accent, Offset(-60.dp.toPx(), y - dy), Offset(0f, y - dy * 0.1f), 3.dp.toPx())
                drawLine(t.accent, Offset(size.width, y - dy * 0.1f), Offset(size.width + 60.dp.toPx(), y - dy), 3.dp.toPx())
            }
            .clip(shape)
            .background(Brush.verticalGradient(if (active) listOf(Color(0xFF3A3A40), Color(0xFF1C1C20)) else listOf(Color(0xFF26262B), Color(0xFF111114))))
            .border(1.dp, if (active) t.accent else t.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = t.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SeatShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            SeatTab("MENU", true, screen == Screen.NOW_PLAYING, Modifier.align(Alignment.TopCenter).width(260.dp).fillMaxHeight()) { onSelect(Screen.NOW_PLAYING) }
            SeatTab("SELECTION", true, screen == Screen.LIBRARY, Modifier.align(Alignment.TopEnd).width(220.dp).fillMaxHeight()) { onSelect(Screen.LIBRARY) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) { body() }
        Box(Modifier.fillMaxWidth().height(46.dp)) {
            SeatTab("SETUP", false, screen == Screen.SETTINGS, Modifier.align(Alignment.BottomCenter).width(260.dp).fillMaxHeight()) { onSelect(Screen.SETTINGS) }
        }
    }
}

@Composable
fun SeatNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(0.62f).height(70.dp)
                .border(1.dp, t.outline, RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.35f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlainKey(Icons.Rounded.SkipPrevious, m.a.previous, t.text, 32.dp, 64.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Memorija", color = t.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                    Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.FLAT, height = 14.dp, fill = t.text, track = t.text.copy(alpha = 0.25f))
                    }
                    Text(formatTime(pb.durationMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
            PlainKey(Icons.Rounded.SkipNext, m.a.next, t.text, 32.dp, 64.dp)
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(m.artist, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pb.album, color = t.textMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                AnimatedContent(m.title, transitionSpec = { fadeIn(tween(t.duration(400))) togetherWith fadeOut(tween(t.duration(200))) }, label = "seatTitle") {
                    Text(it, color = t.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SeatKey(Icons.Rounded.Shuffle, pb.shuffle, 64.dp, m.a.toggleShuffle)
                    SeatKey(repeatIcon(pb.repeatMode), pb.repeatMode != Player.REPEAT_MODE_OFF, 64.dp, m.a.cycleRepeat)
                    SeatKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, false, 130.dp, m.a.togglePlay)
                }
            }
            Spacer(Modifier.width(26.dp))
            Box(Modifier.fillMaxHeight(0.82f).aspectRatio(1f)) { AnimatedCover(m, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) }
        }
    }
}

@Composable
private fun SeatKey(icon: ImageVector, active: Boolean, width: Dp, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier.size(width, 56.dp)
            .background(Brush.verticalGradient(if (pressed) listOf(Color(0xFF15151A), Color(0xFF2A2A30)) else listOf(Color(0xFF34343A), Color(0xFF17171B))))
            .border(1.dp, if (active) t.accent else t.outline)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) t.accent else t.text, modifier = Modifier.size(28.dp)) }
}

// ============================================================================================= CUPRA
// Floating rounded side panel with icon+label entries; slim top bar with clock; "Source" pill;
// cover left with title / artist / album to its right; thin slider with ring thumb; centred keys.

@Composable
fun CupraShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Icons.Rounded.ViewSidebar, Icons.Rounded.Apps, Icons.Rounded.DirectionsCar).forEach {
                Box(Modifier.padding(end = 10.dp).size(38.dp).clip(RoundedCornerShape(8.dp)).background(t.surfaceHigh), contentAlignment = Alignment.Center) {
                    Icon(it, null, tint = t.text, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Clock()
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.padding(start = 14.dp, top = 6.dp).width(118.dp)
                    .clip(RoundedCornerShape(t.corner))
                    .background(t.surface.copy(alpha = 0.92f))
                    .padding(vertical = 10.dp),
            ) {
                CupraPanelItem(Icons.Rounded.GraphicEq, "Player", screen == Screen.NOW_PLAYING) { onSelect(Screen.NOW_PLAYING) }
                CupraPanelItem(Icons.Rounded.MusicNote, "Media", screen == Screen.LIBRARY) { onSelect(Screen.LIBRARY) }
                CupraPanelItem(Icons.Rounded.Settings, "Settings", screen == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) { body() }
        }
    }
}

@Composable
private fun CupraPanelItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val c by animateColorAsState(if (active) t.text else t.textMuted, tween(t.duration(250)), label = "cupraItem")
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (active) t.accent else c, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = c, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CupraNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(10.dp)).background(t.surfaceHigh)
                    .clickable { m.navigate(Screen.LIBRARY) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayCircleOutline, null, tint = t.text, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Source", color = t.text, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Rounded.ArrowDropDown, null, tint = t.text)
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(start = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight(0.78f).aspectRatio(1f)) { AnimatedCover(m, Modifier.fillMaxSize(), shape = RoundedCornerShape(6.dp)) }
            Spacer(Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                AnimatedContent(m.title, transitionSpec = { (fadeIn(tween(t.duration(400))) + slideInHorizontally { it / 8 }) togetherWith fadeOut(tween(t.duration(200))) }, label = "cupraTitle") {
                    Text(it, color = t.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(m.artist, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pb.album, color = t.textMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        CupraSlider(m)
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
            Text(formatTime(pb.positionMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text(formatTime(pb.durationMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            PlainKey(Icons.Rounded.SkipPrevious, m.a.previous, t.text, 34.dp, 70.dp)
            Spacer(Modifier.width(28.dp))
            PlainKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay, t.text, 44.dp, 76.dp)
            Spacer(Modifier.width(28.dp))
            PlainKey(Icons.Rounded.SkipNext, m.a.next, t.text, 34.dp, 70.dp)
        }
    }
}

@Composable
private fun CupraSlider(m: PlayerModel) {
    val t = LocalCarTheme.current
    val f by animateFloatAsState(m.fraction, tween(300), label = "cupraSlider")
    Canvas(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp).height(34.dp).pointerInput(m.pb.durationMs) {
            detectTapGestures { o -> if (m.pb.durationMs > 0) m.a.seekTo((o.x / size.width * m.pb.durationMs).toLong()) }
        }
    ) {
        val cy = size.height / 2
        drawLine(t.text.copy(alpha = 0.3f), Offset(0f, cy), Offset(size.width, cy), 2.dp.toPx())
        drawLine(t.text, Offset(0f, cy), Offset(size.width * f, cy), 2.dp.toPx())
        val c = Offset(size.width * f, cy)
        drawCircle(t.bgBottom, 11.dp.toPx(), c)
        drawCircle(t.text, 11.dp.toPx(), c, style = Stroke(2.dp.toPx()))
        drawCircle(t.text, 3.dp.toPx(), c)
    }
}

// =============================================================================================== PCM
// Folder tabs (MEDIA / OPTION) top left; album block with small cover and track count; artist and
// title in orange; orange progress with "min" times and codec label; stacked list buttons on the
// right with an orange frame for the selected one; status line with track and boxed clock.

@Composable
fun PcmShell(screen: Screen, onSelect: (Screen) -> Unit, m: PlayerModel, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).padding(start = 14.dp, top = 8.dp).drawBehind {
                drawLine(t.outline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 2f)
            },
            verticalAlignment = Alignment.Bottom,
        ) {
            Screen.entries.forEach { s ->
                val active = s == screen
                Box(
                    Modifier.padding(end = 2.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (active) Brush.verticalGradient(listOf(t.surfaceHigh, t.bgBottom)) else Brush.verticalGradient(listOf(t.surface, t.surface)))
                        .border(1.dp, t.outline, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .clickable { onSelect(s) }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s.factoryTitle(PlayerLayout.PCM), color = if (active) t.text else t.textMuted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) { body() }
        Row(
            Modifier.fillMaxWidth().height(44.dp).drawBehind { drawLine(t.outline, Offset(0f, 0f), Offset(size.width, 0f), 2f) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.padding(start = 14.dp).size(30.dp, 18.dp)) {
                for (i in 0 until 5) {
                    val h = size.height * (i + 1) / 5f
                    drawRect(if (i < 4) t.text.copy(alpha = 0.8f) else t.text.copy(alpha = 0.3f), Offset(i * size.width / 5f, size.height - h), Size(size.width / 5f - 2f, h))
                }
            }
            Spacer(Modifier.width(12.dp))
            VLine(t.outline, Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.MusicNote, null, tint = t.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (m.pb.count > 0) "Pjesma ${m.pb.index + 1} - ${m.title}" else m.title,
                color = t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.fillMaxHeight().border(1.dp, t.outline).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Clock()
            }
        }
    }
}

@Composable
fun PcmNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column(Modifier.weight(1.1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp)) { AnimatedCover(m, Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp), artSize = 256) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(pb.album.ifBlank { "Memorija" }, color = t.text, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pb.count} pjesama", color = t.text, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(m.artist, color = t.accent, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedContent(m.title, transitionSpec = { fadeIn(tween(t.duration(350))) togetherWith fadeOut(tween(t.duration(180))) }, label = "pcmTitle") {
                Text(it, color = t.accent, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Text("mp3", color = t.text, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
            FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.FLAT, height = 14.dp, track = t.text.copy(alpha = 0.3f))
            Row {
                Text("${formatTime(pb.positionMs)} min", color = t.text, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${formatTime(pb.durationMs)} min", color = t.text, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PcmButton("", Modifier.width(80.dp), false, Icons.Rounded.SkipPrevious, m.a.previous)
                PcmButton("", Modifier.width(110.dp), false, if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay)
                PcmButton("", Modifier.width(80.dp), false, Icons.Rounded.SkipNext, m.a.next)
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PcmButton("Traži muziku", Modifier.fillMaxWidth(), false) { m.navigate(Screen.LIBRARY) }
            PcmButton(if (m.isFavorite) "Omiljena ♥" else "Dodaj u omiljene", Modifier.fillMaxWidth(), m.isFavorite, onClick = m.a.toggleFavorite)
            Spacer(Modifier.weight(1f))
            PcmButton("Lista pjesama", Modifier.fillMaxWidth(), false) { m.navigate(Screen.LIBRARY) }
            PcmButton("Redoslijed: ${if (pb.shuffle) "Mix" else "Normalno"}", Modifier.fillMaxWidth(), pb.shuffle, onClick = m.a.toggleShuffle)
            PcmButton(
                "Ponavljanje: " + when (pb.repeatMode) { Player.REPEAT_MODE_ONE -> "Pjesma"; Player.REPEAT_MODE_ALL -> "Sve"; else -> "Isklj." },
                Modifier.fillMaxWidth(), pb.repeatMode != Player.REPEAT_MODE_OFF, onClick = m.a.cycleRepeat,
            )
        }
    }
}

@Composable
private fun PcmButton(label: String, modifier: Modifier, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier.height(54.dp)
            .background(Brush.verticalGradient(if (pressed) listOf(t.surface, t.surfaceHigh) else listOf(t.surfaceHigh, t.surface)))
            .border(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.outline)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = if (icon != null) Alignment.Center else Alignment.CenterStart,
    ) {
        if (icon != null) Icon(icon, null, tint = t.text, modifier = Modifier.size(30.dp))
        else Text(label, color = t.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ============================================================================================ OPEL CID
// Light display; red/grey tab strip with icons on top; row of small status flags; large centred
// text; grey status bar with cells along the bottom.

@Composable
fun CidShell(screen: Screen, onSelect: (Screen) -> Unit, m: PlayerModel, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            CidTab(Icons.Rounded.MusicNote, screen == Screen.NOW_PLAYING, 1.6f) { onSelect(Screen.NOW_PLAYING) }
            CidTab(Icons.AutoMirrored.Rounded.QueueMusic, screen == Screen.LIBRARY, 1f) { onSelect(Screen.LIBRARY) }
            CidTab(Icons.Rounded.DirectionsCar, screen == Screen.SETTINGS, 1f) { onSelect(Screen.SETTINGS) }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) { body() }
        Row(Modifier.fillMaxWidth().height(42.dp).background(Brush.verticalGradient(listOf(Color(0xFFD2D2D2), Color(0xFFB9B9B9))))) {
            CidCell("♪ ${if (m.pb.count > 0) "${m.pb.index + 1}/${m.pb.count}" else "—"}", 1f)
            VLine(Color.White)
            CidCell(formatTime(m.pb.positionMs), 1f)
            VLine(Color.White)
            CidCell(buildString { if (m.pb.shuffle) append("MIX "); if (m.pb.repeatMode != Player.REPEAT_MODE_OFF) append("RPT") }.ifBlank { "—" }, 1f)
            VLine(Color.White)
            Box(Modifier.weight(0.8f).fillMaxHeight(), contentAlignment = Alignment.Center) { Clock() }
        }
    }
}

@Composable
private fun RowScope.CidTab(icon: ImageVector, active: Boolean, weight: Float, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val bg by animateColorAsState(if (active) t.accent else Color(0xFFA8A8A8), tween(t.duration(250)), label = "cidTab")
    Box(
        Modifier.weight(weight).fillMaxHeight().padding(end = 3.dp)
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.85f), bg)))
            .clickable(onClick = onClick),
        contentAlignment = if (weight > 1f) Alignment.CenterStart else Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.padding(horizontal = 18.dp).size(28.dp)) }
}

@Composable
private fun RowScope.CidCell(text: String, weight: Float) {
    Box(Modifier.weight(weight).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF1A1A1A), style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
fun CidNowPlaying(m: PlayerModel) {
    val t = LocalCarTheme.current
    val pb = m.pb
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            CidFlag("SD", true)
            CidFlag("MIX", pb.shuffle)
            CidFlag("[RPT]", pb.repeatMode != Player.REPEAT_MODE_OFF)
            CidFlag("FAV", m.isFavorite)
            CidFlag(if (pb.isPlaying) "PLAY" else "PAUSE", pb.isPlaying)
            CidFlag("MP3", true)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(m.title, transitionSpec = { fadeIn(tween(t.duration(350))) togetherWith fadeOut(tween(t.duration(180))) }, label = "cidTitle") {
                    Text(it, color = t.text, fontSize = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                Text(m.artist, color = t.textMuted, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        FactoryBar(m.fraction, pb.durationMs, m.a.seekTo, BarStyle.FLAT, Modifier.padding(horizontal = 60.dp), height = 14.dp, track = Color(0xFFBDBDBD))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            CidKey(Icons.Rounded.SkipPrevious, m.a.previous)
            Spacer(Modifier.width(10.dp))
            CidKey(if (pb.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, m.a.togglePlay, wide = true)
            Spacer(Modifier.width(10.dp))
            CidKey(Icons.Rounded.SkipNext, m.a.next)
        }
    }
}

@Composable
private fun CidFlag(text: String, on: Boolean) {
    val t = LocalCarTheme.current
    Text(text, color = if (on) t.accent else Color(0xFF9A9A9A), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
}

@Composable
private fun CidKey(icon: ImageVector, onClick: () -> Unit, wide: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier.size(if (wide) 150.dp else 100.dp, 56.dp)
            .background(Brush.verticalGradient(if (pressed) listOf(Color(0xFFBDBDBD), Color(0xFFDADADA)) else listOf(Color(0xFFF4F4F4), Color(0xFFC4C4C4))))
            .border(1.dp, Color(0xFF8A8A8A))
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color(0xFF222222), modifier = Modifier.size(32.dp)) }
}
