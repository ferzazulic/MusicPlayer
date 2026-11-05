package com.ferzazulic.carmusic.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import com.ferzazulic.carmusic.settings.AppSettings
import kotlin.math.ln

object EqPresets {
    /** UI band centre frequencies in Hz. */
    val bandsHz = listOf(60, 230, 910, 3600, 14000)
    val labels = listOf("60", "230", "910", "3.6k", "14k")

    val presets: Map<String, List<Int>> = linkedMapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass" to listOf(600, 400, 0, 0, 100),
        "Rock" to listOf(500, 300, -200, 300, 500),
        "Pop" to listOf(-100, 300, 500, 300, -100),
        "Jazz" to listOf(300, 100, -100, 200, 400),
        "Klasična" to listOf(400, 200, -100, 200, 400),
        "Elektronska" to listOf(500, 300, 0, 200, 500),
        "Vokal" to listOf(-200, 0, 400, 300, 0),
        "Auto akustika" to listOf(300, 100, -100, 100, 300),
        "Noćna vožnja" to listOf(200, 0, 0, -100, -200),
        "Custom" to listOf(0, 0, 0, 0, 0),
    )
}

class AudioEffects(private val context: Context) {
    private var sessionId = C.AUDIO_SESSION_ID_UNSET
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var last: AppSettings? = null

    fun attach(id: Int) {
        if (id == sessionId) return
        release()
        sessionId = id
        if (id == C.AUDIO_SESSION_ID_UNSET) return
        eq = runCatching { Equalizer(0, id) }.getOrNull()
        bass = runCatching { BassBoost(0, id) }.getOrNull()
        virt = runCatching { Virtualizer(0, id) }.getOrNull()
        loud = runCatching { LoudnessEnhancer(id) }.getOrNull()
        // Let the head unit's own DSP / system equaliser hook into our session as well.
        runCatching {
            context.sendBroadcast(Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            })
        }
        last?.let { apply(it) }
    }

    fun apply(s: AppSettings) {
        last = s
        eq?.let { e ->
            runCatching {
                e.enabled = s.eqEnabled
                if (s.eqEnabled) {
                    val range = e.bandLevelRange
                    for (b in 0 until e.numberOfBands) {
                        val hz = e.getCenterFreq(b.toShort()) / 1000.0
                        val level = interpolate(hz, s.eqBands)
                        e.setBandLevel(b.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                    }
                }
            }
        }
        bass?.let { b ->
            runCatching {
                b.enabled = s.eqEnabled && s.bassBoost > 0
                if (b.strengthSupported) b.setStrength(s.bassBoost.coerceIn(0, 1000).toShort())
            }
        }
        virt?.let { v ->
            runCatching {
                v.enabled = s.eqEnabled && s.virtualizer > 0
                if (v.strengthSupported) v.setStrength(s.virtualizer.coerceIn(0, 1000).toShort())
            }
        }
        loud?.let { l ->
            runCatching {
                l.enabled = s.eqEnabled && s.loudnessGain > 0
                l.setTargetGain(s.loudnessGain)
            }
        }
    }

    /** Log-frequency interpolation of our 5 UI bands onto whatever bands the device equaliser exposes. */
    private fun interpolate(hz: Double, levels: List<Int>): Int {
        val f = EqPresets.bandsHz
        if (hz <= f.first()) return levels.first()
        if (hz >= f.last()) return levels.last()
        for (i in 0 until f.size - 1) {
            if (hz >= f[i] && hz <= f[i + 1]) {
                val t = (ln(hz) - ln(f[i].toDouble())) / (ln(f[i + 1].toDouble()) - ln(f[i].toDouble()))
                return (levels[i] + (levels[i + 1] - levels[i]) * t).toInt()
            }
        }
        return 0
    }

    fun release() {
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            runCatching {
                context.sendBroadcast(Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                })
            }
        }
        listOf(eq, bass, virt, loud).forEach { runCatching { it?.release() } }
        eq = null; bass = null; virt = null; loud = null
        sessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
