package com.ferzazulic.carmusic.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ferzazulic.carmusic.CarMusicApp
import com.ferzazulic.carmusic.data.ArtworkLoader
import com.ferzazulic.carmusic.data.Library
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.playback.PlaybackService
import com.ferzazulic.carmusic.playback.toMediaItem
import com.ferzazulic.carmusic.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUi(
    val connected: Boolean = false,
    val songId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val index: Int = 0,
    val count: Int = 0,
    /** +1 when moving forward in the queue, -1 when going back – drives slide animations. */
    val direction: Int = 1,
    val queueIds: List<Long> = emptyList(),
    val nextIndex: Int = -1,
    val prevIndex: Int = -1,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CarMusicApp
    val settings: StateFlow<AppSettings> = app.settings.settings
    val library: StateFlow<Library> = app.music.library
    val cidStatus = app.cid.status
    val cidPreview = app.cid.preview

    private val _playback = MutableStateFlow(PlaybackUi())
    val playback: StateFlow<PlaybackUi> = _playback

    val currentSong: StateFlow<Song?> = combine(library, playback.distinctUntilChangedBy { it.songId }) { lib, pb ->
        pb.songId?.let { lib.song(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    init {
        viewModelScope.launch {
            val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
            val c = runCatching { MediaController.Builder(app, token).buildAsync().await() }.getOrNull() ?: return@launch
            controller = c
            c.addListener(listener)
            refresh()
            while (isActive) {
                delay(if (c.isPlaying) 250 else 1000)
                _playback.value = _playback.value.copy(
                    positionMs = c.currentPosition,
                    durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: _playback.value.durationMs,
                )
            }
        }
        // Re-scan the library when library filters change.
        viewModelScope.launch {
            settings.distinctUntilChangedBy { it.minDurationSec to it.excludedFolders }.drop(1).collect { rescan() }
        }
    }

    fun rescan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            ArtworkLoader.clearMisses()
            val s = app.settings.current()
            app.music.scan(s.minDurationSec, s.excludedFolders)
            _scanning.value = false
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val prev = _playback.value
        val md = c.mediaMetadata
        val newIndex = c.currentMediaItemIndex
        val newId = c.currentMediaItem?.mediaId?.toLongOrNull()
        val direction = when {
            newId == prev.songId -> prev.direction
            newIndex < prev.index && !(prev.index == prev.count - 1 && newIndex == 0) -> -1
            else -> 1
        }
        _playback.value = PlaybackUi(
            connected = true,
            songId = newId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
            isPlaying = c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING),
            positionMs = c.currentPosition,
            durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: 0,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            index = newIndex,
            count = c.mediaItemCount,
            direction = direction,
            queueIds = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() },
            nextIndex = c.nextMediaItemIndex,
            prevIndex = c.previousMediaItemIndex,
        )
    }

    fun play(songs: List<Song>, index: Int, shuffle: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        val start = if (shuffle && index < 0) songs.indices.random() else index.coerceAtLeast(0)
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0)
        c.prepare()
        c.play()
    }

    fun playQueueIndex(i: Int) = controller?.let { it.seekTo(i, 0); it.play() }

    fun togglePlay() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            val songs = library.value.songs
            if (songs.isNotEmpty()) play(songs, -1, shuffle = true)
            return
        }
        if (c.playWhenReady) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0)
            c.play()
        }
    }

    fun next() = controller?.seekToNext()
    fun previous() = controller?.seekToPrevious()
    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _playback.value = _playback.value.copy(positionMs = ms)
    }
    fun seekBy(deltaMs: Long) = controller?.let { seekTo((it.currentPosition + deltaMs).coerceAtLeast(0)) }
    fun toggleShuffle() = controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = controller?.let {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = app.settings.update(transform)
    fun sendCidTest() = app.cid.sendTest()

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
