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
