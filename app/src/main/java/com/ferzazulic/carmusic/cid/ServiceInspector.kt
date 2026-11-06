package com.ferzazulic.carmusic.cid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
import kotlin.coroutines.resume

/**
 * Looks at the HCN media / source / CAN services: which services they declare, which binder
 * interface a running service exposes, and which AIDL interfaces and method-like names their code
 * contains. Binds only to services that are already running (no BIND_AUTO_CREATE), calls nothing.
 */
object ServiceInspector {
    val packages = listOf("com.hcn.mediaservice", "com.hcn.autosource", "com.hcn.autocan", "com.hcn.AutoMediaPlayer")

    data class Svc(val component: String, val exported: Boolean, val permission: String?, val descriptor: String?)
    data class Pkg(val pkg: String, val installed: Boolean, val services: List<Svc>, val interfaces: List<String>, val methods: List<String>)

    private val methodLike = Regex("^[a-z][A-Za-z0-9]{4,40}$")
    private val words = listOf("Media", "Music", "Id3", "ID3", "Track", "Song", "Info", "Time", "Play", "Source", "Can", "Title", "Artist", "Album", "Progress", "Status", "Callback", "Listener", "Register")

    suspend fun inspect(context: Context): List<Pkg> = packages.map { inspectPkg(context, it) }

    private suspend fun inspectPkg(context: Context, pkg: String): Pkg {
        val pm = context.packageManager
        val info = runCatching { pm.getPackageInfo(pkg, PackageManager.GET_SERVICES) }.getOrNull()
            ?: return Pkg(pkg, false, emptyList(), emptyList(), emptyList())
        val services = info.services.orEmpty().map { si ->
            val cn = ComponentName(si.packageName, si.name)
            val descriptor = if (si.exported) describe(context, cn) else null
            Svc(si.name.removePrefix(pkg), si.exported, si.permission, descriptor)
        }
        val (ifaces, methods) = runCatching { codeNames(info.applicationInfo!!.sourceDir, pkg) }.getOrDefault(emptyList<String>() to emptyList())
        return Pkg(pkg, true, services, ifaces, methods)
    }

    /** Binds without creating the service; returns its binder interface descriptor if it is running. */
    private suspend fun describe(context: Context, cn: ComponentName): String? = withTimeoutOrNull(2_500) {
        suspendCancellableCoroutine { cont ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val d = runCatching { binder.interfaceDescriptor }.getOrNull() ?: "(bez opisa)"
                    runCatching { context.unbindService(this) }
                    if (cont.isActive) cont.resume(d)
                }
                override fun onServiceDisconnected(name: ComponentName) {}
                override fun onNullBinding(name: ComponentName) {
                    runCatching { context.unbindService(this) }
                    if (cont.isActive) cont.resume("(null binder)")
                }
            }
            val ok = runCatching { context.bindService(Intent().setComponent(cn), conn, 0) }.getOrElse {
                if (cont.isActive) cont.resume("odbijeno: ${it.javaClass.simpleName}")
                false
            }
            if (!ok && cont.isActive) cont.resume("ne radi / nije dostupno")
            cont.invokeOnCancellation { runCatching { context.unbindService(conn) } }
        }
    } ?: "ne radi (nije pokrenut)"

    /** AIDL-style interfaces (…/IXxx) of this package and method-like names mentioning media topics. */
    private fun codeNames(apk: String, pkg: String): Pair<List<String>, List<String>> {
        val ifaces = HashSet<String>()
        val methods = HashSet<String>()
        val pkgPath = pkg.replace('.', '/')
        ZipFile(apk).use { zip ->
            java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { e ->
                val bytes = zip.getInputStream(e).use { it.readBytes() }
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val count = buf.getInt(0x38)
                val off = buf.getInt(0x3C)
                for (i in 0 until count) {
                    var p = buf.getInt(off + i * 4)
                    while (bytes[p].toInt() and 0x80 != 0) p++
                    p++
                    var end = p
                    while (end < bytes.size && bytes[end].toInt() != 0) end++
                    if (end - p !in 4..160) continue
                    val s = String(bytes, p, end - p, Charsets.UTF_8)
                    if (s.startsWith("L") && s.endsWith(";") && s.contains("/I") && (s.contains("hcn") || s.contains(pkgPath))) {
                        val name = s.substring(1, s.length - 1).replace('/', '.')
                        val simple = name.substringAfterLast('.').substringBefore('$')
                        if (simple.length > 1 && simple[0] == 'I' && simple[1].isUpperCase()) ifaces += name
                    } else if (methodLike.matches(s) && words.any { s.contains(it) }) {
                        methods += s
                    }
                }
            }
        }
        return ifaces.sorted().take(40) to methods.sorted().take(150)
    }

    fun asText(list: List<Pkg>): String = buildString {
        list.forEach { p ->
            appendLine("== ${p.pkg}${if (!p.installed) " (nije instaliran)" else ""}")
            p.services.forEach { appendLine("  servis ${it.component}  exported=${it.exported}  perm=${it.permission ?: "-"}  binder=${it.descriptor ?: "-"}") }
            if (p.interfaces.isNotEmpty()) appendLine("  interfejsi: " + p.interfaces.joinToString(", "))
            if (p.methods.isNotEmpty()) appendLine("  metode: " + p.methods.joinToString(", "))
        }
    }
}
