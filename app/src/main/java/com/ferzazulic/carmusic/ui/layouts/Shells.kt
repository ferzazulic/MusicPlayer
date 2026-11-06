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
    PlayerLayout.MMI, PlayerLayout.VC -> when (this) { Screen.NOW_PLAYING -> "Media"; Screen.LIBRARY -> "Lista"; Screen.SETTINGS -> "Settings" }
    PlayerLayout.RCD, PlayerLayout.CID -> when (this) { Screen.NOW_PLAYING -> "MEDIA"; Screen.LIBRARY -> "LISTA"; Screen.SETTINGS -> "SETUP" }
    PlayerLayout.PCM -> when (this) { Screen.NOW_PLAYING -> "MEDIA"; Screen.LIBRARY -> "LISTA"; Screen.SETTINGS -> "OPTION" }
    PlayerLayout.CUPRA -> when (this) { Screen.NOW_PLAYING -> "Player"; Screen.LIBRARY -> "Media"; Screen.SETTINGS -> "Settings" }
    else -> when (this) { Screen.NOW_PLAYING -> "Media"; Screen.LIBRARY -> "Lista"; Screen.SETTINGS -> "Postavke" }
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
    m: PlayerModel,
    mini: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        mini()
    }
    when (layout) {
        PlayerLayout.RCD -> RcdShell(screen, onSelect, body)
        PlayerLayout.MIB3 -> Mib3Shell(screen, onSelect, body)
        PlayerLayout.AMUNDSEN -> AmundsenShell(screen, onSelect, m, body)
        PlayerLayout.SEAT -> SeatShell(screen, onSelect, body)
        PlayerLayout.CUPRA -> CupraShell(screen, onSelect, body)
        PlayerLayout.MMI -> MmiShell(screen, onSelect, m, body)
        PlayerLayout.VC -> VcShell(screen, onSelect, m, body)
        PlayerLayout.PCM -> PcmShell(screen, onSelect, m, body)
        PlayerLayout.CID -> CidShell(screen, onSelect, m, body)
        PlayerLayout.COCKPIT -> CockpitShell(screen, onSelect, body)
        PlayerLayout.DIALS -> DialsShell(screen, onSelect, body)
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
private fun DialsShell(screen: Screen, onSelect: (Screen) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(50.dp).padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().width(260.dp).clip(slantShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(t.accent, t.accent.copy(alpha = 0.25f)))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    screen.factoryTitle(PlayerLayout.DIALS).uppercase(), color = t.onAccent,
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
                        Text(s.factoryTitle(PlayerLayout.DIALS).uppercase(), color = if (active) t.onAccent else t.text, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
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

