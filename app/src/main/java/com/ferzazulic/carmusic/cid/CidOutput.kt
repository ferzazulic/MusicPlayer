package com.ferzazulic.carmusic.cid

import com.ferzazulic.carmusic.settings.AppSettings
import com.ferzazulic.carmusic.settings.CidLayout

data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val trackCount: Int,
    val path: String = "",
    val favorite: Boolean = false,
)

interface CidOutput {
    fun show(info: NowPlayingInfo?, settings: AppSettings)
    fun close() {}
}

/** Text lines as they should appear on the 2-3 line CID text area. */
data class CidText(val line1: String, val line2: String, val line3: String)

object CidFormatter {
    private const val MAX = 28

    fun format(info: NowPlayingInfo?, s: AppSettings, scrollOffset: Int = 0): CidText {
        if (info == null || info.title.isBlank()) return CidText("CarMusic", "", "")
        val icon = if (!s.cidShowPlayIcon) "" else if (info.isPlaying) "▶ " else "❙❙ "
        val track = info.album.ifBlank { "${info.trackNumber}/${info.trackCount}" }
        return when (s.cidLayout) {
            CidLayout.TITLE_ARTIST -> CidText(clip(icon + info.title), clip(info.artist), clip(track))
            CidLayout.ARTIST_TITLE -> CidText(clip(icon + info.artist), clip(info.title), clip(track))
            CidLayout.TITLE_ONLY -> CidText(clip(icon + info.title), "", "")
            CidLayout.SCROLLING -> {
                val full = "${info.artist} - ${info.title}     "
                val start = if (full.length <= MAX) 0 else scrollOffset % full.length
                val rotated = (full.substring(start) + full).take(MAX)
                CidText(icon + rotated, clip(info.album), clip(track))
            }
        }
    }

    private fun clip(s: String) = if (s.length <= MAX) s else s.take(MAX - 1) + "…"

    fun time(ms: Long): String {
        val total = ms / 1000
        return "%d:%02d".format(total / 60, total % 60)
    }
}
