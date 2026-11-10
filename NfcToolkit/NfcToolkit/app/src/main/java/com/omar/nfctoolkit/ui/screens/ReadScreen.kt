package com.omar.nfctoolkit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omar.nfctoolkit.nfc.TagInfo
import com.omar.nfctoolkit.ui.InfoNote
import com.omar.nfctoolkit.ui.KeyValueRow
import com.omar.nfctoolkit.ui.collectAsStateSafe
import com.omar.nfctoolkit.ui.PulsingNfc
import com.omar.nfctoolkit.ui.iconFor
import com.omar.nfctoolkit.ui.savedTagFromInfo
import com.omar.nfctoolkit.ui.theme.MonospaceUid
import com.omar.nfctoolkit.viewmodel.MainViewModel
import com.omar.nfctoolkit.viewmodel.NfcAction

@Composable
fun ReadScreen(vm: MainViewModel, nfcSupported: Boolean, nfcEnabled: Boolean) {
    LaunchedEffect(Unit) { vm.setAction(NfcAction.Read) }

    val lastRead by vm.lastRead.collectAsStateSafe()
    val history by vm.history.collectAsStateSafe(emptyList())
    var saveTarget by remember { mutableStateOf<TagInfo?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Read a tag", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            when {
                !nfcSupported -> "This device has no NFC hardware."
                !nfcEnabled -> "NFC is turned off — enable it in system settings."
                else -> "Hold any NFC tag to the back of your phone."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.size(24.dp))
        PulsingNfc(active = nfcSupported && nfcEnabled)
        Spacer(Modifier.size(24.dp))

        lastRead?.let { info ->
            TagDetails(info)
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { saveTarget = info },
                    enabled = info.rawNdef != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Save to Wallet")
                }
            }
            if (info.rawNdef == null) {
                Spacer(Modifier.size(8.dp))
                InfoNote(
                    "This tag has no NDEF data, so there's nothing to save or re-write. " +
                        "Raw UID/sector cloning (e.g. access fobs) isn't supported on stock Android."
                )
            }
        }

        if (history.size > 1) {
            Spacer(Modifier.size(24.dp))
            Text(
                "Recent scans",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.size(8.dp))
            history.drop(1).take(8).forEach { h -> HistoryRow(h) }
        }
    }

    saveTarget?.let { info ->
        SaveDialog(
            defaultTitle = info.records.firstOrNull()?.kind ?: "Tag",
            onDismiss = { saveTarget = null },
            onConfirm = { title ->
                savedTagFromInfo(info, title)?.let { vm.saveTag(it) }
                saveTarget = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagDetails(info: TagInfo) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(info.typeLabel, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(8.dp))
            Text("UID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(info.uid.ifBlank { "—" }, style = MonospaceUid)

            Spacer(Modifier.size(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                info.techList.forEach { tech ->
                    AssistChip(onClick = {}, label = { Text(tech, style = MaterialTheme.typography.labelSmall) })
                }
            }

            if (info.extra.isNotEmpty()) {
                Spacer(Modifier.size(12.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                info.extra.forEach { (k, v) -> KeyValueRow(k, v) }
                info.maxSize?.let { KeyValueRow("Capacity", "$it bytes") }
                info.writable?.let { KeyValueRow("Writable", if (it) "Yes" else "No (locked)") }
            }

            if (info.records.isNotEmpty()) {
                Spacer(Modifier.size(12.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                Text("NDEF records (${info.records.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.size(6.dp))
                info.records.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        Icon(iconFor(r.kind), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                r.kind + (r.meta?.let { "  ·  $it" } ?: ""),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(r.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(info: TagInfo) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(info.records.firstOrNull()?.kind ?: "Nfc"), null, Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(info.typeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    info.records.firstOrNull()?.content?.take(40) ?: info.uid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SaveDialog(defaultTitle: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf(defaultTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Wallet") },
        text = {
            Column {
                Text("Give this tag a name so you can find it later.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
