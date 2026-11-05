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
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.ferzazulic.carmusic.data.Artwork
import com.ferzazulic.carmusic.data.ArtworkLoader
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

@Composable
fun rememberArtwork(song: Song?, size: Int = 512): State<Artwork?> {
    val context = LocalContext.current
    return produceState(song?.let { ArtworkLoader.cached(it, size) }, song?.albumId, size) {
        value = song?.let { ArtworkLoader.load(context, it, size) }
    }
}

@Composable
fun ArtworkImage(song: Song?, modifier: Modifier = Modifier, corner: Dp? = null, size: Int = 256) {
    val t = LocalCarTheme.current
    val art = rememberArtwork(song, size).value
    val shape = RoundedCornerShape(corner ?: t.corner)
    Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
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
