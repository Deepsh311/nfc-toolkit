package com.omar.nfctoolkit.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord

object NdefUtils {

    // NFC Forum URI prefix abbreviation table.
    private val URI_PREFIX = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
        "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:",
        "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
        "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
    )

    // ---------- Parsing ----------

    fun parse(message: NdefMessage): List<ParsedRecord> = message.records.map { parseRecord(it) }

    private fun parseRecord(r: NdefRecord): ParsedRecord {
        return when (r.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> when {
                r.type.contentEquals(NdefRecord.RTD_TEXT) -> decodeText(r.payload)
                r.type.contentEquals(NdefRecord.RTD_URI) -> decodeUri(r.payload)
                r.type.contentEquals(NdefRecord.RTD_SMART_POSTER) -> decodeSmartPoster(r.payload)
                else -> ParsedRecord("Well-known", String(r.type, Charsets.US_ASCII))
            }
            NdefRecord.TNF_MIME -> decodeMime(r)
            NdefRecord.TNF_ABSOLUTE_URI -> ParsedRecord("URI", String(r.type, Charsets.UTF_8))
            NdefRecord.TNF_EXTERNAL_TYPE -> ParsedRecord(
                "External", String(r.payload, Charsets.UTF_8), String(r.type, Charsets.US_ASCII)
            )
            NdefRecord.TNF_EMPTY -> ParsedRecord("Empty", "(no content)")
            else -> ParsedRecord("Unknown", "${r.payload.size} bytes")
        }
    }

    private fun decodeText(p: ByteArray): ParsedRecord {
        if (p.isEmpty()) return ParsedRecord("Text", "")
        val status = p[0].toInt()
        val langLen = status and 0x3F
        val enc = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
        return try {
            val lang = String(p, 1, langLen, Charsets.US_ASCII)
            val text = String(p, 1 + langLen, p.size - 1 - langLen, enc)
            ParsedRecord("Text", text, "lang=$lang")
        } catch (e: Exception) {
            ParsedRecord("Text", "(unreadable)")
        }
    }

    private fun decodeUri(p: ByteArray): ParsedRecord {
        if (p.isEmpty()) return ParsedRecord("URI", "")
        val prefix = URI_PREFIX.getOrElse(p[0].toInt() and 0xFF) { "" }
        val rest = String(p, 1, p.size - 1, Charsets.UTF_8)
        return ParsedRecord("URI", prefix + rest)
    }

    private fun decodeSmartPoster(p: ByteArray): ParsedRecord {
        return try {
            val nested = NdefMessage(p).records.joinToString("\n") {
                val pr = parseRecord(it); "${pr.kind}: ${pr.content}"
            }
            ParsedRecord("Smart Poster", nested)
        } catch (e: Exception) {
            ParsedRecord("Smart Poster", "(nested content)")
        }
    }

    private fun decodeMime(r: NdefRecord): ParsedRecord {
        val mime = r.toMimeType() ?: String(r.type, Charsets.US_ASCII)
        if (mime.equals("application/vnd.wfa.wsc", ignoreCase = true)) {
            val wifi = Wsc.parse(r.payload)
            return ParsedRecord(
                "Wi-Fi",
                wifi?.let { "SSID: ${it.ssid}\nPassword: ${it.key}" } ?: "(Wi-Fi configuration)",
                mime
            )
        }
        val text = if (mime.startsWith("text") || mime.contains("json") || mime.contains("xml") ||
            mime.contains("vcard") || mime.contains("x-vcard")
        ) String(r.payload, Charsets.UTF_8) else "${r.payload.size} bytes"
        return ParsedRecord(if (mime.contains("vcard")) "Contact" else "MIME", text, mime)
    }

    // ---------- Building ----------

    fun text(value: String, lang: String = "en"): NdefMessage =
        NdefMessage(NdefRecord.createTextRecord(lang, value))

    fun uri(value: String): NdefMessage = NdefMessage(NdefRecord.createUri(value))

    fun phone(number: String): NdefMessage = uri("tel:${number.trim()}")

    fun email(address: String, subject: String = ""): NdefMessage {
        val u = if (subject.isBlank()) "mailto:${address.trim()}"
        else "mailto:${address.trim()}?subject=${android.net.Uri.encode(subject)}"
        return uri(u)
    }

    fun app(packageName: String): NdefMessage =
        NdefMessage(NdefRecord.createApplicationRecord(packageName.trim()))

    fun wifi(ssid: String, password: String): NdefMessage =
        NdefMessage(NdefRecord.createMime("application/vnd.wfa.wsc", Wsc.build(ssid, password)))

    fun contact(name: String, phone: String, email: String): NdefMessage {
        val vcard = buildString {
            append("BEGIN:VCARD\r\n")
            append("VERSION:3.0\r\n")
            append("N:$name\r\n")
            append("FN:$name\r\n")
            if (phone.isNotBlank()) append("TEL;TYPE=CELL:$phone\r\n")
            if (email.isNotBlank()) append("EMAIL:$email\r\n")
            append("END:VCARD\r\n")
        }
        return NdefMessage(NdefRecord.createMime("text/vcard", vcard.toByteArray(Charsets.UTF_8)))
    }
}
