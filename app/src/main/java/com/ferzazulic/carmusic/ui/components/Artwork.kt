package com.ferzazulic.carmusic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.ferzazulic.carmusic.data.Artwork
import com.ferzazulic.carmusic.data.ArtworkLoader
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

/**
 * State is keyed on the song id, so a recycled list row or animated slot can never keep showing
 * the previous song's cover while the new one loads.
 */
@Composable
fun rememberArtwork(song: Song?, size: Int = 512): MutableState<Artwork?> {
    val context = LocalContext.current
    val state = remember(song?.id, size) { mutableStateOf(song?.let { ArtworkLoader.cached(it, size) }) }
    LaunchedEffect(song?.id, size) {
        if (state.value == null && song != null) state.value = ArtworkLoader.load(context, song, size)
    }
    return state
}

@Composable
fun ArtworkImage(song: Song?, modifier: Modifier = Modifier, corner: Dp? = null, size: Int = 256, shape: Shape? = null) {
    val t = LocalCarTheme.current
    val art = rememberArtwork(song, size).value
    val s = shape ?: RoundedCornerShape(corner ?: t.corner)
    Box(modifier.clip(s), contentAlignment = Alignment.Center) {
        if (art != null) {
            Image(art.bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(t.surfaceHigh, t.accent.copy(alpha = 0.35f), t.surface))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = t.text.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(0.4f))
            }
        }
    }
}
