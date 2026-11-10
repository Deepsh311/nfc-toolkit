package com.omar.nfctoolkit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.omar.nfctoolkit.nfc.WriteResult
import com.omar.nfctoolkit.ui.InfoNote
import com.omar.nfctoolkit.ui.ResultDialog
import com.omar.nfctoolkit.ui.ScanWaitingDialog
import com.omar.nfctoolkit.ui.collectAsStateSafe
import com.omar.nfctoolkit.ui.savedTagFromInfo
import com.omar.nfctoolkit.viewmodel.MainViewModel
import com.omar.nfctoolkit.viewmodel.NfcAction

@Composable
fun CopyScreen(vm: MainViewModel) {
    val source by vm.copySource.collectAsStateSafe()
    var awaiting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WriteResult?>(null) }
    var showSave by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.clearCopySource(); vm.setAction(NfcAction.Read) }
    LaunchedEffect(Unit) {
        vm.writeResult.collect { res ->
            result = res
            awaiting = false
            vm.setAction(NfcAction.Read)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Copy a tag", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "Duplicate the NDEF content of one tag onto another writable tag.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.size(20.dp))

        StepCard(
            number = 1,
            title = "Scan the source tag",
            done = source != null,
            body = {
                if (source == null) {
                    Text(
                        "Hold the tag you want to copy to your phone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = { vm.setAction(NfcAction.CopySource) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ready — scan source") }
                } else {
                    val s = source!!
                    Text(s.typeLabel, fontWeight = FontWeight.Medium)
                    s.records.firstOrNull()?.let {
                        Text("${it.kind}: ${it.content.take(60)}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.rawNdef == null) {
                        Spacer(Modifier.size(10.dp))
                        InfoNote(
                            "This tag carries no NDEF data, so there's nothing to copy. Copying raw " +
                                "UID/sector data (access cards, transit cards) needs dedicated hardware " +
                                "like a Proxmark — a phone can't clone or emulate those."
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    OutlinedButton(
                        onClick = { vm.clearCopySource() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rescan") }
                }
            }
        )

        Spacer(Modifier.size(12.dp))

        StepCard(
            number = 2,
            title = "Write to the target tag",
            done = false,
            enabled = source?.rawNdef != null,
            body = {
                Text(
                    "Hold a blank/writable tag to copy the content onto it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.size(12.dp))
                Button(
                    onClick = {
                        source?.rawNdef?.let {
                            vm.setAction(NfcAction.CopyTarget(it))
                            awaiting = true
                        }
                    },
                    enabled = source?.rawNdef != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ready — scan target") }
            }
        )

        source?.rawNdef?.let {
            Spacer(Modifier.size(16.dp))
            OutlinedButton(onClick = { showSave = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Save source to Wallet instead")
            }
        }
    }

    if (awaiting) {
        ScanWaitingDialog(
            label = "The source content will be written to this tag.",
            onCancel = { awaiting = false; vm.setAction(NfcAction.Read) }
        )
    }
    result?.let { ResultDialog(it) { result = null } }

    if (showSave) {
        source?.let { s ->
            SaveDialog(
                defaultTitle = s.records.firstOrNull()?.kind ?: "Copied tag",
                onDismiss = { showSave = false },
                onConfirm = { title ->
                    savedTagFromInfo(s, title)?.let { vm.saveTag(it) }
                    showSave = false
                }
            )
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    done: Boolean,
    enabled: Boolean = true,
    body: @Composable () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.size(10.dp))
                Text("Step $number · $title", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(12.dp))
            body()
        }
    }
}
