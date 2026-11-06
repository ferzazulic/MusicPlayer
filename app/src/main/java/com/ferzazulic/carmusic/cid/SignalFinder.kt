package com.ferzazulic.carmusic.cid

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * Finds how the head unit's own apps hand media info (e.g. "T362 00:28") to the CAN-box app:
 * reads the string tables of the system CAN / music apps' DEX files and lists intent-action-like
 * strings and keys that mention media, music, track, ID3 and so on. Nothing is executed or changed.
 */
object SignalFinder {

    data class AppHits(val pkg: String, val label: String, val hits: List<String>)

    data class Result(val apps: List<AppHits>) {
        /** Best guesses first: media words + an action-like name, HCN / CAN apps preferred. */
        val candidates: List<String> by lazy {
            apps.flatMap { a -> a.hits.map { it to score(it, a.pkg) } }
                .filter { it.second >= 3 }
                .sortedByDescending { it.second }
                .map { it.first }
                .distinct()
                .take(25)
        }
        /** Every action-like string, used for live listening. */
        val actions: List<String> get() = apps.flatMap { it.hits }.filter { actionLike.matches(it) }.distinct()

        fun asText(): String = buildString {
            appendLine("CarMusic – pretraga CAN kanala")
            apps.forEach { a ->
                appendLine()
                appendLine("== ${a.label} (${a.pkg}) – ${a.hits.size}")
                a.hits.forEach { appendLine(it) }
            }
        }
    }

    private fun score(s: String, pkg: String): Int {
        val l = s.lowercase()
        var sc = 0
        if (listOf("id3", "track", "song", "music", "media").any { l.contains(it) }) sc += 2
        if (listOf("info", "time", "play", "curr", "num", "index").any { l.contains(it) }) sc += 1
        if (actionLike.matches(s)) sc += 1
        if (l.contains("action") || l.contains("intent")) sc += 1
        if (pkg.startsWith("com.hcn.")) sc += 1
        if (l.contains("can")) sc += 1
        return sc
    }

    private val keywords = listOf(
        "music", "media", "id3", "track", "song", "play", "audio", "mp3", "cdc", "disc",
        "source", "canbus", "can_", ".can.", "mcu", "hcn", "time", "curr",
    )
    internal val actionLike = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+){2,}$")
    private val constLike = Regex("^[A-Z][A-Z0-9_]{5,}$")

    fun scan(context: Context): Result = Result(scanApps(context))

    private fun scanApps(context: Context): List<AppHits> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0).filter { ai ->
            val p = ai.packageName.lowercase()
            val system = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0
            p.startsWith("com.hcn.") || (system && (p.contains("music") || p.contains("media") || p.contains("can"))) &&
                !p.startsWith("com.android.providers") && !p.startsWith("com.google")
        }.sortedBy { if (it.packageName.startsWith("com.hcn.")) 0 else 1 }

        return apps.map { ai ->
            val strings = runCatching { dexStrings(ai.sourceDir) }.getOrDefault(emptySet())
            val hits = strings.filter { s ->
                val l = s.lowercase()
                (actionLike.matches(s) || constLike.matches(s) || l.startsWith("persist.") || l.startsWith("sys.")) &&
                    keywords.any { l.contains(it) } &&
                    !l.startsWith("android.support") && !l.startsWith("androidx.") && !l.startsWith("kotlin") &&
                    !l.startsWith("com.google") && !l.startsWith("java.") && !l.startsWith("org.") &&
                    !l.endsWith(".java") && !l.contains("/")
            }.sorted().take(600)
            AppHits(ai.packageName, pm.getApplicationLabel(ai).toString(), hits)
        }
    }

    /** Reads every string_data_item of all classesN.dex files in the APK. */
    private fun dexStrings(apkPath: String): Set<String> {
        val out = HashSet<String>()
        ZipFile(apkPath).use { zip ->
            java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { entry ->
                val bytes = zip.getInputStream(entry).use { DataInputStream(it).readBytes() }
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val count = buf.getInt(0x38)
                val idsOff = buf.getInt(0x3C)
                for (i in 0 until count) {
                    var p = buf.getInt(idsOff + i * 4)
                    // skip uleb128 utf16 length
                    while (bytes[p].toInt() and 0x80 != 0) p++
                    p++
                    var end = p
                    while (end < bytes.size && bytes[end].toInt() != 0) end++
                    val len = end - p
                    if (len in 6..120) out += String(bytes, p, len, Charsets.UTF_8)
                }
            }
        }
        return out
    }
}
