package com.omar.nfctoolkit.nfc

/**
 * Minimal Wi-Fi Simple Configuration (WSC) builder/parser for the
 * "application/vnd.wfa.wsc" NDEF MIME type. Enough to share/read a
 * home Wi-Fi network (SSID + password, WPA2-PSK/AES by default).
 */
object Wsc {
    data class WifiCred(val ssid: String, val key: String)

    private const val CREDENTIAL = 0x100E
    private const val SSID = 0x1045
    private const val NETWORK_KEY = 0x1027
    private const val AUTH_TYPE = 0x1003
    private const val ENC_TYPE = 0x100F
    private const val MAC_ADDRESS = 0x1020
    private const val NETWORK_INDEX = 0x1026
    private const val AUTH_WPA2PSK = 0x0020
    private const val ENC_AES = 0x0008

    private fun tlv(type: Int, value: ByteArray): ByteArray {
        val out = ByteArray(4 + value.size)
        out[0] = (type ushr 8).toByte()
        out[1] = type.toByte()
        out[2] = (value.size ushr 8).toByte()
        out[3] = value.size.toByte()
        System.arraycopy(value, 0, out, 4, value.size)
        return out
    }

    private fun u16(v: Int) = byteArrayOf((v ushr 8).toByte(), v.toByte())

    fun build(ssid: String, key: String): ByteArray {
        val credValue =
            tlv(NETWORK_INDEX, byteArrayOf(1)) +
            tlv(SSID, ssid.toByteArray(Charsets.UTF_8)) +
            tlv(AUTH_TYPE, u16(AUTH_WPA2PSK)) +
            tlv(ENC_TYPE, u16(ENC_AES)) +
            tlv(NETWORK_KEY, key.toByteArray(Charsets.UTF_8)) +
            tlv(MAC_ADDRESS, ByteArray(6))
        return tlv(CREDENTIAL, credValue)
    }

    fun parse(payload: ByteArray): WifiCred? {
        var ssid: String? = null
        var key: String? = null
        fun walk(data: ByteArray) {
            var i = 0
            while (i + 4 <= data.size) {
                val type = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
                val len = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
                val start = i + 4
                if (start + len > data.size) break
                val value = data.copyOfRange(start, start + len)
                when (type) {
                    CREDENTIAL -> walk(value)
                    SSID -> ssid = String(value, Charsets.UTF_8)
                    NETWORK_KEY -> key = String(value, Charsets.UTF_8)
                }
                i = start + len
            }
        }
        return try {
            walk(payload)
            ssid?.let { WifiCred(it, key ?: "") }
        } catch (e: Exception) {
            null
        }
    }
}
