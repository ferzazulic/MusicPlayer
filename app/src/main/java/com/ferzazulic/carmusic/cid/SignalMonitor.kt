package com.ferzazulic.carmusic.cid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.ferzazulic.carmusic.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records which broadcasts the head unit's own player sends (actions found by [SignalFinder]).
 * Runs as a foreground service in its own process (":monitor") and writes every line to a file,
 * so it keeps listening – and the log survives – even when the firmware closes CarMusic's main
 * process as the stock player starts.
 */
object SignalMonitor {
    private fun logFile(c: Context) = File(c.filesDir, "can_monitor.log")
    private fun untilFile(c: Context) = File(c.filesDir, "can_monitor.until")
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun start(context: Context, actions: List<String>, durationMs: Long = 180_000) {
        val c = context.applicationContext
        logFile(c).writeText("")
        untilFile(c).writeText((System.currentTimeMillis() + durationMs).toString())
        val i = Intent(c, SignalMonitorService::class.java)
            .putStringArrayListExtra(SignalMonitorService.EXTRA_ACTIONS, ArrayList(actions.take(900)))
            .putExtra(SignalMonitorService.EXTRA_DURATION, durationMs)
        ContextCompat.startForegroundService(c, i)
    }

    /** Records the firmware audio source in the background (CarMusic does not need to be open). */
    fun startSource(context: Context, durationMs: Long = 120_000) {
        val c = context.applicationContext
        logFile(c).writeText("")
        untilFile(c).writeText((System.currentTimeMillis() + durationMs).toString())
        val i = Intent(c, SignalMonitorService::class.java)
            .putExtra(SignalMonitorService.EXTRA_MODE, "source")
            .putExtra(SignalMonitorService.EXTRA_DURATION, durationMs)
        ContextCompat.startForegroundService(c, i)
    }

    fun stop(context: Context) {
        val c = context.applicationContext
        untilFile(c).delete()
        c.stopService(Intent(c, SignalMonitorService::class.java))
    }

    fun isRunning(context: Context): Boolean {
        val until = runCatching { untilFile(context.applicationContext).readText().toLong() }.getOrNull() ?: return false
        return System.currentTimeMillis() < until
    }

    /** Newest first. */
    fun read(context: Context, max: Int = 60): List<String> =
        runCatching { logFile(context.applicationContext).readLines() }.getOrDefault(emptyList())
            .filter { it.isNotBlank() }.asReversed().take(max)

    internal fun append(context: Context, text: String) {
        runCatching { logFile(context.applicationContext).appendText("${fmt.format(Date())}  $text\n") }
    }
}

class SignalMonitorService : Service() {
    private var receiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        val duration = intent?.getLongExtra(EXTRA_DURATION, 180_000) ?: 180_000
        if (intent?.getStringExtra(EXTRA_MODE) == "source") {
            recordSource(duration)
            return START_NOT_STICKY
        }
        val actions = intent?.getStringArrayListExtra(EXTRA_ACTIONS).orEmpty()
        receiver?.let { runCatching { unregisterReceiver(it) } }
        val filter = IntentFilter()
        (actions + listOf("com.android.music.metachanged", "com.android.music.playstatechanged"))
            .distinct().forEach { filter.addAction(it) }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.getStringExtra("package") == packageName) return // our own broadcasts
                val extras = i.extras?.let { b ->
                    b.keySet().joinToString(", ") { k -> "$k=${runCatching { @Suppress("DEPRECATION") b.get(k) }.getOrNull()}".take(80) }
                }.orEmpty()
                SignalMonitor.append(c, "${i.action}  {$extras}")
            }
        }
        ContextCompat.registerReceiver(this, r, filter, ContextCompat.RECEIVER_EXPORTED)
        receiver = r
        SignalMonitor.append(this, "Slušam ${filter.countActions()} kanala – pusti pjesmu u ugrađenom playeru.")
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            SignalMonitor.append(this, "Prisluškivanje završeno.")
            stopSelf()
        }, duration)
        return START_REDELIVER_INTENT
    }

    /** Source recorder: ISourceCallback events + getMusicInfo every 2 s, written to the log file. */
    private fun recordSource(duration: Long) {
        val c = SourceInfoClient
        c.sink = { SignalMonitor.append(this, it) }
        SignalMonitor.append(this, "Snimam izvor – sada otvori ugrađeni player i pusti pjesmu.")
        Thread {
            c.describe()
            c.watch()
            val end = System.currentTimeMillis() + duration
            var n = 0
            while (System.currentTimeMillis() < end && SignalMonitor.isRunning(this)) {
                c.musicInfo()
                if (n++ % 5 == 0) c.props()
                Thread.sleep(2_000)
            }
            SignalMonitor.append(this, "Snimanje izvora završeno.")
            c.sink = null
            handler.post { stopSelf() }
        }.start()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "CAN dijagnostika", NotificationManager.IMPORTANCE_LOW))
        }
        val n: Notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CarMusic prisluškuje CAN kanale")
            .setContentText("Pusti pjesmu u ugrađenom playeru, pa se vrati u CarMusic.")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID, n)
        }
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ACTIONS = "actions"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_MODE = "mode"
        private const val CHANNEL = "can_monitor"
        private const val ID = 4711
    }
}
