package com.omar.nfctoolkit.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.omar.nfctoolkit.util.toHex

/** Low-level read/write operations against a discovered [Tag]. Runs on the NFC callback thread. */
object NfcOps {

    fun readTag(tag: Tag): TagInfo {
        val uid = tag.id.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }
        val extra = linkedMapOf<String, String>()
        var typeLabel = "Unknown"
        var maxSize: Int? = null
        var writable: Boolean? = null
        var records: List<ParsedRecord> = emptyList()
        var rawNdef: ByteArray? = null

        NfcA.get(tag)?.let { a ->
            runCatching { extra["ATQA"] = a.atqa.toHex() }
            runCatching { extra["SAK"] = String.format("0x%02X", a.sak) }
        }
        IsoDep.get(tag)?.let { iso ->
            typeLabel = "ISO-DEP (Type 4)"
            runCatching { iso.historicalBytes?.takeIf { it.isNotEmpty() }?.let { extra["Historical"] = it.toHex() } }
        }
        NfcF.get(tag)?.let { typeLabel = "FeliCa (Type 3)" }
        NfcV.get(tag)?.let { typeLabel = "ISO 15693 (Type 5)" }
        MifareUltralight.get(tag)?.let { ul ->
            typeLabel = when (ul.type) {
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight"
            }
        }
        MifareClassic.get(tag)?.let { mc ->
            typeLabel = "MIFARE Classic " + when (mc.size) {
                MifareClassic.SIZE_1K -> "1K"
                MifareClassic.SIZE_2K -> "2K"
                MifareClassic.SIZE_4K -> "4K"
                MifareClassic.SIZE_MINI -> "Mini"
                else -> "${mc.size} B"
            }
            extra["Sectors"] = mc.sectorCount.toString()
            extra["Blocks"] = mc.blockCount.toString()
        }
        Ndef.get(tag)?.let { ndef ->
            maxSize = ndef.maxSize
            writable = ndef.isWritable
            extra["NDEF type"] = ndef.type
            val msg = ndef.cachedNdefMessage ?: runCatching {
                ndef.connect(); ndef.ndefMessage
            }.getOrNull().also { runCatching { ndef.close() } }
            if (msg != null) {
                rawNdef = msg.toByteArray()
                records = NdefUtils.parse(msg)
            }
            if (typeLabel == "Unknown") typeLabel = "NDEF Tag"
        }
        if (typeLabel == "Unknown" && techs.any { it == "NfcA" }) typeLabel = "ISO 14443-A"

        return TagInfo(uid, techs, typeLabel, maxSize, writable, records, rawNdef, extra)
    }

    fun writeNdef(tag: Tag, bytes: ByteArray): WriteResult {
        return try {
            val msg = NdefMessage(bytes)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close(); return WriteResult(false, "This tag is read-only")
                }
                if (ndef.maxSize < bytes.size) {
                    ndef.close()
                    return WriteResult(false, "Tag too small: ${ndef.maxSize} B capacity, need ${bytes.size} B")
                }
                ndef.writeNdefMessage(msg)
                ndef.close()
                WriteResult(true, "Wrote ${bytes.size} bytes")
            } else {
                val form = NdefFormatable.get(tag)
                    ?: return WriteResult(false, "Tag is not NDEF-compatible")
                form.connect()
                form.format(msg)
                form.close()
                WriteResult(true, "Formatted and wrote ${bytes.size} bytes")
            }
        } catch (e: Exception) {
            WriteResult(false, e.message ?: "Write failed")
        }
    }

    fun makeReadOnly(tag: Tag): WriteResult {
        return try {
            val ndef = Ndef.get(tag) ?: return WriteResult(false, "Not an NDEF tag")
            ndef.connect()
            val ok = ndef.makeReadOnly()
            ndef.close()
            WriteResult(ok, if (ok) "Tag locked (now permanently read-only)" else "This tag can't be locked")
        } catch (e: Exception) {
            WriteResult(false, e.message ?: "Lock failed")
        }
    }
}
