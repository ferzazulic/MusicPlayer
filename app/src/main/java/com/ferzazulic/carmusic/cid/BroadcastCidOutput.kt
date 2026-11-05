package com.ferzazulic.carmusic.cid

import android.content.Context
import android.content.Intent
import com.ferzazulic.carmusic.settings.AppSettings

/**
 * Publishes the de-facto standard "music metadata" broadcasts. Most Android head units built for the
 * Opel Astra H ship with a CAN-bus decoder (canbox) whose system app forwards these values to the
 * original colour display (CID/GID) and the instrument cluster. Media session metadata is published
 * by Media3 as well, which covers the head units that read MediaSession instead.
 */
class BroadcastCidOutput(private val context: Context) : CidOutput {
    private var lastKey: String? = null

    override fun show(info: NowPlayingInfo?, settings: AppSettings) {
        if (info == null) return
        val key = "${info.title}|${info.artist}|${info.isPlaying}"
        val metaChanged = key != lastKey
        lastKey = key
        fun Intent.fill(): Intent = apply {
            putExtra("id", info.trackNumber.toLong())
            putExtra("track", info.title)
            putExtra("title", info.title)
            putExtra("artist", info.artist)
            putExtra("album", info.album)
            putExtra("playing", info.isPlaying)
            putExtra("isPlaying", info.isPlaying)
            putExtra("duration", info.durationMs)
            putExtra("position", info.positionMs)
            putExtra("ListSize", info.trackCount)
            putExtra("package", context.packageName)
            putExtra("scrobbling_source", context.packageName)
        }
        runCatching {
            if (metaChanged) context.sendBroadcast(Intent("com.android.music.metachanged").fill())
            context.sendBroadcast(Intent("com.android.music.playstatechanged").fill())
        }
    }
}
