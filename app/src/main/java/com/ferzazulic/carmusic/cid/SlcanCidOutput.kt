package com.ferzazulic.carmusic.cid

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import com.ferzazulic.carmusic.settings.AppSettings
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.util.concurrent.Executors

/**
 * Writes now-playing text directly to the Astra H CID through a USB CAN adapter running SLCAN
 * firmware (CANable, Lawicel CANUSB, Arduino + MCP2515 slcan sketch) connected to MS-CAN.
 */
class SlcanCidOutput(private val context: Context, private val onStatus: (String) -> Unit) {
    private val io = Executors.newSingleThreadExecutor()
    private var port: UsbSerialPort? = null
    private var openedBitrate = -1
    private var lastPresence = 0L
    private var sent = 0
    private var acked = 0

    private fun probers(): List<UsbSerialProber> {
        val custom = ProbeTable().apply {
            addProduct(0x0483, 0x5740, CdcAcmSerialDriver::class.java) // CANable slcan
            addProduct(0x16D0, 0x117E, CdcAcmSerialDriver::class.java) // CANable 2 slcan
            addProduct(0x2341, 0x0043, CdcAcmSerialDriver::class.java) // Arduino Uno
            addProduct(0x2341, 0x0001, CdcAcmSerialDriver::class.java) // Arduino Uno (old)
        }
        return listOf(UsbSerialProber.getDefaultProber(), UsbSerialProber(custom))
    }

    private fun ensureOpen(s: AppSettings): UsbSerialPort? {
        port?.let { if (openedBitrate == s.cidSlcanBitrate) return it }
        closeInternal()
        val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val driver = probers().firstNotNullOfOrNull { it.findAllDrivers(usb).firstOrNull() }
        if (driver == null) {
            onStatus("USB CAN adapter nije pronađen")
            return null
        }
        if (!usb.hasPermission(driver.device)) {
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent("com.ferzazulic.carmusic.USB_PERMISSION").setPackage(context.packageName), flags,
            )
            usb.requestPermission(driver.device, pi)
            onStatus("Čekam USB dozvolu…")
            return null
        }
        val conn = usb.openDevice(driver.device) ?: run { onStatus("USB uređaj se ne može otvoriti"); return null }
        return runCatching {
            val p = driver.ports.first()
            p.open(conn)
            p.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            runCatching { p.setDTR(true); p.setRTS(true) }
            p.write("C\r".toByteArray(), 200)
            p.write(AstraCidProtocol.bitrateCommand(s.cidSlcanBitrate).toByteArray(), 200)
            p.write("O\r".toByteArray(), 200)
            port = p
            openedBitrate = s.cidSlcanBitrate
            onStatus("Povezano: ${driver.device.productName ?: driver.device.deviceName}")
            p
        }.getOrElse {
            onStatus("Greška: ${it.message}")
            null
        }
    }

    fun send(title: String, artist: String, album: String, s: AppSettings) {
        io.execute {
            val p = ensureOpen(s) ?: return@execute
            runCatching {
                val now = System.currentTimeMillis()
                if (s.cidEmulateRadio && now - lastPresence > 2_000) {
                    p.write(AstraCidProtocol.slcanFrame(AstraCidProtocol.ID_RADIO_PRESENCE, AstraCidProtocol.RADIO_PRESENCE).toByteArray(), 100)
                    lastPresence = now
                }
                val frames = AstraCidProtocol.segment(AstraCidProtocol.buildTextPayload(title, artist, album, "MP3"))
                drain(p)
                p.write(AstraCidProtocol.slcanFrame(s.cidTxId, frames[0]).toByteArray(), 100)
                sent++
                if (frames.size > 1) {
                    if (waitForFlowControl(p, s.cidFcId, 250)) acked++
                    for (i in 1 until frames.size) {
                        p.write(AstraCidProtocol.slcanFrame(s.cidTxId, frames[i]).toByteArray(), 100)
                        Thread.sleep(2)
                    }
                }
                onStatus("Šaljem na CID · potvrde ekrana: $acked/$sent")
            }.onFailure {
                onStatus("Greška pri slanju: ${it.message}")
                closeInternal()
            }
        }
    }

    private fun drain(p: UsbSerialPort) {
        val buf = ByteArray(256)
        runCatching { while (p.read(buf, 5) > 0) Unit }
    }

    /** Reads SLCAN lines until the display's flow-control frame arrives or the timeout expires. */
    private fun waitForFlowControl(p: UsbSerialPort, fcId: Int, timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        val sb = StringBuilder()
        val buf = ByteArray(256)
        while (System.currentTimeMillis() < end) {
            val n = runCatching { p.read(buf, 20) }.getOrDefault(0)
            for (i in 0 until n) {
                val ch = buf[i].toInt().toChar()
                if (ch == '\r' || ch == '\n') {
                    if (AstraCidProtocol.isFlowControl(sb.toString(), fcId)) return true
                    sb.clear()
                } else sb.append(ch)
            }
        }
        return false
    }

    private fun closeInternal() {
        runCatching { port?.write("C\r".toByteArray(), 100) }
        runCatching { port?.close() }
        port = null
        openedBitrate = -1
    }

    fun close() {
        io.execute { closeInternal() }
    }
}
