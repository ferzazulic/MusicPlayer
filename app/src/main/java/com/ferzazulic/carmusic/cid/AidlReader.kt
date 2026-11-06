package com.ferzazulic.carmusic.cid

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * Minimal DEX reader that recovers AIDL interfaces from an installed APK: for each interface
 * (e.g. com.hcn.mediaservice.api.IRemotePlayerService) it lists the abstract methods with their
 * parameter/return types and the transaction code from the generated Stub's TRANSACTION_* constants.
 * Read-only – nothing is loaded or executed.
 */
object AidlReader {

    data class Method(val code: Int?, val name: String, val params: List<String>, val ret: String)
    data class Iface(val name: String, val apk: String, val methods: List<Method>)

    private val targets = Regex("^Lcom/hcn/(media|mediaservice)/.*I[A-Z][A-Za-z0-9]*;$")

    fun read(context: Context, packages: List<String>): List<Iface> {
        val out = ArrayList<Iface>()
        for (pkg in packages) {
            val apk = runCatching { context.packageManager.getApplicationInfo(pkg, 0).sourceDir }.getOrNull() ?: continue
            runCatching {
                ZipFile(apk).use { zip ->
                    java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { e ->
                        val bytes = zip.getInputStream(e).use { it.readBytes() }
                        out += Dex(bytes).interfaces(pkg)
                    }
                }
            }
        }
        // keep the richest copy of each interface
        return out.groupBy { it.name }.map { (_, v) -> v.maxBy { it.methods.size } }.sortedBy { it.name }
    }

    fun asText(list: List<Iface>): String = buildString {
        list.forEach { i ->
            appendLine("== ${i.name}  (${i.apk})")
            i.methods.sortedWith(compareBy({ it.code ?: Int.MAX_VALUE }, { it.name })).forEach { m ->
                appendLine("  ${m.code ?: "?"}: ${m.ret} ${m.name}(${m.params.joinToString(", ")})")
            }
        }
    }

    /**
     * Code-level details needed to talk to the media service:
     *  - field list and writeToParcel() order of the Parcelables (MusicInfo, MediaPlayInfo)
     *  - every place in the stock player / media service that calls the key AIDL methods, with the
     *    string constants used in that method (client names, event names).
     */
    fun analyze(context: Context): String = buildString {
        val pkgs = listOf("com.hcn.AutoMediaPlayer", "com.hcn.mediaservice")
        val parcelables = listOf(
            "Lcom/hcn/mediaservice/data/MusicInfo;",
            "Lcom/hcn/media/base/MediaPlayInfo;",
        )
        val calls = setOf("attachMediaClient", "setClientBinder", "updateMusicPlayInfo", "bindMediaClient", "scheduleTask", "setMediaCallback", "requestExecuteMusicApi")
        val doneParcel = HashSet<String>()
        for (pkg in pkgs) {
            val apk = runCatching { context.packageManager.getApplicationInfo(pkg, 0).sourceDir }.getOrNull() ?: continue
            runCatching {
                ZipFile(apk).use { zip ->
                    java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { e ->
                        val dex = Dex(zip.getInputStream(e).use { it.readBytes() })
                        for (pc in parcelables) {
                            if (pc in doneParcel) continue
                            dex.parcelLayout(pc)?.let { appendLine("== PARCEL ${dex.pretty(pc)} ($pkg)"); append(it); doneParcel += pc }
                        }
                        dex.callSites(calls).forEach { appendLine("== POZIV ($pkg) $it") }
                    }
                }
            }.onFailure { appendLine("greška $pkg: ${it.message}") }
        }
    }

    /**
     * The firmware's system "sourceservice" (android.sourceservice.* in framework.jar): its classes,
     * method signatures and constants, plus what the stock player and the CAN app call on it.
     */
    fun sourceService(context: Context): String = buildString {
        val prefix = "Landroid/sourceservice"
        val jars = java.io.File("/system/framework").listFiles { f -> f.name.endsWith(".jar") }.orEmpty()
            .sortedBy { if (it.name == "framework.jar") 0 else 1 }
        var found = false
        for (jar in jars) {
            runCatching {
                ZipFile(jar).use { zip ->
                    java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { e ->
                        val dex = Dex(zip.getInputStream(e).use { it.readBytes() })
                        dex.parcelLayout("Landroid/sourceservice/MusicInfo;")?.let { appendLine("== PARCEL android.sourceservice.MusicInfo (${jar.name})"); append(it) }
                        val dump = if (prefix.isNotEmpty()) "" else dex.classDump(prefix)
                        if (dump.isNotBlank()) { found = true; appendLine("== ${jar.name}/${e.name}"); append(dump) }
                    }
                }
            }
        }
        if (!found) appendLine("android.sourceservice nije pronađen u /system/framework (${jars.size} jar fajlova)")
        for (pkg in listOf("com.hcn.AutoMediaPlayer", "com.hcn.autocan", "com.hcn.mediaservice", "com.hcn.autosource")) {
            val apk = runCatching { context.packageManager.getApplicationInfo(pkg, 0).sourceDir }.getOrNull() ?: continue
            runCatching {
                ZipFile(apk).use { zip ->
                    java.util.Collections.list(zip.entries()).filter { it.name.matches(Regex("classes\\d*\\.dex")) }.forEach { e ->
                        Dex(zip.getInputStream(e).use { it.readBytes() }).callersOf(prefix).forEach { appendLine("== KORISTI ($pkg) $it") }
                    }
                }
            }
        }
        // system services the app can see by name
        val names = listOf("sourceservice", "sourceservices", "source", "hcnsource", "mcu", "canbus", "carservice")
        names.forEach { n -> appendLine("getSystemService(\"$n\") = ${runCatching { context.getSystemService(n)?.javaClass?.name }.getOrElse { "greška ${it.javaClass.simpleName}" }}") }
        runCatching {
            val sm = Class.forName("android.os.ServiceManager")
            @Suppress("UNCHECKED_CAST")
            val list = (sm.getMethod("listServices").invoke(null) as Array<String>).filter { s ->
                val l = s.lowercase(); listOf("source", "hcn", "mcu", "can", "media").any { l.contains(it) }
            }
            appendLine("ServiceManager: " + list.joinToString(", "))
        }.onFailure { appendLine("ServiceManager.listServices nedostupan: ${it.javaClass.simpleName}") }
    }

    private class Dex(val b: ByteArray) {
        val buf: ByteBuffer = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val stringIdsOff = buf.getInt(0x3C)
        val typeIdsOff = buf.getInt(0x44)
        val protoIdsOff = buf.getInt(0x4C)
        val fieldIdsOff = buf.getInt(0x54)
        val methodIdsOff = buf.getInt(0x5C)
        val classDefsSize = buf.getInt(0x60)
        val classDefsOff = buf.getInt(0x64)

        private var pos = 0
        private fun uleb(): Int {
            var result = 0
            var shift = 0
            while (true) {
                val v = b[pos++].toInt() and 0xFF
                result = result or ((v and 0x7F) shl shift)
                if (v and 0x80 == 0) return result
                shift += 7
            }
        }

        fun string(idx: Int): String {
            var p = buf.getInt(stringIdsOff + idx * 4)
            while (b[p].toInt() and 0x80 != 0) p++
            p++
            var end = p
            while (b[end].toInt() != 0) end++
            return String(b, p, end - p, Charsets.UTF_8)
        }

        fun type(idx: Int): String = string(buf.getInt(typeIdsOff + idx * 4))
        private fun ushort(off: Int) = buf.getShort(off).toInt() and 0xFFFF

        fun methodName(idx: Int) = string(buf.getInt(methodIdsOff + idx * 8 + 4))
        fun methodClass(idx: Int) = type(ushort(methodIdsOff + idx * 8))
        fun proto(methodIdx: Int): Pair<List<String>, String> {
            val protoIdx = ushort(methodIdsOff + methodIdx * 8 + 2)
            val base = protoIdsOff + protoIdx * 12
            val ret = type(buf.getInt(base + 4))
            val paramsOff = buf.getInt(base + 8)
            val params = if (paramsOff == 0) emptyList() else (0 until buf.getInt(paramsOff)).map { type(ushort(paramsOff + 4 + it * 2)) }
            return params.map(::pretty) to pretty(ret)
        }

        fun fieldName(idx: Int) = string(buf.getInt(fieldIdsOff + idx * 8 + 4))

        fun pretty(t: String): String = when {
            t.startsWith("[") -> pretty(t.substring(1)) + "[]"
            t == "I" -> "int"; t == "J" -> "long"; t == "Z" -> "boolean"; t == "V" -> "void"
            t == "F" -> "float"; t == "D" -> "double"; t == "B" -> "byte"; t == "S" -> "short"; t == "C" -> "char"
            t.startsWith("L") -> t.substring(1, t.length - 1).replace('/', '.').removePrefix("java.lang.")
            else -> t
        }

        private fun unit(off: Int) = ushort(off)

        /** Width of the instruction at [insns]+[i] in 16-bit code units. */
        private fun width(insns: Int, i: Int): Int {
            val u = unit(insns + i * 2)
            val op = u and 0xFF
            if (op == 0x00) {
                return when (u) {
                    0x0100 -> 4 + unit(insns + (i + 1) * 2) * 2
                    0x0200 -> 2 + unit(insns + (i + 1) * 2) * 4
                    0x0300 -> {
                        val w = unit(insns + (i + 1) * 2)
                        val n = buf.getInt(insns + (i + 2) * 2)
                        4 + (n * w + 1) / 2
                    }
                    else -> 1
                }
            }
            return when (op) {
                0x01, 0x04, 0x07, in 0x0a..0x12, in 0x1d..0x1e, 0x21, 0x27, 0x28, in 0x3e..0x43, 0x73, in 0x79..0x8f, in 0xb0..0xcf, in 0xe3..0xf9 -> 1
                0x02, 0x05, 0x08, 0x13, 0x15, 0x16, 0x19, 0x1a, 0x1c, 0x1f, 0x20, 0x22, 0x23, 0x29, in 0x2d..0x3d, in 0x44..0x6d, in 0x90..0xaf, in 0xd0..0xe2, 0xfe, 0xff -> 2
                0x03, 0x06, 0x09, 0x14, 0x17, 0x1b, 0x24, 0x25, 0x26, 0x2a, 0x2b, 0x2c, in 0x6e..0x72, in 0x74..0x78, 0xfc, 0xfd -> 3
                0xfa, 0xfb -> 4
                0x18 -> 5
                else -> 1
            }
        }

        private fun fieldInfo(idx: Int): Pair<String, String> {
            val o = fieldIdsOff + idx * 8
            return string(buf.getInt(o + 4)) to pretty(type(ushort(o + 2)))
        }

        /** (methodIdx, codeOff) for all direct + virtual methods of a class. */
        private fun methods(classDataOff: Int): List<Pair<Int, Int>> {
            if (classDataOff == 0) return emptyList()
            pos = classDataOff
            val sf = uleb(); val inf = uleb(); val dm = uleb(); val vm = uleb()
            repeat(sf + inf) { uleb(); uleb() }
            val out = ArrayList<Pair<Int, Int>>()
            var idx = 0
            repeat(dm) { idx += uleb(); uleb(); out += idx to uleb() }
            idx = 0
            repeat(vm) { idx += uleb(); uleb(); out += idx to uleb() }
            return out
        }

        private fun instanceFields(classDataOff: Int): List<Pair<String, String>> {
            if (classDataOff == 0) return emptyList()
            pos = classDataOff
            val sf = uleb(); val inf = uleb(); uleb(); uleb()
            repeat(sf) { uleb(); uleb() }
            var idx = 0
            val out = ArrayList<Pair<String, String>>()
            repeat(inf) { idx += uleb(); uleb(); out += fieldInfo(idx) }
            return out
        }

        /** Walks a method body and reports field reads, Parcel calls, other invokes and string constants. */
        private fun trace(codeOff: Int, full: Boolean = false, onStep: (String) -> Unit) {
            if (codeOff == 0) return
            val n = buf.getInt(codeOff + 12)
            val insns = codeOff + 16
            var i = 0
            while (i < n) {
                val op = unit(insns + i * 2) and 0xFF
                when (op) {
                    in 0x52..0x58 -> { val (name, t) = fieldInfo(unit(insns + (i + 1) * 2)); onStep("lese $name:$t") }
                    0x1a -> onStep("\"" + string(unit(insns + (i + 1) * 2)) + "\"")
                    0x1b -> onStep("\"" + string(buf.getInt(insns + (i + 1) * 2)) + "\"")
                    in 0x6e..0x72, in 0x74..0x78 -> {
                        val m = unit(insns + (i + 1) * 2)
                        val cn = pretty(methodClass(m))
                        onStep("${if (full) cn else cn.substringAfterLast('.')}.${methodName(m)}")
                    }
                }
                i += width(insns, i)
            }
        }

        fun parcelLayout(desc: String): String? {
            val def = (0 until classDefsSize).map { classDefsOff + it * 32 }.firstOrNull { type(buf.getInt(it)) == desc } ?: return null
            val classData = buf.getInt(def + 24)
            val sb = StringBuilder()
            sb.appendLine("  polja: " + instanceFields(classData).joinToString(", ") { "${it.first}:${it.second}" })
            methods(classData).forEach { (mi, code) ->
                val name = methodName(mi)
                if (name == "writeToParcel" || name == "readFromParcel" || (name == "<init>" && proto(mi).first.any { it.endsWith("Parcel") })) {
                    val steps = ArrayList<String>()
                    trace(code) { st -> if (st.startsWith("lese") || st.startsWith("Parcel.")) steps += st.removePrefix("lese ") }
                    sb.appendLine("  $name: " + steps.joinToString(" → "))
                }
            }
            return sb.toString()
        }

        fun callSites(targets: Set<String>): List<String> {
            val out = ArrayList<String>()
            for (c in 0 until classDefsSize) {
                val o = classDefsOff + c * 32
                val cls = type(buf.getInt(o))
                if (!cls.startsWith("Lcom/hcn/")) continue
                val classData = buf.getInt(o + 24)
                for ((mi, code) in methods(classData)) {
                    val steps = ArrayList<String>()
                    var hit = false
                    trace(code) { st ->
                        steps += st
                        if (targets.any { t -> st.endsWith(".$t") } && !st.contains("Stub")) hit = true
                    }
                    val impl = !hit && methodName(mi) in setOf("updateMusicPlayInfo", "updateVideoPlayInfo", "attachMediaClient") &&
                        !cls.endsWith("Default;") && !cls.endsWith("Proxy;") && steps.isNotEmpty()
                    if (impl) {
                        out += "IMPL ${pretty(cls)}.${methodName(mi)}: " + steps.filter { !it.startsWith("lese") || true }.take(60).joinToString(" | ")
                        continue
                    }
                    if (hit && !cls.contains("\$Stub")) {
                        val compact = steps.filter { st -> st.startsWith("\"") || targets.any { t -> st.endsWith(".$t") } || st.contains("Parcel.") }
                        out += "${pretty(cls)}.${methodName(mi)}: " + compact.take(40).joinToString(" | ")
                    }
                }
            }
            return out.take(40)
        }

        /** Methods (with signatures) and static constants of every class whose descriptor starts with [prefix]. */
        fun classDump(prefix: String): String {
            val sb = StringBuilder()
            for (c in 0 until classDefsSize) {
                val o = classDefsOff + c * 32
                val desc = type(buf.getInt(o))
                if (!desc.startsWith(prefix)) continue
                val classData = buf.getInt(o + 24)
                val staticOff = buf.getInt(o + 28)
                sb.appendLine("class ${pretty(desc)}")
                // static fields + values
                if (classData != 0) {
                    pos = classData
                    val sf = uleb(); uleb(); uleb(); uleb()
                    val fields = ArrayList<Int>()
                    var idx = 0
                    repeat(sf) { idx += uleb(); uleb(); fields += idx }
                    val values = ArrayList<String>()
                    if (staticOff != 0) {
                        pos = staticOff
                        val n = uleb()
                        repeat(n) { values += valueText() }
                    }
                    fields.forEachIndexed { i, f ->
                        val (name, t) = fieldInfo(f)
                        sb.appendLine("  const $t $name = ${values.getOrNull(i) ?: "?"}")
                    }
                }
                methods(classData).forEach { (mi, _) ->
                    val (params, ret) = proto(mi)
                    sb.appendLine("  $ret ${methodName(mi)}(${params.joinToString(", ")})")
                }
            }
            return sb.toString()
        }

        /** Every com.hcn method that calls into a class whose descriptor starts with [prefix]. */
        fun callersOf(prefix: String): List<String> {
            val target = pretty(prefix.removeSuffix("/") + ";").removeSuffix(";")
            val out = ArrayList<String>()
            for (c in 0 until classDefsSize) {
                val o = classDefsOff + c * 32
                val cls = type(buf.getInt(o))
                if (!cls.startsWith("Lcom/hcn/")) continue
                for ((mi, code) in methods(buf.getInt(o + 24))) {
                    val steps = ArrayList<String>()
                    var hit = false
                    trace(code, full = true) { st ->
                        if (st.startsWith(target)) { hit = true; steps += st.removePrefix("android.") }
                        else if (st.startsWith("\"")) steps += st
                    }
                    if (hit) out += "${pretty(cls)}.${methodName(mi)}: " + steps.take(30).joinToString(" | ")
                }
            }
            return out.take(60)
        }

        private fun valueText(): String {
            val head = b[pos++].toInt() and 0xFF
            val type = head and 0x1F
            val arg = head shr 5
            fun num(): Long { var v = 0L; for (k in 0..arg) v = v or ((b[pos + k].toLong() and 0xFF) shl (8 * k)); pos += arg + 1; return v }
            return when (type) {
                0x00, 0x02, 0x03, 0x04, 0x06 -> {
                    var v = num(); val bits = 8 * (arg + 1)
                    if (type != 0x03 && bits < 64 && (v shr (bits - 1)) and 1L == 1L) v = v or (-1L shl bits)
                    v.toString()
                }
                0x17 -> "\"" + string(num().toInt()) + "\""
                0x18 -> pretty(type(num().toInt()))
                0x10, 0x11, 0x15, 0x16, 0x19, 0x1A, 0x1B -> { num(); "…" }
                0x1C -> { val n = uleb(); repeat(n) { valueText() }; "[…]" }
                0x1D -> { uleb(); val n = uleb(); repeat(n) { uleb(); valueText() }; "@…" }
                0x1E -> "null"
                0x1F -> (arg != 0).toString()
                else -> "?"
            }
        }

        fun interfaces(apk: String): List<Iface> {
            // collect class defs
            data class Def(val desc: String, val classDataOff: Int, val staticValuesOff: Int)
            val defs = (0 until classDefsSize).map { i ->
                val o = classDefsOff + i * 32
                Def(type(buf.getInt(o)), buf.getInt(o + 24), buf.getInt(o + 28))
            }
            val byDesc = defs.associateBy { it.desc }
            return defs.filter { targets.matches(it.desc) && !it.desc.contains('$') }.map { iface ->
                val codes = HashMap<String, Int>()
                byDesc[iface.desc.removeSuffix(";") + "\$Stub;"]?.let { stub -> codes += stubCodes(stub.classDataOff, stub.staticValuesOff) }
                val methods = virtualMethods(iface.classDataOff).map { mi ->
                    val (params, ret) = proto(mi)
                    val name = methodName(mi)
                    Method(codes["TRANSACTION_$name"], name, params, ret)
                }
                Iface(pretty(iface.desc), apk, methods)
            }
        }

        private fun virtualMethods(classDataOff: Int): List<Int> {
            if (classDataOff == 0) return emptyList()
            pos = classDataOff
            val sf = uleb(); val inf = uleb(); val dm = uleb(); val vm = uleb()
            repeat(sf + inf) { uleb(); uleb() }
            repeat(dm) { uleb(); uleb(); uleb() }
            val out = ArrayList<Int>()
            var idx = 0
            repeat(vm) {
                idx += uleb(); uleb(); uleb()
                out += idx
            }
            return out
        }

        /** Static int fields TRANSACTION_* of the Stub with their initial values. */
        private fun stubCodes(classDataOff: Int, staticValuesOff: Int): Map<String, Int> {
            if (classDataOff == 0 || staticValuesOff == 0) return emptyMap()
            pos = classDataOff
            val sf = uleb(); uleb(); uleb(); uleb()
            val fields = ArrayList<Int>()
            var idx = 0
            repeat(sf) { idx += uleb(); uleb(); fields += idx }
            pos = staticValuesOff
            val n = uleb()
            val out = HashMap<String, Int>()
            for (i in 0 until n) {
                val v = readValue()
                if (i < fields.size && v != null) {
                    val name = fieldName(fields[i])
                    if (name.startsWith("TRANSACTION_")) out[name] = v
                }
            }
            return out
        }

        /** Reads one encoded_value; returns it if it is an int-like value. */
        private fun readValue(): Int? {
            val head = b[pos++].toInt() and 0xFF
            val type = head and 0x1F
            val arg = head shr 5
            return when (type) {
                0x00, 0x02, 0x03, 0x04 -> {
                    var v = 0L
                    for (k in 0..arg) v = v or ((b[pos + k].toLong() and 0xFF) shl (8 * k))
                    pos += arg + 1
                    // sign-extend
                    val bits = 8 * (arg + 1)
                    if (type != 0x03 && bits < 64 && (v shr (bits - 1)) and 1L == 1L) v = v or (-1L shl bits)
                    v.toInt()
                }
                0x06, 0x10, 0x11, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B -> { pos += arg + 1; null }
                0x1C -> { val n = uleb(); repeat(n) { readValue() }; null }
                0x1D -> { uleb(); val n = uleb(); repeat(n) { uleb(); readValue() }; null }
                else -> null // 0x1E null, 0x1F boolean: no payload
            }
        }
    }
}
