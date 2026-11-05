package com.ferzazulic.carmusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Artwork(val bitmap: Bitmap, val dominant: Int, val vibrant: Int)

/** Loads album art (MediaStore thumbnail -> embedded picture) with an in-memory cache and palette extraction. */
object ArtworkLoader {
    private val cache = object : LruCache<String, Artwork>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Artwork) = value.bitmap.byteCount
    }
    private val misses = HashSet<String>()

    fun cached(song: Song, size: Int): Artwork? = cache.get("${song.albumId}_$size")

    suspend fun load(context: Context, song: Song, size: Int = 512): Artwork? {
        val key = "${song.albumId}_$size"
        cache.get(key)?.let { return it }
        synchronized(misses) { if (key in misses) return null }
        return withContext(Dispatchers.IO) {
            val bmp = runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
                } else {
                    context.contentResolver.openInputStream(song.albumArtUri)?.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull() ?: runCatching {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, song.uri)
                    r.embeddedPicture?.let { bytes ->
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        var sample = 1
                        while (opts.outWidth / (sample * 2) >= size) sample *= 2
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                } finally {
                    r.release()
                }
            }.getOrNull()

            if (bmp == null) {
                synchronized(misses) { misses += key }
                null
            } else {
                val palette = Palette.from(bmp).maximumColorCount(16).generate()
                val dominant = palette.getDominantColor(0xFF303030.toInt())
                val vibrant = palette.getVibrantColor(palette.getLightVibrantColor(dominant))
                Artwork(bmp, dominant, vibrant).also { cache.put(key, it) }
            }
        }
    }

    fun clearMisses() = synchronized(misses) { misses.clear() }
}
