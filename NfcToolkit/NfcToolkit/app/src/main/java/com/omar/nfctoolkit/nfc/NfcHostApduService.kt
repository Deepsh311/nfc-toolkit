package com.omar.nfctoolkit.nfc

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.omar.nfctoolkit.util.hexToBytes

/**
 * Emulates an NFC Forum Type 4 Tag over Host Card Emulation, presenting whichever
 * saved NDEF record the user marked "active". A reader (or another phone) can read
 * this phone as if it were an NDEF tag.
 *
 * Note on scope: Android HCE only speaks ISO-DEP (ISO 14443-4) for registered AIDs.
 * It cannot spoof a card's UID or emulate MIFARE Classic / 125 kHz fobs, so this is
 * strictly NDEF-content sharing, not access-credential cloning.
 */
class NfcHostApduService : HostApduService() {

    private var selectedFile = 0 // 0 = none, 1 = CC, 2 = NDEF
    private var ndefFile = ByteArray(0)
    private var ccFile = ByteArray(0)

    private val selectNdefApp = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
    )
    private val swOk = byteArrayOf(0x90.toByte(), 0x00)
    private val swNotFound = byteArrayOf(0x6A, 0x82.toByte())
    private val ccFid = byteArrayOf(0xE1.toByte(), 0x03)
    private val ndefFid = byteArrayOf(0xE1.toByte(), 0x04)

    private fun loadActivePayload() {
        val hex = getSharedPreferences("hce", Context.MODE_PRIVATE)
            .getString("active_ndef_hex", null)
        val msg = hex?.hexToBytes() ?: ByteArray(0)
        val nlen = msg.size
        ndefFile = ByteArray(2 + nlen).also {
            it[0] = (nlen ushr 8).toByte()
            it[1] = nlen.toByte()
            System.arraycopy(msg, 0, it, 2, nlen)
        }
        val maxNdef = 0x7FFF
        ccFile = byteArrayOf(
            0x00, 0x0F,              // CCLEN = 15
            0x20,                    // mapping version 2.0
            0x00, 0x3B,              // MLe (max read)
            0x00, 0x34,              // MLc (max write)
            0x04, 0x06,              // NDEF File Control TLV
            0xE1.toByte(), 0x04,     // NDEF file id
            (maxNdef ushr 8).toByte(), maxNdef.toByte(),
            0x00,                    // read access granted
            0xFF.toByte()            // write access denied (read-only)
        )
    }

    override fun processCommandApdu(apdu: ByteArray?, extras: Bundle?): ByteArray {
        if (apdu == null) return swNotFound

        if (apdu.size >= selectNdefApp.size &&
            apdu.copyOfRange(0, selectNdefApp.size).contentEquals(selectNdefApp)
        ) {
            loadActivePayload()
            selectedFile = 0
            return swOk
        }

        // SELECT by file id: 00 A4 00 0C 02 <fid hi> <fid lo>
        if (apdu.size == 7 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x00.toByte() && apdu[3] == 0x0C.toByte() && apdu[4] == 0x02.toByte()
        ) {
            val fid = apdu.copyOfRange(5, 7)
            return when {
                fid.contentEquals(ccFid) -> { selectedFile = 1; swOk }
                fid.contentEquals(ndefFid) -> { selectedFile = 2; swOk }
                else -> swNotFound
            }
        }

        // READ BINARY: 00 B0 <off hi> <off lo> <le>
        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xB0.toByte()) {
            val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
            val le = apdu[4].toInt() and 0xFF
            val src = when (selectedFile) {
                1 -> ccFile
                2 -> ndefFile
                else -> return swNotFound
            }
            if (offset > src.size) return swNotFound
            val end = minOf(offset + le, src.size)
            return src.copyOfRange(offset, end) + swOk
        }

        return swNotFound
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = 0
    }
}
