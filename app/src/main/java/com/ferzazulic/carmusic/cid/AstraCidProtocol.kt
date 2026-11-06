package com.ferzazulic.carmusic.cid

/**
 * Opel Astra H / Zafira B / Corsa D "radio -> display" text messages on MS-CAN
 * (95.238 kbit/s, OBD pin 3 = MS-CAN high, pin 11 = MS-CAN low).
 *
 * Based on the community documentation (Car-CAN-Message-DB, Opel/Astra/H/MS-CAN/Info-Display.md)
 * and the "Opel Astra H GID tester" sketch:
 *  - The head unit announces itself on 0x691 every ~2.5 s; without it the display ignores audio text.
 *  - Text goes to 0x6C1 as ISO-TP: first frame, wait for flow control (0x30 ..) on 0x2C1, then
 *    consecutive frames 0x21..0x2F.
 *  - Payload: C0 00 <size> <type> { <fieldId> <charCount> <UTF-16BE chars> }...
 *    size = number of bytes after the size byte; type 0x03 = main audio screen.
 *    Fields for type 0x03: 0x10 title, 0x11 album, 0x12 artist, 0x23 source/menu label.
 */
object AstraCidProtocol {
    const val TYPE_AUDIO = 0x03
    const val FIELD_TITLE = 0x10
    const val FIELD_ALBUM = 0x11
    const val FIELD_ARTIST = 0x12
    const val FIELD_SOURCE = 0x23
    const val FIELD_HEADER = 0x01

    const val ID_RADIO_PRESENCE = 0x691
    /** "Head unit on" announcement as sent by the factory radio. */
    val RADIO_PRESENCE = byteArrayOf(0x41, 0x00, 0x60, 0x02, 0x82.toByte(), 0x00, 0x00, 0x2E)

    private const val MAX_CHARS = 30

    fun buildTextPayload(title: String, artist: String, album: String, source: String): ByteArray {
        val fields = listOf(
            FIELD_HEADER to " ",
            FIELD_SOURCE to source,
            FIELD_TITLE to title,
            FIELD_ALBUM to album,
            FIELD_ARTIST to artist,
        )
        val body = ArrayList<Byte>()
        body += TYPE_AUDIO.toByte()
        for ((id, raw) in fields) {
            val value = sanitize(raw).ifEmpty { " " }.take(MAX_CHARS)
            body += id.toByte()
            body += value.length.toByte()
            for (c in value) {
                body += (c.code shr 8).toByte()
                body += (c.code and 0xFF).toByte()
            }
        }
        return byteArrayOf(0xC0.toByte(), 0x00, body.size.toByte()) + body.toByteArray()
    }

    /** The display only renders basic Latin-1; map Bosnian/Croatian letters to the closest ASCII. */
    fun sanitize(s: String): String = buildString {
        for (c in s) {
            append(
                when (c) {
                    'č', 'ć' -> 'c'; 'Č', 'Ć' -> 'C'; 'š' -> 's'; 'Š' -> 'S'; 'ž' -> 'z'; 'Ž' -> 'Z'
                    'đ' -> 'd'; 'Đ' -> 'D'; '▶' -> '>'; '❙' -> '|'; '…' -> '.'
                    else -> if (c.code in 0x20..0xFF) c else '?'
                }
            )
        }
    }

    /** First frame + consecutive frames (ISO 15765-2, 8 data bytes, padded). */
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

    /** SLCAN / Lawicel ASCII frame: tIIILDD..\r */
    fun slcanFrame(id: Int, data: ByteArray): String = buildString {
        append('t')
        append("%03X".format(id and 0x7FF))
        append(data.size)
        data.forEach { append("%02X".format(it.toInt() and 0xFF)) }
        append('\r')
    }

    /** Matches a received flow-control frame "t2C1830..." for the given id. */
    fun isFlowControl(line: String, fcId: Int): Boolean =
        line.length >= 7 && line[0] == 't' && line.substring(1, 4).equals("%03X".format(fcId), true) && line.substring(5, 7).startsWith("3")

    /** 95.238 kbit/s is non-standard: SJA1000-style BTR value (16 MHz clock); others are standard S-codes. */
    fun bitrateCommand(kbit: Int): String = when (kbit) {
        95 -> "s034E\r"
        100 -> "S3\r"
        125 -> "S4\r"
        250 -> "S5\r"
        500 -> "S6\r"
        else -> "S3\r"
    }
}
