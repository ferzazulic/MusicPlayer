package com.ferzazulic.carmusic.cid

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Client for the firmware's system service "sourceinfo" (android.sourceservice.SourceService), which
 * decides the current audio source and holds the MusicInfo the CAN app shows on the CID.
 * AIDL android.sourceservice.ISourceService:
 *   1 setcallback(ISourceCallback)  2 removecallback(ISourceCallback)  3 onRequestPlayAudio(String)
 *   4 getMusicInfo(): MusicInfo     5 doExtAudioEffect(int, int[])
 * ISourceCallback: 1 onMusicEvent(MusicInfo)  2 onSourceChanged(String)
 */
object SourceInfoClient {
    private const val SERVICE = "android.sourceservice.ISourceService"
    private const val CALLBACK = "android.sourceservice.ISourceCallback"

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var registered = false
    var onSourceChanged: (String) -> Unit = {}

    fun log(s: String) { _log.value = (_log.value + "${fmt.format(Date())}  $s").takeLast(50) }
    fun clear() { _log.value = emptyList() }

    fun service(): IBinder? = runCatching {
        Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "sourceinfo") as IBinder?
    }.onFailure { log("getService greška: ${it.javaClass.simpleName}: ${it.message}") }.getOrNull()

    fun prop(name: String): String = runCatching {
        Runtime.getRuntime().exec(arrayOf("getprop", name)).inputStream.bufferedReader().readText().trim()
    }.getOrDefault("?")

    fun props() {
        log("sys.sourcing.package = ${prop("sys.sourcing.package")}")
        log("sys.top.activity = ${prop("sys.top.activity")}")
    }

    private val callback = object : Binder() {
        init { attachInterface(null, CALLBACK) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
            runCatching { data.enforceInterface(CALLBACK) }
            when (code) {
                FIRST_CALL_TRANSACTION -> log("onMusicEvent ← ${dumpParcelable(data)}")
                FIRST_CALL_TRANSACTION + 1 -> {
                    val src = data.readString().orEmpty()
                    log("onSourceChanged ← $src")
                    onSourceChanged(src)
                }
                else -> log("callback $code")
            }
            reply?.writeNoException()
            return true
        }
    }

    fun describe() {
        val s = service() ?: run { log("Servis \"sourceinfo\" nije dostupan"); return }
        log("sourceinfo: ${runCatching { s.interfaceDescriptor }.getOrElse { "greška ${it.message}" }}")
        props()
    }

    fun watch() {
        val s = service() ?: return
        if (registered) { log("Već pratim izvor"); return }
        call(s, 1, "setcallback") { it.writeStrongBinder(callback) }?.let { registered = true }
    }

    fun requestSource(packageName: String) {
        val s = service() ?: return
        props()
        call(s, 3, "onRequestPlayAudio(\"$packageName\")") { it.writeString(packageName) }
        Thread {
            Thread.sleep(1200)
            props()
        }.start()
    }

    fun musicInfo() {
        val s = service() ?: return
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(SERVICE)
            val ok = s.transact(4, data, reply, 0)
            reply.readException()
            log("getMusicInfo → ${if (ok) dumpParcelable(reply) else "odbijeno"}")
        } catch (t: Throwable) {
            log("getMusicInfo greška: ${t.javaClass.simpleName}: ${t.message}")
        } finally { data.recycle(); reply.recycle() }
    }

    private fun call(s: IBinder, code: Int, label: String, write: (Parcel) -> Unit): Boolean? {
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(SERVICE)
            write(data)
            val ok = s.transact(code, data, reply, 0)
            reply.readException()
            log("$label → ${if (ok) "OK" else "odbijeno"}")
            if (ok) true else null
        } catch (t: Throwable) {
            log("$label greška: ${t.javaClass.simpleName}: ${t.message}")
            null
        } finally { data.recycle(); reply.recycle() }
    }

    /** Heuristic dump of a parcelable we do not know the layout of: ints and UTF-16 strings in order. */
    fun dumpParcelable(p: Parcel): String {
        val start = p.dataPosition()
        val end = p.dataSize()
        val parts = ArrayList<String>()
        var pos = start
        while (pos + 4 <= end && parts.size < 40) {
            p.setDataPosition(pos)
            val v = p.readInt()
            val asString = if (v in 1..300 && pos + 4 + v * 2 <= end) {
                p.setDataPosition(pos)
                runCatching { p.readString() }.getOrNull()?.takeIf { s -> s.length == v && s.all { it.code >= 0x20 } }
            } else null
            if (asString != null) {
                parts += "\"$asString\""
                pos = p.dataPosition()
            } else if (v == -1) {
                parts += "null"
                pos += 4
            } else {
                parts += v.toString()
                pos += 4
            }
        }
        return parts.joinToString(", ")
    }
}
