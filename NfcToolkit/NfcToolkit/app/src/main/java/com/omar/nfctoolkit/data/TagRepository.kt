package com.omar.nfctoolkit.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.tagsDataStore by preferencesDataStore("tags")

class TagRepository(private val context: Context) {

    private val tagsKey = stringPreferencesKey("tags_json")
    private val activeKey = stringPreferencesKey("active_id")
    private val json = Json { ignoreUnknownKeys = true }

    val tagsFlow: Flow<List<SavedTag>> = context.tagsDataStore.data.map { prefs ->
        prefs[tagsKey]?.let { decode(it) } ?: emptyList()
    }

    val activeIdFlow: Flow<String?> = context.tagsDataStore.data.map { it[activeKey] }

    private fun decode(s: String): List<SavedTag> =
        runCatching { json.decodeFromString<List<SavedTag>>(s) }.getOrDefault(emptyList())

    suspend fun add(tag: SavedTag) = mutate { it + tag }

    suspend fun delete(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
        context.tagsDataStore.edit { prefs ->
            if (prefs[activeKey] == id) {
                prefs.remove(activeKey)
                writeHce(null)
            }
        }
    }

    suspend fun setActive(id: String) {
        context.tagsDataStore.edit { it[activeKey] = id }
        val tag = tagsFlow.first().find { it.id == id }
        writeHce(tag?.ndefHex)
    }

    private suspend fun mutate(transform: (List<SavedTag>) -> List<SavedTag>) {
        context.tagsDataStore.edit { prefs ->
            val current = prefs[tagsKey]?.let { decode(it) } ?: emptyList()
            prefs[tagsKey] = json.encodeToString(transform(current))
        }
    }

    /** Mirror the active payload into plain SharedPreferences for fast, synchronous HCE reads. */
    private fun writeHce(hex: String?) {
        context.getSharedPreferences("hce", Context.MODE_PRIVATE).edit().apply {
            if (hex != null) putString("active_ndef_hex", hex) else remove("active_ndef_hex")
        }.apply()
    }
}
