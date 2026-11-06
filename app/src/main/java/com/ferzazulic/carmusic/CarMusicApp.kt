package com.ferzazulic.carmusic

import android.app.Application
import com.ferzazulic.carmusic.cid.CidManager
import com.ferzazulic.carmusic.data.MusicRepository
import com.ferzazulic.carmusic.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CarMusicApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var settings: SettingsRepository private set
    lateinit var music: MusicRepository private set
    lateinit var cid: CidManager private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // The ":monitor" process only hosts the CAN diagnostics service – no player, settings or CID output.
        if (processName().endsWith(":monitor")) return
        settings = SettingsRepository(this, appScope)
        music = MusicRepository(this)
        cid = CidManager(this, appScope, settings)
    }

    private fun processName(): String =
        if (android.os.Build.VERSION.SDK_INT >= 28) getProcessName()
        else runCatching { java.io.File("/proc/self/cmdline").readText().trim('\u0000', ' ') }.getOrDefault(packageName)

    companion object {
        lateinit var instance: CarMusicApp private set
    }
}
