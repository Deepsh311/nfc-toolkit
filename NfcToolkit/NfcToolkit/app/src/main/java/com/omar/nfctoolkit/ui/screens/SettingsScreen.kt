package com.omar.nfctoolkit.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omar.nfctoolkit.data.AppSettings
import com.omar.nfctoolkit.ui.SectionCard
import com.omar.nfctoolkit.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, nfcSupported: Boolean, nfcEnabled: Boolean) {
    val settings by vm.settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(16.dp))

        SectionCard("Appearance") {
            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            val options = listOf("System", "Light", "Dark")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = settings.themeMode == index,
                        onClick = { vm.setThemeMode(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.size(16.dp))
            ToggleRow(
                title = "Dynamic colour",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "Match your wallpaper palette (Android 12+)"
                else "Requires Android 12 or newer",
                checked = settings.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onChange = { vm.setDynamicColor(it) }
            )
        }

        Spacer(Modifier.size(12.dp))
        SectionCard("Scanning") {
            ToggleRow(
                title = "Haptic feedback",
                subtitle = "Vibrate when a tag is detected",
                checked = settings.haptics,
                onChange = { vm.setHaptics(it) }
            )
            Spacer(Modifier.size(12.dp))
            HorizontalDivider()
            Spacer(Modifier.size(12.dp))
            Text(
                "NFC status: " + when {
                    !nfcSupported -> "no NFC hardware on this device"
                    !nfcEnabled -> "supported but currently turned off"
                    else -> "ready"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.size(12.dp))
        SectionCard("What this app can and can't do") {
            Text(
                "Reads NFC tags, writes NDEF records to blank/writable tags, duplicates NDEF " +
                    "content between tags, and shares a saved record over NFC (Host Card Emulation).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "It can't clone or emulate access fobs, keycards, transit cards or bank cards. " +
                    "Stock Android can't spoof a card's UID, emulate MIFARE Classic, or read 125 kHz " +
                    "(low-frequency) tags at all — that needs dedicated hardware such as a Proxmark3 " +
                    "or Flipper Zero, and cloning credentials you don't own is usually illegal anyway.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.size(12.dp))
        Text(
            "NFC Toolkit · v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
