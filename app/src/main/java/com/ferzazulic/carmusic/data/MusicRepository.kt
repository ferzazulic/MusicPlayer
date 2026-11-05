package com.ferzazulic.carmusic.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** Reads every audio file on internal storage / SD / USB that MediaStore has indexed. */
class MusicRepository(private val context: Context) {

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library

    private val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    suspend fun scan(minDurationSec: Int, excludedFolders: Set<String>): Library = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= ?"
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection,
                arrayOf((minDurationSec * 1000L).coerceAtLeast(60_000L).toString()), null,
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val duration = c.getLong(iDur)
                    if (duration < minDurationSec * 1000L) continue
                    val path = c.getString(iData).orEmpty()
                    if (excludedFolders.any { path.startsWith(it) }) continue
                    val artist = c.getString(iArtist).orEmpty()
                    songs += Song(
                        id = c.getLong(iId),
                        title = c.getString(iTitle)?.takeIf { it.isNotBlank() }
                            ?: path.substringAfterLast('/').substringBeforeLast('.'),
                        artist = if (artist.isBlank() || artist == "<unknown>") "Nepoznat izvođač" else artist,
                        album = c.getString(iAlbum).orEmpty().ifBlank { "Nepoznat album" },
                        albumId = c.getLong(iAlbumId),
                        durationMs = duration,
                        track = c.getInt(iTrack) % 1000,
                        year = c.getInt(iYear),
                        path = path,
                        dateAdded = c.getLong(iAdded),
                    )
                }
            }
        }
        songs.sortWith { a, b -> collator.compare(a.title, b.title) }

        val albums = songs.groupBy { it.albumId }.map { (id, list) ->
            val sorted = list.sortedWith(compareBy<Song> { it.track }.thenBy { it.title })
            Album(id, sorted.first().album, sorted.groupingBy { it.artist }.eachCount().maxBy { it.value }.key, sorted)
        }.sortedWith { a, b -> collator.compare(a.title, b.title) }

        val artists = songs.groupBy { it.artist }.map { (name, list) ->
            Artist(name, list.sortedWith(compareBy<Song> { it.album }.thenBy { it.track }))
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }

        val folders = songs.groupBy { it.folder }.map { (path, list) ->
            Folder(path, list.sortedBy { it.path.substringAfterLast('/') })
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }

        Library(songs, albums, artists, folders).also { _library.value = it }
    }
}
