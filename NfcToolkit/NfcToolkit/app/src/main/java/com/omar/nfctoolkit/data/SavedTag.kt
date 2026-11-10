package com.omar.nfctoolkit.data

import kotlinx.serialization.Serializable

/**
 * A saved NDEF payload the user can re-write to blank tags or present over NFC (HCE).
 * ndefHex is the raw NDEF *message* bytes, hex-encoded.
 */
@Serializable
data class SavedTag(
    val id: String,
    val title: String,
    val kind: String,
    val preview: String,
    val ndefHex: String,
    val colorSeed: Int,
    val createdAt: Long
)
