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
        settings = SettingsRepository(this, appScope)
        music = MusicRepository(this)
        cid = CidManager(this, appScope, settings)
    }

    companion object {
        lateinit var instance: CarMusicApp private set
    }
}
