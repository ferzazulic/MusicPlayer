package com.ferzazulic.carmusic.cid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EXPERIMENTAL client for the HCN firmware media service (com.hcn.mediaservice). Registers CarMusic as
 * a media client the same way the stock player does:
 *   IRemotePlayerService.attachMediaClient(name, IMediaClientThread)       (transaction 2)
 *   -> service calls IMediaClientThread.bindMediaClient(IMediaObserver)     (transaction 1)
 *   -> client reports IMediaObserver.updateMusicPlayInfo(MusicInfo, ...)    (transaction 2)
 * This step only attaches and records what the service sends back.
 */
object HcnMediaClient {
    const val SERVICE = "com.hcn.mediaservice.api.IRemotePlayerService"
    const val CLIENT = "com.hcn.media.IMediaClientThread"
    const val OBSERVER = "com.hcn.mediaservice.api.IMediaObserver"
    private val component = ComponentName("com.hcn.mediaservice", "com.hcn.mediaservice.service.MediaPlayerService")

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @Volatile var service: IBinder? = null
        private set
    @Volatile var observer: IBinder? = null
        private set
    private var conn: ServiceConnection? = null

    private fun log(s: String) {
        _log.value = (_log.value + "${fmt.format(Date())}  $s").takeLast(40)
    }

    /** Our IMediaClientThread implementation – the service calls into it. */
    private val clientThread = object : Binder() {
        init { attachInterface(null, CLIENT) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
            runCatching { data.enforceInterface(CLIENT) }
            when (code) {
                FIRST_CALL_TRANSACTION -> {
                    val obs = data.readStrongBinder()
                    observer = obs
                    log("bindMediaClient ← observer: ${runCatching { obs?.interfaceDescriptor }.getOrNull()}")
                }
                FIRST_CALL_TRANSACTION + 1 -> {
                    val a = data.readString()
                    val b = data.readString()
                    log("scheduleTask ← \"$a\", \"$b\"")
                }
                else -> log("nepoznat poziv $code (${data.dataSize()} B)")
            }
            reply?.writeNoException()
            return true
        }
    }

    fun connect(context: Context, clientName: String) {
        val c = context.applicationContext
        disconnect(c)
        _log.value = emptyList()
        log("Povezujem se na ${component.className}…")
        val sc = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = binder
                log("Povezano: ${runCatching { binder.interfaceDescriptor }.getOrNull()}")
                attach(clientName)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
                log("Servis se odspojio")
            }
        }
        val ok = runCatching { c.bindService(Intent().setComponent(component), sc, Context.BIND_AUTO_CREATE) }
            .onFailure { log("bindService greška: ${it.message}") }.getOrDefault(false)
        if (ok) conn = sc else log("bindService nije uspio")
    }

    private fun attach(name: String) {
        val s = service ?: return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(SERVICE)
            data.writeString(name)
            data.writeStrongBinder(clientThread)
            val ok = s.transact(Binder.FIRST_CALL_TRANSACTION + 1, data, reply, 0)
            reply.readException()
            log("attachMediaClient(\"$name\") → ${if (ok) "OK" else "odbijeno"}")
        } catch (t: Throwable) {
            log("attachMediaClient greška: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            data.recycle(); reply.recycle()
        }
    }

    fun disconnect(context: Context) {
        conn?.let { runCatching { context.applicationContext.unbindService(it) } }
        conn = null
        service = null
        observer = null
    }
}
