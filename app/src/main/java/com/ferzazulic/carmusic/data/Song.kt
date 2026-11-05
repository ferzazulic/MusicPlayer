package com.ferzazulic.carmusic.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val track: Int,
    val year: Int,
    val path: String,
    val dateAdded: Long,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val albumArtUri: Uri get() = ContentUris.withAppendedId(ALBUM_ART, albumId)
    val folder: String get() = path.substringBeforeLast('/', "")

    companion object {
        val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

data class Album(val id: Long, val title: String, val artist: String, val songs: List<Song>) {
    val artUri: Uri get() = songs.first().albumArtUri
}

data class Artist(val name: String, val songs: List<Song>) {
    val albumCount: Int get() = songs.map { it.albumId }.distinct().size
}

data class Folder(val path: String, val songs: List<Song>) {
    val name: String get() = path.substringAfterLast('/').ifEmpty { "/" }
}

data class Library(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
) {
    private val byId by lazy { songs.associateBy { it.id } }
    fun song(id: Long): Song? = byId[id]
}
