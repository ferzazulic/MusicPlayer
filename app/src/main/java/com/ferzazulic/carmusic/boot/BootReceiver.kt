package com.ferzazulic.carmusic.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ferzazulic.carmusic.CarMusicApp
import com.ferzazulic.carmusic.MainActivity
import kotlinx.coroutines.launch

/** Starts the player when the head unit boots (ignition on), if enabled in settings. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CarMusicApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                if (app.settings.current().launchOnBoot) {
                    runCatching {
                        context.startActivity(
                            Intent(context, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(MainActivity.EXTRA_FROM_BOOT, true)
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
