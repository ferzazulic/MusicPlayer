package com.ferzazulic.carmusic.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.abs

/**
 * Silent android.media.MediaPlayer that mirrors CarMusic's playback (same file, position, play/pause).
 * HCN firmware records path, state, duration and position only from MediaPlayer and forwards them to
 * the CAN-box, which shows the track time on the Opel CID. Audio is still produced by ExoPlayer.
 */
class ShadowMediaPlayer(private val context: Context) {
    private var mp: MediaPlayer? = null
    private var currentUri: Uri? = null
    private var prepared = false
    private var wantPlay = false
    private var wantPos = 0L

    var enabled = false
        set(v) {
            field = v
            if (!v) release()
        }

    fun sync(uri: Uri?, playing: Boolean, positionMs: Long, seeked: Boolean) {
        if (!enabled || uri == null) {
            release()
            return
        }
        wantPlay = playing
        wantPos = positionMs
        if (uri != currentUri) {
            load(uri)
            return
        }
        val p = mp ?: return
        if (!prepared) return
        runCatching {
            if (seeked || abs(p.currentPosition - positionMs) > 1500) {
                p.seekTo(positionMs.toInt()) // play state is applied when the seek completes
            } else {
                applyState(p)
            }
        }
    }

    private fun applyState(p: MediaPlayer) {
        if (wantPlay && !p.isPlaying) p.start()
        else if (!wantPlay && p.isPlaying) p.pause()
    }

    private fun load(uri: Uri) {
        release()
        currentUri = uri
        val m = MediaPlayer()
        mp = m
        runCatching {
            m.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            m.setVolume(0f, 0f)
            m.setOnPreparedListener {
                prepared = true
                it.seekTo(wantPos.toInt())
            }
            m.setOnSeekCompleteListener { runCatching { applyState(it) } }
            m.setOnErrorListener { _, _, _ -> release(); true }
            m.setDataSource(context, uri)
            m.prepareAsync()
        }.onFailure { release() }
    }

    fun release() {
        mp?.let {
            runCatching { it.reset() }
            runCatching { it.release() }
        }
        mp = null
        currentUri = null
        prepared = false
    }
}
