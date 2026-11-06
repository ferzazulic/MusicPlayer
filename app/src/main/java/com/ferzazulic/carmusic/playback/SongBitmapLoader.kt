package com.ferzazulic.carmusic.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.ferzazulic.carmusic.data.ArtworkLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Supplies the media session (notification, lock screen, head unit / CAN-box) with the cover that
 * belongs to the exact song, using the same per-song loader as the UI.
 */
@UnstableApi
class SongBitmapLoader(private val context: Context) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    private val fallback = DataSourceBitmapLoader(context)

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (uri.scheme != SCHEME) return fallback.loadBitmap(uri)
        val id = uri.lastPathSegment?.toLongOrNull()
        return executor.submit<Bitmap> {
            id?.let { ArtworkLoader.loadSync(context, it, 512)?.bitmap } ?: throw IOException("No artwork for $uri")
        }
    }

    companion object {
        const val SCHEME = "carmusic-art"
        fun uriFor(songId: Long): Uri = Uri.parse("$SCHEME://song/$songId")
    }
}
