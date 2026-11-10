package com.omar.nfctoolkit.util

fun ByteArray.toHex(): String = joinToString(" ") { String.format("%02X", it) }

fun ByteArray.toHexCompact(): String = joinToString("") { String.format("%02X", it) }

fun String.hexToBytes(): ByteArray {
    val s = this.replace(" ", "").replace("\n", "")
    if (s.length % 2 != 0) return ByteArray(0)
    return ByteArray(s.length / 2) {
        ((Character.digit(s[it * 2], 16) shl 4) + Character.digit(s[it * 2 + 1], 16)).toByte()
    }
}
