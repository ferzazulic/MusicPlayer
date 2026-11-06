package com.ferzazulic.carmusic.cid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Listens for a limited time to the broadcast actions found by [SignalFinder] (plus common music
 * broadcasts) and records which ones the head unit's own player sends, with their extras.
 */
object SignalMonitor {
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private var receiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val ignored = setOf("com.android.music.metachanged", "com.android.music.playstatechanged")

    fun start(context: Context, actions: List<String>, durationMs: Long = 180_000) {
        stop(context)
        val app = context.applicationContext
        val filter = IntentFilter()
        (actions + listOf("com.android.music.metachanged", "com.android.music.playstatechanged"))
            .distinct().take(900).forEach { filter.addAction(it) }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                // Skip our own CarMusic broadcasts.
                if (intent.action in ignored && intent.getStringExtra("package") == c.packageName) return
                val extras = intent.extras?.let { b ->
                    b.keySet().joinToString(", ") { k -> "$k=${runCatching { b.get(k) }.getOrNull()}".take(80) }
                }.orEmpty()
                val line = "${fmt.format(Date())}  ${intent.action}  {$extras}"
                _log.value = (listOf(line) + _log.value).take(60)
            }
        }
        ContextCompat.registerReceiver(app, r, filter, ContextCompat.RECEIVER_EXPORTED)
        receiver = r
        _log.value = listOf("${fmt.format(Date())}  Slušam ${filter.countActions()} kanala… pusti pjesmu u ugrađenom playeru.")
        _running.value = true
        handler.postDelayed({ stop(app) }, durationMs)
    }

    fun stop(context: Context) {
        receiver?.let { runCatching { context.applicationContext.unregisterReceiver(it) } }
        receiver = null
        handler.removeCallbacksAndMessages(null)
        _running.value = false
    }
}
