package com.omar.nfctoolkit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

data class AppSettings(
    val themeMode: Int = 0,        // 0 = system, 1 = light, 2 = dark
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private val themeKey = intPreferencesKey("theme_mode")
    private val dynKey = booleanPreferencesKey("dynamic_color")
    private val hapticKey = booleanPreferencesKey("haptics")

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map {
        AppSettings(
            themeMode = it[themeKey] ?: 0,
            dynamicColor = it[dynKey] ?: true,
            haptics = it[hapticKey] ?: true
        )
    }

    suspend fun setThemeMode(mode: Int) = context.settingsDataStore.edit { it[themeKey] = mode }
    suspend fun setDynamicColor(on: Boolean) = context.settingsDataStore.edit { it[dynKey] = on }
    suspend fun setHaptics(on: Boolean) = context.settingsDataStore.edit { it[hapticKey] = on }
}
