package com.ferzazulic.carmusic.cid

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Identifies the head unit platform and its CAN-box apps, so the user can tell which mechanism the
 * unit uses to put text on the factory display (shown on the CID settings page).
 */
object HeadUnitInfo {
    private val platformHints = listOf(
        "com.hcn." to "HCN / Hengchen (Junsun i sl.) – CAN-box app com.hcn.autocan",
        "com.syu." to "FYT (UIS7862 / SC9853i…) – CAN-box app com.syu.canbus",
        "com.microntek" to "Microntek MTCD/MTCE (PX5/PX6)",
        "android.microntek" to "Microntek MTCD/MTCE (PX5/PX6)",
        "com.ts." to "TopWay TS10 / TS18",
        "com.hzbhd" to "HZBHD platforma",
        "com.qf." to "QF / Teyes slična platforma",
        "com.teyes" to "Teyes",
        "com.wits." to "WITS platforma",
        "com.nwd" to "NWD platforma",
        "com.zjinnova" to "ZJInnova (CarPlay/CAN modul)",
        "com.android.canbus" to "Generički CAN-box servis",
    )

    @SuppressLint("QueryPermissionsNeeded")
    fun describe(context: Context): String {
        val pm = context.packageManager
        val packages = runCatching {
            pm.getInstalledPackages(PackageManager.GET_META_DATA).map { it.packageName }
        }.getOrDefault(emptyList())
        val canRelated = packages.filter { p ->
            val l = p.lowercase()
            listOf("can", "syu", "microntek", "mcu", "carinfo", "carsetting", "hzbhd", "ts.", "teyes", "decoder", "raise", "hiworld", "xinbas", "simple")
                .any { l.contains(it) } && !l.startsWith("com.google") && !l.startsWith("com.android.providers")
        }.sorted()
        val platform = platformHints.firstOrNull { (prefix, _) -> packages.any { it.startsWith(prefix) } }?.second ?: "nepoznata"
        return buildString {
            appendLine("Uređaj: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Hardver: ${Build.HARDWARE} · Board: ${Build.BOARD}")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.DISPLAY}")
            appendLine("Platforma: $platform")
            append("CAN/MCU aplikacije: ")
            append(if (canRelated.isEmpty()) "nisu pronađene" else canRelated.joinToString(", "))
        }
    }
}
