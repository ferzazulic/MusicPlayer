package com.ferzazulic.carmusic.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("car_music_settings")

class SettingsRepository(private val context: Context, scope: CoroutineScope) {

    private object K {
        val theme = stringPreferencesKey("theme")
        val accent = longPreferencesKey("accent")
        val anim = stringPreferencesKey("anim")
        val artBg = booleanPreferencesKey("art_bg")
        val visualizer = booleanPreferencesKey("visualizer")
        val clock = booleanPreferencesKey("clock")
        val screenOn = booleanPreferencesKey("screen_on")
        val largeControls = booleanPreferencesKey("large_controls")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val bass = intPreferencesKey("bass")
        val virt = intPreferencesKey("virt")
        val loud = intPreferencesKey("loud")
        val fade = booleanPreferencesKey("fade")
        val fadeMs = intPreferencesKey("fade_ms")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val speed = floatPreferencesKey("speed")
        val resume = booleanPreferencesKey("resume")
        val autoplay = booleanPreferencesKey("autoplay")
        val boot = booleanPreferencesKey("boot")
        val rememberPos = booleanPreferencesKey("remember_pos")
        val prevRestart = booleanPreferencesKey("prev_restart")
        val pauseDisconnect = booleanPreferencesKey("pause_disconnect")
        val swcLong = booleanPreferencesKey("swc_long")
        val swcStep = intPreferencesKey("swc_step")
        val swcDouble = booleanPreferencesKey("swc_double")
        val minDur = intPreferencesKey("min_dur")
        val excluded = stringSetPreferencesKey("excluded")
        val sort = stringPreferencesKey("sort")
        val cidMode = stringPreferencesKey("cid_mode")
        val cidLayout = stringPreferencesKey("cid_layout")
        val cidIcon = booleanPreferencesKey("cid_icon")
        val cidBitrate = intPreferencesKey("cid_bitrate")
        val cidTx = intPreferencesKey("cid_tx")
        val cidFc = intPreferencesKey("cid_fc")
        val cidRadio = booleanPreferencesKey("cid_radio")
        val favorites = stringSetPreferencesKey("favorites")
        val nightDim = booleanPreferencesKey("night_dim")
        val nightFrom = intPreferencesKey("night_from")
        val nightTo = intPreferencesKey("night_to")
        val swipe = booleanPreferencesKey("swipe")
    }

    val settings: StateFlow<AppSettings> = context.dataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun current(): AppSettings = context.dataStore.data.first().toSettings()

    private val writeScope = scope

    fun update(transform: (AppSettings) -> AppSettings) {
        writeScope.launch {
            context.dataStore.edit { prefs ->
                val next = transform(prefs.toSettings())
                prefs.write(next)
            }
        }
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeId = this[K.theme] ?: d.themeId,
            customAccent = this[K.accent],
            animationLevel = enum(K.anim, d.animationLevel),
            artworkBackground = this[K.artBg] ?: d.artworkBackground,
            showVisualizer = this[K.visualizer] ?: d.showVisualizer,
            showClock = this[K.clock] ?: d.showClock,
            keepScreenOn = this[K.screenOn] ?: d.keepScreenOn,
            largeControls = this[K.largeControls] ?: d.largeControls,
            eqEnabled = this[K.eqEnabled] ?: d.eqEnabled,
            eqPreset = this[K.eqPreset] ?: d.eqPreset,
            eqBands = this[K.eqBands]?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 5 } ?: d.eqBands,
            bassBoost = this[K.bass] ?: d.bassBoost,
            virtualizer = this[K.virt] ?: d.virtualizer,
            loudnessGain = this[K.loud] ?: d.loudnessGain,
            fadeOnPlayPause = this[K.fade] ?: d.fadeOnPlayPause,
            fadeDurationMs = this[K.fadeMs] ?: d.fadeDurationMs,
            skipSilence = this[K.skipSilence] ?: d.skipSilence,
            playbackSpeed = this[K.speed] ?: d.playbackSpeed,
            resumeOnStart = this[K.resume] ?: d.resumeOnStart,
            autoPlayOnStart = this[K.autoplay] ?: d.autoPlayOnStart,
            launchOnBoot = this[K.boot] ?: d.launchOnBoot,
            rememberPosition = this[K.rememberPos] ?: d.rememberPosition,
            previousRestartsTrack = this[K.prevRestart] ?: d.previousRestartsTrack,
            pauseOnDisconnect = this[K.pauseDisconnect] ?: d.pauseOnDisconnect,
            swcLongPressSeek = this[K.swcLong] ?: d.swcLongPressSeek,
            swcSeekStepSec = this[K.swcStep] ?: d.swcSeekStepSec,
            swcDoublePressNext = this[K.swcDouble] ?: d.swcDoublePressNext,
            minDurationSec = this[K.minDur] ?: d.minDurationSec,
            excludedFolders = this[K.excluded] ?: d.excludedFolders,
            sortOrder = enum(K.sort, d.sortOrder),
            cidMode = enum(K.cidMode, d.cidMode),
            cidLayout = enum(K.cidLayout, d.cidLayout),
            cidShowPlayIcon = this[K.cidIcon] ?: d.cidShowPlayIcon,
            cidSlcanBitrate = this[K.cidBitrate] ?: d.cidSlcanBitrate,
            cidTxId = this[K.cidTx] ?: d.cidTxId,
            cidFcId = this[K.cidFc] ?: d.cidFcId,
            cidEmulateRadio = this[K.cidRadio] ?: d.cidEmulateRadio,
            favorites = this[K.favorites] ?: d.favorites,
            nightDim = this[K.nightDim] ?: d.nightDim,
            nightFrom = this[K.nightFrom] ?: d.nightFrom,
            nightTo = this[K.nightTo] ?: d.nightTo,
            swipeOnCover = this[K.swipe] ?: d.swipeOnCover,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.theme] = s.themeId
        if (s.customAccent != null) this[K.accent] = s.customAccent else remove(K.accent)
        this[K.anim] = s.animationLevel.name
        this[K.artBg] = s.artworkBackground
        this[K.visualizer] = s.showVisualizer
        this[K.clock] = s.showClock
        this[K.screenOn] = s.keepScreenOn
        this[K.largeControls] = s.largeControls
        this[K.eqEnabled] = s.eqEnabled
        this[K.eqPreset] = s.eqPreset
        this[K.eqBands] = s.eqBands.joinToString(",")
        this[K.bass] = s.bassBoost
        this[K.virt] = s.virtualizer
        this[K.loud] = s.loudnessGain
        this[K.fade] = s.fadeOnPlayPause
        this[K.fadeMs] = s.fadeDurationMs
        this[K.skipSilence] = s.skipSilence
        this[K.speed] = s.playbackSpeed
        this[K.resume] = s.resumeOnStart
        this[K.autoplay] = s.autoPlayOnStart
        this[K.boot] = s.launchOnBoot
        this[K.rememberPos] = s.rememberPosition
        this[K.prevRestart] = s.previousRestartsTrack
        this[K.pauseDisconnect] = s.pauseOnDisconnect
        this[K.swcLong] = s.swcLongPressSeek
        this[K.swcStep] = s.swcSeekStepSec
        this[K.swcDouble] = s.swcDoublePressNext
        this[K.minDur] = s.minDurationSec
        this[K.excluded] = s.excludedFolders
        this[K.sort] = s.sortOrder.name
        this[K.cidMode] = s.cidMode.name
        this[K.cidLayout] = s.cidLayout.name
        this[K.cidIcon] = s.cidShowPlayIcon
        this[K.cidBitrate] = s.cidSlcanBitrate
        this[K.cidTx] = s.cidTxId
        this[K.cidFc] = s.cidFcId
        this[K.cidRadio] = s.cidEmulateRadio
        this[K.favorites] = s.favorites
        this[K.nightDim] = s.nightDim
        this[K.nightFrom] = s.nightFrom
        this[K.nightTo] = s.nightTo
        this[K.swipe] = s.swipeOnCover
    }
}
