package com.ferzazulic.carmusic.playback

import android.view.KeyEvent
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KeyLog(val keyCode: Int, val name: String, val action: String, val repeat: Int, val time: Long)

/** Process-wide bridge between the playback service and the UI (sleep timer, steering-wheel key diagnostics). */
object PlayerHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile var player: Player? = null

    private val _keys = MutableStateFlow<List<KeyLog>>(emptyList())
    val keys: StateFlow<List<KeyLog>> = _keys

    fun reportKey(e: KeyEvent, source: String) {
        val log = KeyLog(
            e.keyCode,
            KeyEvent.keyCodeToString(e.keyCode).removePrefix("KEYCODE_"),
            (if (e.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP") + " · $source",
            e.repeatCount,
            System.currentTimeMillis(),
        )
        _keys.value = (listOf(log) + _keys.value).take(12)
    }

    private val _sleepAt = MutableStateFlow<Long?>(null)
    val sleepAt: StateFlow<Long?> = _sleepAt
    private var sleepJob: Job? = null

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepAt.value = null
            return
        }
        val at = System.currentTimeMillis() + minutes * 60_000L
        _sleepAt.value = at
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            player?.pause()
            _sleepAt.value = null
        }
    }
}
