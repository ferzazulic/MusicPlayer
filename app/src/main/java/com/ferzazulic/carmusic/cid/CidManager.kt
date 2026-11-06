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
class CidManager(private val context: Context, scope: CoroutineScope, private val settings: SettingsRepository) {

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
                    CidMode.OFF -> { slcan.close(); HcnMediaClient.disconnect(context); "Isključeno" }
                    CidMode.BROADCAST -> { slcan.close(); HcnMediaClient.disconnect(context); "Šaljem metadata broadcast + MediaSession" }
                    CidMode.HCN_MEDIA -> { slcan.close(); "Povezujem se s HCN medijskim servisom…" }
                    CidMode.USB_SLCAN -> { HcnMediaClient.disconnect(context); "Tražim USB CAN adapter…" }
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
                // Re-send every second: the CAN-box / radio keeps overwriting the display.
                if (s.cidMode == CidMode.USB_SLCAN) sendUsb(s)
                if (s.cidMode == CidMode.HCN_MEDIA) sendHcn(s)
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
            CidMode.HCN_MEDIA -> {
                broadcast.show(last, s)
                sendHcn(s)
            }
            CidMode.USB_SLCAN -> {
                broadcast.show(last, s)
                sendUsb(s)
            }
        }
    }

    private fun sendHcn(s: AppSettings) {
        HcnMediaClient.onStatus = { _status.value = it }
        HcnMediaClient.ensureConnected(context, s.hcnClientName.ifBlank { "HMediaPlayer" })
        last?.let { HcnMediaClient.update(it, s.hcnTimeInSeconds, s.hcnPlayState, s.hcnPauseState) }
    }

    private fun sendUsb(s: AppSettings) {
        val text = CidFormatter.format(last, s, scroll)
        val info = last
        slcan.send(text.line1, text.line2, info?.album ?: "", s)
    }

    fun sendTest() {
        val s = settings.settings.value
        val text = CidText("CarMusic test", "Astra H CID", "♪ OK")
        _preview.value = text
        if (s.cidMode == CidMode.USB_SLCAN) slcan.send(text.line1, text.line2, text.line3, s)
        else broadcast.show(NowPlayingInfo("CarMusic test", "Astra H CID", "Test", true, 0, 0, 1, 1), s)
    }
}
