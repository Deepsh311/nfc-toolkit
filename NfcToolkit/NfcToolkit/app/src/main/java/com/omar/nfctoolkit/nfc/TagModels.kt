package com.omar.nfctoolkit.nfc

/** A single decoded NDEF record, ready for display. */
data class ParsedRecord(
    val kind: String,
    val content: String,
    val meta: String? = null
)

/** Everything we learned from a scanned tag. */
data class TagInfo(
    val uid: String,
    val techList: List<String>,
    val typeLabel: String,
    val maxSize: Int?,
    val writable: Boolean?,
    val records: List<ParsedRecord>,
    val rawNdef: ByteArray?,
    val extra: Map<String, String>,
    val scannedAt: Long = System.currentTimeMillis()
)

data class WriteResult(val success: Boolean, val message: String)
