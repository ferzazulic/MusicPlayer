package com.ferzazulic.carmusic.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Artwork(val bitmap: Bitmap, val dominant: Int, val vibrant: Int)

/**
 * Loads cover art per SONG (never per MediaStore album id: Android frequently assigns the same
 * album id to unrelated files – e.g. everything without an album tag or from one folder – which
 * made one song's cover appear on another).
 *
 * Order: picture embedded in the file itself -> MediaStore thumbnail of that file -> album art.
 */
object ArtworkLoader {
    private val cache = object : LruCache<String, Artwork>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Artwork) = value.bitmap.byteCount
    }
    private val misses = HashSet<String>()

    private fun key(songId: Long, size: Int) = "${songId}_$size"

    fun cached(song: Song, size: Int): Artwork? = cache.get(key(song.id, size))

    suspend fun load(context: Context, song: Song, size: Int = 512): Artwork? {
        cache.get(key(song.id, size))?.let { return it }
        return withContext(Dispatchers.IO) { loadSync(context, song.id, size) }
    }

    /** Blocking variant, also used by the media session to give the head unit / notification the right cover. */
    fun loadSync(context: Context, songId: Long, size: Int): Artwork? {
        val k = key(songId, size)
        cache.get(k)?.let { return it }
        synchronized(misses) { if (k in misses) return null }
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val bmp = embedded(context, uri, size)
            ?: runCatching {
                if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(uri, Size(size, size), null) else null
            }.getOrNull()
            ?: legacyAlbumArt(context, songId, size)
        if (bmp == null) {
            synchronized(misses) { misses += k }
            return null
        }
        val palette = Palette.from(bmp).maximumColorCount(12).generate()
        val dominant = palette.getDominantColor(0xFF303030.toInt())
        val vibrant = palette.getVibrantColor(palette.getLightVibrantColor(dominant))
        return Artwork(bmp, dominant, vibrant).also { cache.put(k, it) }
    }

    private fun embedded(context: Context, uri: Uri, size: Int): Bitmap? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.embeddedPicture?.let { decodeSampled(it, size) }
        } finally {
            r.release()
        }
    }.getOrNull()

    /** Pre-Android-10 fallback: album art is only trustworthy if the album id is unique to this song's album+folder. */
    private fun legacyAlbumArt(context: Context, songId: Long, size: Int): Bitmap? = runCatching {
        val albumId = context.contentResolver.query(
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId),
            arrayOf(MediaStore.Audio.Media.ALBUM_ID), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null } ?: return null
        context.contentResolver.openInputStream(ContentUris.withAppendedId(Song.ALBUM_ART, albumId))?.use { input ->
            decodeSampled(input.readBytes(), size)
        }
    }.getOrNull()

    private fun decodeSampled(bytes: ByteArray, size: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= size && opts.outHeight / (sample * 2) >= size) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun clearMisses() = synchronized(misses) { misses.clear() }
}
