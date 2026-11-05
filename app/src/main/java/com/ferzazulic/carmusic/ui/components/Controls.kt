package com.ferzazulic.carmusic.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

/** Icon button with a springy press animation and optional "active" accent state. */
@Composable
fun CarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    active: Boolean = false,
    tint: Color? = null,
) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && t.animate) 0.86f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val bg by animateFloatAsState(if (pressed) 0.14f else 0f, tween(150), label = "pressBg")
    Box(
        modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(t.corner.coerceAtMost(size / 2)))
            .background(t.text.copy(alpha = bg))
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription,
            tint = tint ?: if (active) t.accent else t.text,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Large play/pause button: morphing icon, glow halo and press bounce. */
@Composable
fun PlayPauseButton(playing: Boolean, onClick: () -> Unit, size: Dp = 96.dp, modifier: Modifier = Modifier) {
    val t = LocalCarTheme.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && t.animate) 0.9f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "ppScale",
    )
    val halo by animateFloatAsState(if (playing) 1f else 0.4f, tween(t.duration(600)), label = "halo")
    val shape = RoundedCornerShape(if (t.corner >= 10.dp) size / 2 else t.corner * 1.5f)
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (t.base.glow) {
                    val r = this.size.minDimension * 0.85f
                    drawCircle(
                        Brush.radialGradient(listOf(t.accent.copy(alpha = 0.45f * halo), Color.Transparent), center, r),
                        radius = r, center = Offset(this.size.width / 2, this.size.height / 2),
                    )
                }
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(t.accent, t.accent.copy(alpha = 0.78f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = {
                (scaleIn(tween(t.duration(250)), initialScale = 0.4f) + fadeIn(tween(t.duration(200)))) togetherWith
                    (scaleOut(tween(t.duration(200)), targetScale = 0.4f) + fadeOut(tween(t.duration(150))))
            },
            label = "playPause",
        ) { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pauza" else "Pusti",
                tint = t.onAccent,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}
