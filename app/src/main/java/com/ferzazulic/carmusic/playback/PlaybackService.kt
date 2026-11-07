package com.ferzazulic.carmusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ferzazulic.carmusic.CarMusicApp
import com.ferzazulic.carmusic.MainActivity
import com.ferzazulic.carmusic.cid.NowPlayingInfo
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var exo: ExoPlayer
    private lateinit var player: CarPlayer
    private var session: MediaSession? = null
    private lateinit var effects: AudioEffects
    private lateinit var queueStore: QueueStore
    private lateinit var shadow: ShadowMediaPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val app get() = application as CarMusicApp
    private val settings get() = app.settings.settings.value

    override fun onCreate() {
        super.onCreate()
        effects = AudioEffects(this)
        queueStore = QueueStore(this)
        shadow = ShadowMediaPlayer(this)

        exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player = CarPlayer(exo) { settings }
        PlayerHub.player = player

        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.hcn.MediaActivity.MusicPlayerUiActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(activityIntent)
            .setBitmapLoader(SongBitmapLoader(this))
            .build()

        effects.attach(exo.audioSessionId)
        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) = effects.attach(audioSessionId)

            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                    )
                ) pushCid()
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
                    saveQueue()
                }
                if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED)) savePosition()
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAYBACK_STATE_CHANGED,
                    )
                ) syncShadow(events.contains(Player.EVENT_POSITION_DISCONTINUITY))
            }
        })

        scope.launch {
            app.settings.settings.collect { s ->
                effects.apply(s)
                exo.skipSilenceEnabled = s.skipSilence
                if (exo.playbackParameters.speed != s.playbackSpeed) exo.playbackParameters = PlaybackParameters(s.playbackSpeed)
                exo.setHandleAudioBecomingNoisy(s.pauseOnDisconnect)
                val hcn = s.cidMode == com.ferzazulic.carmusic.settings.CidMode.HCN_MEDIA
                if (shadow.enabled != hcn) {
                    shadow.enabled = hcn
                    syncShadow(true)
                }
            }
        }
        scope.launch { restoreQueue() }
        scope.launch {
            while (isActive) {
                delay(5_000)
                if (exo.isPlaying) {
                    savePosition()
                    pushCid()
                    syncShadow(false)
                }
            }
        }
    }

    private suspend fun restoreQueue() {
        val s = app.settings.current()
        if (!s.resumeOnStart) return
        val saved = queueStore.load() ?: return
        val lib = app.music.library.value.takeIf { it.songs.isNotEmpty() }
            ?: app.music.scan(s.minDurationSec, s.excludedFolders)
        if (exo.mediaItemCount > 0) return // the UI already started something
        val songs = saved.ids.mapNotNull { lib.song(it) }
        if (songs.isEmpty()) return
        val currentId = saved.ids.getOrNull(saved.index)
        val index = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        exo.setMediaItems(songs.map { it.toMediaItem() }, index, if (s.rememberPosition) saved.positionMs else 0L)
        exo.shuffleModeEnabled = saved.shuffle
        exo.repeatMode = saved.repeat
        exo.prepare()
        if (s.autoPlayOnStart) player.play()
    }

    /** Mirrors playback into the silent MediaPlayer the HCN firmware reads (CID track time). */
    private fun syncShadow(seeked: Boolean) {
        val uri = exo.currentMediaItem?.mediaId?.toLongOrNull()?.let {
            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it)
        }
        shadow.sync(uri, exo.isPlaying, exo.currentPosition, seeked)
    }

    private fun queueIds(): List<Long> =
        (0 until exo.mediaItemCount).mapNotNull { exo.getMediaItemAt(it).mediaId.toLongOrNull() }

    private fun saveQueue() {
        if (exo.mediaItemCount == 0) return
        queueStore.save(queueIds(), exo.currentMediaItemIndex, exo.currentPosition, exo.shuffleModeEnabled, exo.repeatMode)
    }

    private fun savePosition() {
        if (exo.mediaItemCount == 0) return
        queueStore.savePosition(exo.currentMediaItemIndex, exo.currentPosition)
    }

    private fun pushCid() {
        val md = exo.mediaMetadata
        app.cid.update(
            NowPlayingInfo(
                title = md.title?.toString().orEmpty(),
                artist = md.artist?.toString().orEmpty(),
                album = md.albumTitle?.toString().orEmpty(),
                isPlaying = exo.isPlaying,
                positionMs = exo.currentPosition,
                durationMs = exo.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
                trackNumber = exo.currentMediaItemIndex + 1,
                trackCount = exo.mediaItemCount,
                path = exo.currentMediaItem?.mediaId?.toLongOrNull()?.let { app.music.library.value.song(it)?.path }.orEmpty(),
                favorite = exo.currentMediaItem?.mediaId?.let { it in settings.favorites } == true,
            )
        )
    }

    // ---------------------------------------------------------------- steering wheel handling

    private var pendingSkip: Runnable? = null
    private var longPressActive = false
    private var pendingToggle: Runnable? = null
    private var lastPlayPauseDown = 0L

    /** Returns true when the key was consumed. */
    private fun handleKey(e: KeyEvent): Boolean {
        val s = settings
        when (e.keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                if (!s.swcLongPressSeek) return false
                val next = e.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                val skip = Runnable { if (next) player.seekToNext() else player.seekToPrevious() }
                when (e.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (e.repeatCount == 0) {
                            longPressActive = false
                            pendingSkip?.let(handler::removeCallbacks)
                            // Some head units never send ACTION_UP; fall back to a short delayed skip.
                            pendingSkip = skip.also { handler.postDelayed(it, 600) }
                        } else {
                            pendingSkip?.let(handler::removeCallbacks)
                            pendingSkip = null
                            longPressActive = true
                            val step = s.swcSeekStepSec * 1000L
                            val target = (exo.currentPosition + if (next) step else -step)
                                .coerceIn(0, (exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
                            exo.seekTo(target)
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        val p = pendingSkip
                        if (p != null && !longPressActive) {
                            handler.removeCallbacks(p)
                            p.run()
                        }
                        pendingSkip = null
                        longPressActive = false
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (!s.swcDoublePressNext) return false
                if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
                    val now = e.eventTime
                    val pending = pendingToggle
                    if (pending != null && now - lastPlayPauseDown < 450) {
                        handler.removeCallbacks(pending)
                        pendingToggle = null
                        player.seekToNext()
                    } else {
                        pendingToggle = Runnable {
                            pendingToggle = null
                            if (player.isPlaying || exo.playWhenReady) player.pause() else player.play()
                        }.also { handler.postDelayed(it, 450) }
                    }
                    lastPlayPauseDown = now
                }
                return true
            }
        }
        return false
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
            mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                if (uri != null) item.buildUpon().setUri(uri).build()
                else item.mediaId.toLongOrNull()?.let { app.music.library.value.song(it)?.toMediaItem() } ?: item
            }.toMutableList()
        )

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val key = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                ?: return false
            PlayerHub.reportKey(key, "volan/sistem")
            return handleKey(key)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val saved = queueStore.load()
            val lib = app.music.library.value
            val songs = saved?.ids?.mapNotNull { lib.song(it) }.orEmpty()
            return if (saved == null || songs.isEmpty()) {
                Futures.immediateFailedFuture(UnsupportedOperationException("Nothing to resume"))
            } else {
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        saved.index.coerceIn(0, songs.lastIndex),
                        saved.positionMs,
                    )
                )
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveQueue()
        PlayerHub.player = null
        player.cancelFade()
        shadow.release()
        app.cid.update(null)
        session?.release()
        session = null
        exo.release()
        effects.release()
        scope.cancel()
        super.onDestroy()
    }
}
