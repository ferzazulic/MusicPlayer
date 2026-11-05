package com.ferzazulic.carmusic.cid

/**
 * Opel Astra H / Zafira B / Corsa D "radio -> display" text messages on the MS-CAN bus (95.238 kbit/s,
 * OBD pins 3 = MS-CAN high and 11 = MS-CAN low).
 *
 * The factory CD30/CD70 radio sends ISO-TP (ISO 15765-2) segmented frames on 0x6C1 and the display
 * answers with flow-control frames on 0x2C1. The payload layout below follows community reverse
 * engineering of the factory radio and is marked EXPERIMENTAL – field ids may differ between
 * display variants (GID / CID / colour CID), which is why the CAN ids are configurable in settings.
 *
 * Payload: 0x40 0x00 <len> 0x03 { <fieldId> <charCount> <UTF-16BE chars...> } x3
 */
object AstraCidProtocol {

    fun buildTextPayload(text: CidText): ByteArray {
        val fields = listOf(0x10 to text.line1, 0x11 to text.line2, 0x12 to text.line3)
        val body = ArrayList<Byte>()
        body += 0x03 // number of fields
        for ((id, value) in fields) {
            val chars = value.take(30)
            body += id.toByte()
            body += chars.length.toByte()
            for (c in chars) {
                body += (c.code shr 8).toByte()
                body += (c.code and 0xFF).toByte()
            }
        }
        val out = ArrayList<Byte>()
        out += 0x40
        out += 0x00
        out += body.size.toByte()
        out += body
        return out.toByteArray()
    }

    /** Splits a payload into classic ISO-TP CAN frames (8 data bytes each). */
    fun segment(payload: ByteArray): List<ByteArray> {
        if (payload.size <= 7) {
            return listOf(byteArrayOf(payload.size.toByte()) + payload + ByteArray(7 - payload.size))
        }
        val frames = ArrayList<ByteArray>()
        val len = payload.size
        frames += byteArrayOf((0x10 or ((len shr 8) and 0x0F)).toByte(), (len and 0xFF).toByte()) + payload.copyOfRange(0, 6)
        var offset = 6
        var seq = 1
        while (offset < len) {
            val chunk = payload.copyOfRange(offset, minOf(offset + 7, len))
            frames += byteArrayOf((0x20 or (seq and 0x0F)).toByte()) + chunk + ByteArray(7 - chunk.size)
            offset += 7
            seq++
        }
        return frames
    }

    /** SLCAN / Lawicel ASCII frame: tIIILDDDDDDDDDDDDDDDD\r */
    fun slcanFrame(id: Int, data: ByteArray): String = buildString {
        append('t')
        append("%03X".format(id and 0x7FF))
        append(data.size)
        data.forEach { append("%02X".format(it.toInt() and 0xFF)) }
        append('\r')
    }

    /** Bit-rate command. 95.238 kbit/s is non-standard, so an SJA1000 BTR value is used (16 MHz clock). */
    fun bitrateCommand(kbit: Int): String = when (kbit) {
        95 -> "s034E\r"
        100 -> "S3\r"
        125 -> "S4\r"
        250 -> "S5\r"
        500 -> "S6\r"
        else -> "S3\r"
    }
}
