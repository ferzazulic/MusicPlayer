@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ferzazulic.carmusic.ui.layouts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ferzazulic.carmusic.ui.Screen
import com.ferzazulic.carmusic.ui.components.Clock
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.PlayerLayout

/** Screen titles as the factory units label them. */
fun Screen.factoryTitle(layout: PlayerLayout): String = when (layout) {
    PlayerLayout.MMI -> when (this) { Screen.NOW_PLAYING -> "Media"; Screen.LIBRARY -> "Media · Lista"; Screen.SETTINGS -> "Setup" }
    PlayerLayout.RNS -> when (this) { Screen.NOW_PLAYING -> "MEDIA"; Screen.LIBRARY -> "LISTA"; Screen.SETTINGS -> "SETUP" }
    else -> when (this) { Screen.NOW_PLAYING -> "Media"; Screen.LIBRARY -> "Biblioteka"; Screen.SETTINGS -> "Postavke" }
}

/**
 * The app frame (status bar, navigation, soft keys) for the selected factory layout.
 * [content] fills the main area; [mini] is the mini player shown outside Now Playing.
 */
@Composable
fun AppShell(
    layout: PlayerLayout,
    screen: Screen,
    onSelect: (Screen) -> Unit,
    shuffle: Boolean,
    onShuffle: () -> Unit,
    mini: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        mini()
    }
    when (layout) {
        PlayerLayout.MIB -> MibShell(screen, onSelect, body)
        PlayerLayout.RNS -> RnsShell(screen, onSelect, body)
        PlayerLayout.MMI -> MmiShell(screen, onSelect, shuffle, onShuffle, body)
        PlayerLayout.COCKPIT -> CockpitShell(screen, onSelect, body)
        PlayerLayout.GAUGE -> GaugeShell(screen, onSelect, body)
    }
}

// ------------------------------------------------------------------------------------------- MIB

@Composable
private fun MibShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(screen.factoryTitle(PlayerLayout.MIB), color = t.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Clock()
        }
        HLine(t.outline)
        body()
        HLine(t.outline)
        Row(Modifier.fillMaxWidth().height(76.dp).background(t.surface.copy(alpha = 0.92f))) {
            Screen.entries.forEachIndexed { i, s ->
                if (i > 0) VLine(t.outline, Modifier.padding(vertical = 14.dp))
                val active = s == screen
                val bar by animateFloatAsState(if (active) 1f else 0f, tween(t.duration(300)), label = "mibTab")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth(0.6f * bar).height(3.dp).background(t.accent)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.icon, null, tint = if (active) t.accent else t.textMuted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(s.factoryTitle(PlayerLayout.MIB), color = if (active) t.text else t.textMuted, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------- RNS

@Composable
private fun RnsShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Brush.verticalGradient(listOf(t.surfaceHigh, t.surface)))
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("MEDIA", color = t.accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    screen.factoryTitle(PlayerLayout.RNS), color = t.text, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Clock()
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, t.accent, Color.Transparent)))
            )
        }
        body()
        Row(
            Modifier.fillMaxWidth().height(72.dp).background(t.bgBottom.copy(alpha = 0.7f)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Screen.entries.forEach { s ->
                GlossyKey(Modifier.weight(1f).fillMaxHeight(), active = s == screen, onClick = { onSelect(s) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.icon, null, tint = if (s == screen) t.accent else t.text, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.factoryTitle(PlayerLayout.RNS), color = t.text, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** RNS 510-style glossy soft key: gradient body, specular top highlight, thin luminous edge. */
@Composable
fun GlossyKey(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shade by animateFloatAsState(if (pressed) 0.35f else 0f, tween(120), label = "gloss")
    val shape = RoundedCornerShape(t.corner.coerceAtMost(10.dp))
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.surfaceHigh, t.surface, t.bgBottom)))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), 0f, size.height / 2),
                    size = size.copy(height = size.height / 2),
                )
                drawRect(Color.Black.copy(alpha = shade))
            }
            .border(1.dp, if (active) t.accent else t.outline, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ------------------------------------------------------------------------------------------- MMI

@Composable
private fun MmiShell(
    screen: Screen,
    onSelect: (Screen) -> Unit,
    shuffle: Boolean,
    onShuffle: () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.label(screen.factoryTitle(PlayerLayout.MMI)), color = t.textMuted, style = MaterialTheme.typography.titleMedium, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Clock()
        }
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, t.text.copy(alpha = 0.55f), Color.Transparent)))
        )
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            MmiCornerKey("Svira sada", screen == Screen.NOW_PLAYING, top = true, start = true) { onSelect(Screen.NOW_PLAYING) }
            Spacer(Modifier.weight(1f))
            MmiCornerKey("Lista", screen == Screen.LIBRARY, top = true, start = false) { onSelect(Screen.LIBRARY) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) { body() }
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            MmiCornerKey("Setup", screen == Screen.SETTINGS, top = false, start = true) { onSelect(Screen.SETTINGS) }
            Spacer(Modifier.weight(1f))
            MmiCornerKey(if (shuffle) "Mix: uklj." else "Mix: isklj.", shuffle, top = false, start = false, onClick = onShuffle)
        }
    }
}

/** Label in a screen corner with an L-shaped bracket, like the MMI function-key captions. */
@Composable
private fun MmiCornerKey(label: String, active: Boolean, top: Boolean, start: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val color by animateColorAsState(if (active) t.accent else t.textMuted, tween(t.duration(250)), label = "corner")
    Box(
        Modifier
            .fillMaxHeight()
            .widthIn(min = 180.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                val len = 18.dp.toPx()
                val w = 2.dp.toPx()
                val x = if (start) 10.dp.toPx() else size.width - 10.dp.toPx()
                val y = if (top) 6.dp.toPx() else size.height - 6.dp.toPx()
                drawLine(color, Offset(x, y), Offset(x + if (start) len else -len, y), w)
                drawLine(color, Offset(x, y), Offset(x, y + if (top) len else -len), w)
            }
            .padding(horizontal = 30.dp),
        contentAlignment = if (start) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(t.label(label), color = color, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
    }
}

// --------------------------------------------------------------------------------------- COCKPIT

@Composable
private fun CockpitShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(80.dp).fillMaxHeight().background(t.bgBottom.copy(alpha = 0.75f)).padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Screen.entries.forEach { s ->
                val active = s == screen
                val a by animateFloatAsState(if (active) 1f else 0f, tween(t.duration(300)), label = "vcTab")
                Box(
                    Modifier.fillMaxWidth().height(76.dp).clickable { onSelect(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight(0.6f * a).background(t.accent))
                    Icon(s.icon, s.label, tint = lerp(t.textMuted, t.text, a), modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Clock()
        }
        VLine(t.outline.copy(alpha = 0.5f))
        Column(Modifier.weight(1f).fillMaxHeight()) { body() }
    }
}

// ----------------------------------------------------------------------------------------- GAUGE

/** Parallelogram used throughout the sport layout (slanted like performance-mode graphics). */
fun slantShape(skew: Dp = 14.dp): Shape = SlantShape(skew)

private class SlantShape(private val skew: Dp) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val s = with(density) { skew.toPx() }
        val path = Path().apply {
            moveTo(s, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - s, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun GaugeShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(50.dp).padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().width(260.dp).clip(slantShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(t.accent, t.accent.copy(alpha = 0.25f)))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    screen.factoryTitle(PlayerLayout.GAUGE).uppercase(), color = t.onAccent,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    modifier = Modifier.padding(start = 34.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Clock()
        }
        body()
        Row(
            Modifier.fillMaxWidth().height(70.dp).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Screen.entries.forEach { s ->
                val active = s == screen
                val bg by animateColorAsState(if (active) t.accent else t.surface, tween(t.duration(250)), label = "gTab")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .width(200.dp)
                        .fillMaxHeight()
                        .clip(slantShape())
                        .background(bg)
                        .border(1.dp, if (active) t.accent else t.outline, slantShape())
                        .clickable { onSelect(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.icon, null, tint = if (active) t.onAccent else t.textMuted, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.factoryTitle(PlayerLayout.GAUGE).uppercase(), color = if (active) t.onAccent else t.text, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------- shared

@Composable
fun HLine(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun VLine(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(color))
}

@Composable
fun KeyIcon(icon: ImageVector, tint: Color, size: Dp = 30.dp, description: String? = null) {
    Icon(icon, description, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun MarqueeTitle(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, center: Boolean = false, modifier: Modifier = Modifier) {
    val t = LocalCarTheme.current
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(t.duration(420))) { it / 2 } + fadeIn(tween(t.duration(420)))) togetherWith
                (slideOutVertically(tween(t.duration(280))) { -it / 2 } + fadeOut(tween(t.duration(220))))
        },
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart,
        label = "title",
        modifier = modifier,
    ) { value ->
        Text(
            value, style = style, color = color, maxLines = 1, overflow = TextOverflow.Clip,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500),
        )
    }
}

