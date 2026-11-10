package com.omar.nfctoolkit.ui

import com.omar.nfctoolkit.data.SavedTag
import com.omar.nfctoolkit.nfc.TagInfo
import com.omar.nfctoolkit.util.toHexCompact
import java.util.UUID
import kotlin.math.abs

fun savedTagFromInfo(info: TagInfo, title: String): SavedTag? {
    val raw = info.rawNdef ?: return null
    val first = info.records.firstOrNull()
    val finalTitle = title.ifBlank { first?.kind ?: "Tag" }
    return SavedTag(
        id = UUID.randomUUID().toString(),
        title = finalTitle,
        kind = first?.kind ?: "Unknown",
        preview = (first?.content ?: "").replace("\n", " ").take(60),
        ndefHex = raw.toHexCompact(),
        colorSeed = abs((finalTitle + info.uid).hashCode()),
        createdAt = System.currentTimeMillis()
    )
}

fun savedTagFromBytes(bytes: ByteArray, kind: String, preview: String, title: String): SavedTag {
    val finalTitle = title.ifBlank { kind }
    return SavedTag(
        id = UUID.randomUUID().toString(),
        title = finalTitle,
        kind = kind,
        preview = preview.replace("\n", " ").take(60),
        ndefHex = bytes.toHexCompact(),
        colorSeed = abs((finalTitle + preview).hashCode()),
        createdAt = System.currentTimeMillis()
    )
}
