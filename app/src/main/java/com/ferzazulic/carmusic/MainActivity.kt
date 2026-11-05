package com.ferzazulic.carmusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ferzazulic.carmusic.playback.PlayerHub
import com.ferzazulic.carmusic.ui.MainScreen
import com.ferzazulic.carmusic.ui.MainViewModel
import com.ferzazulic.carmusic.ui.theme.CarMusicTheme
import com.ferzazulic.carmusic.ui.theme.CarThemes

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.rescan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        requestPermissionsIfNeeded()

        setContent {
            val settings by vm.settings.collectAsState()
            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            CarMusicTheme(
                theme = CarThemes.byId(settings.themeId),
                customAccent = settings.customAccent?.let { Color(it.toInt()) },
                animations = settings.animationLevel,
            ) {
                MainScreen(vm)
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isEmpty()) vm.rescan() else permissionLauncher.launch(needed.toTypedArray())
    }

    /** Logs every key for the steering-wheel diagnostics page (does not consume it). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK) PlayerHub.reportKey(event, "aplikacija")
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val EXTRA_FROM_BOOT = "from_boot"
    }
}
