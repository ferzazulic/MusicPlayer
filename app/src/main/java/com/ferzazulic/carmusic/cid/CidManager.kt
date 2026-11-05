package com.ferzazulic.carmusic.cid

import android.content.Context
import com.ferzazulic.carmusic.settings.AppSettings
import com.ferzazulic.carmusic.settings.CidLayout
import com.ferzazulic.carmusic.settings.CidMode
import com.ferzazulic.carmusic.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/** Routes now-playing information to the car's upper colour information display (CID). */
class CidManager(context: Context, scope: CoroutineScope, private val settings: SettingsRepository) {

    private val _status = MutableStateFlow("Neaktivno")
    val status: StateFlow<String> = _status

    /** Live preview of what the CID should be showing (rendered in settings). */
    private val _preview = MutableStateFlow(CidText("CarMusic", "", ""))
    val preview: StateFlow<CidText> = _preview

    private val broadcast = BroadcastCidOutput(context)
    private val slcan = SlcanCidOutput(context) { _status.value = it }
    private var last: NowPlayingInfo? = null
    private var scroll = 0

    init {
        scope.launch {
            settings.settings.distinctUntilChangedBy { it.cidMode to it.cidSlcanBitrate }.collect { s ->
                _status.value = when (s.cidMode) {
                    CidMode.OFF -> { slcan.close(); "Isključeno" }
                    CidMode.BROADCAST -> { slcan.close(); "Šaljem metadata broadcast + MediaSession" }
                    CidMode.USB_SLCAN -> "Tražim USB CAN adapter…"
                }
                dispatch(s)
            }
        }
        // The factory radio refreshes the display periodically; do the same (and drive scrolling text).
        scope.launch {
            while (true) {
                delay(1_000)
                val s = settings.settings.value
                scroll++
                if (s.cidMode == CidMode.USB_SLCAN && (scroll % 2 == 0 || s.cidLayout == CidLayout.SCROLLING)) {
                    slcan.showText(CidFormatter.format(last, s, scroll), s)
                }
                _preview.value = CidFormatter.format(last, s, scroll)
            }
        }
    }

    fun update(info: NowPlayingInfo?) {
        last = info
        dispatch(settings.settings.value)
    }

    private fun dispatch(s: AppSettings) {
        _preview.value = CidFormatter.format(last, s, scroll)
        when (s.cidMode) {
            CidMode.OFF -> Unit
            CidMode.BROADCAST -> broadcast.show(last, s)
            CidMode.USB_SLCAN -> {
                broadcast.show(last, s)
                slcan.showText(CidFormatter.format(last, s, scroll), s)
            }
        }
    }

    fun sendTest() {
        val s = settings.settings.value
        val text = CidText("CarMusic test", "Astra H CID", "♪ OK")
        _preview.value = text
        if (s.cidMode == CidMode.USB_SLCAN) slcan.showText(text, s)
        else broadcast.show(NowPlayingInfo("CarMusic test", "Astra H CID", "Test", true, 0, 0, 1, 1), s)
    }
}
