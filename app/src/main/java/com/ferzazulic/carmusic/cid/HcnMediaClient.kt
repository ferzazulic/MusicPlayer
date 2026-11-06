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
    private var attachedName: String? = null
    private var connecting = false
    var onStatus: (String) -> Unit = {}
    private val token = Binder()
    private var sent = 0
    private var failed = 0

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
                    onStatus("Povezano s HCN medijskim servisom")
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

    /** Keeps CarMusic attached as a media client (used by the HCN CID output). */
    fun ensureConnected(context: Context, clientName: String, useClientBinder: Boolean = false) {
        if (connecting) return
        if (service != null && attachedName != null && service?.isBinderAlive == true) return
        if (service != null && attachedName == null && attemptsWithoutAttach < 5) { attemptsWithoutAttach++; return }
        attemptsWithoutAttach = 0
        connect(context, clientName, useClientBinder)
    }
    private var attemptsWithoutAttach = 0

    fun connect(context: Context, clientName: String, useClientBinder: Boolean = false) {
        val c = context.applicationContext
        disconnect(c)
        connecting = true
        _log.value = emptyList()
        log("Povezujem se na ${component.className}…")
        val sc = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = binder
                connecting = false
                log("Povezano: ${runCatching { binder.interfaceDescriptor }.getOrNull()}")
                attach(clientName, useClientBinder)
                if (attachedName == null && clientName != "music") {
                    log("Ponovni pokušaj s imenom \"music\"…")
                    attach("music", false)
                }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                service = null
                observer = null
                attachedName = null
                onStatus("HCN medijski servis se odspojio")
                log("Servis se odspojio")
            }
        }
        val ok = runCatching { c.bindService(Intent().setComponent(component), sc, Context.BIND_AUTO_CREATE) }
            .onFailure { log("bindService greška: ${it.message}") }.getOrDefault(false)
        if (ok) conn = sc else { log("bindService nije uspio"); onStatus("HCN medijski servis nije dostupan") }
        if (!ok) connecting = false
    }

    private fun attach(name: String, useClientBinder: Boolean) {
        val s = service ?: return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        // The stock player calls setClientBinder(token) first; optional, since it made the service reject us.
        if (useClientBinder) runCatching {
            val d = Parcel.obtain(); val r = Parcel.obtain()
            try {
                d.writeInterfaceToken(SERVICE)
                d.writeStrongBinder(token)
                s.transact(Binder.FIRST_CALL_TRANSACTION, d, r, 0)
                r.readException()
                log("setClientBinder → OK")
            } finally { d.recycle(); r.recycle() }
        }.onFailure { log("setClientBinder greška: ${it.message}") }
        try {
            data.writeInterfaceToken(SERVICE)
            data.writeString(name)
            data.writeStrongBinder(clientThread)
            val ok = s.transact(Binder.FIRST_CALL_TRANSACTION + 1, data, reply, 0)
            reply.readException()
            log("attachMediaClient(\"$name\") → ${if (ok) "OK" else "odbijeno"}")
            if (ok) attachedName = name else onStatus("HCN servis je odbio prijavu \"$name\"")
        } catch (t: Throwable) {
            log("attachMediaClient greška: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            data.recycle(); reply.recycle()
        }
    }

    /**
     * IMediaObserver.updateMusicPlayInfo(MusicInfo, String state, int position, int duration) – transaction 2.
     * MusicInfo is written exactly like its writeToParcel(): id3Type, index, totalTime, totalSize,
     * title, artist, album, filePath, fileName, favorite.
     */
    fun update(info: NowPlayingInfo, seconds: Boolean, playState: String, pauseState: String): Boolean {
        val obs = observer ?: return false
        fun t(ms: Long): Int = if (seconds) (ms / 1000).toInt() else ms.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(OBSERVER)
            data.writeInt(1) // non-null parcelable
            data.writeInt(if (info.title.isNotBlank()) 1 else 0) // mID3Type
            data.writeInt(info.trackNumber)                       // mIndex
            data.writeInt(t(info.durationMs))                     // mTotalTime
            data.writeInt(info.trackCount)                        // mTotalSize
            data.writeString(info.title)                          // mTitle
            data.writeString(info.artist)                         // mArtist
            data.writeString(info.album)                          // mAlbum
            data.writeString(info.path)                           // mFilePath
            data.writeString(info.path.substringAfterLast('/'))   // mFileName
            data.writeInt(if (info.favorite) 1 else 0)            // mFavorite
            data.writeString(if (info.isPlaying) playState else pauseState)
            data.writeInt(t(info.positionMs))
            data.writeInt(t(info.durationMs))
            obs.transact(Binder.FIRST_CALL_TRANSACTION + 1, data, reply, 0)
            runCatching { reply.readException() }
            sent++
            if (sent == 1 || sent % 30 == 0) log("updateMusicPlayInfo #$sent: T${info.trackNumber} ${info.positionMs / 1000}s \"${info.title}\"")
            onStatus("HCN: poslano $sent ažuriranja (T${info.trackNumber} ${CidFormatter.time(info.positionMs)})")
            true
        } catch (t: Throwable) {
            failed++
            log("updateMusicPlayInfo greška: ${t.javaClass.simpleName}: ${t.message}")
            observer = null
            attachedName = null
            false
        } finally {
            data.recycle(); reply.recycle()
        }
    }

    fun disconnect(context: Context) {
        conn?.let { runCatching { context.applicationContext.unbindService(it) } }
        conn = null
        attachedName = null
        connecting = false
        service = null
        observer = null
    }
}
