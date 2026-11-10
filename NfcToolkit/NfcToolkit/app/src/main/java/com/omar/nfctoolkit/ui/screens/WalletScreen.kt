package com.omar.nfctoolkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omar.nfctoolkit.data.SavedTag
import com.omar.nfctoolkit.nfc.WriteResult
import com.omar.nfctoolkit.ui.InfoNote
import com.omar.nfctoolkit.ui.ResultDialog
import com.omar.nfctoolkit.ui.ScanWaitingDialog
import com.omar.nfctoolkit.ui.collectAsStateSafe
import com.omar.nfctoolkit.ui.iconFor
import com.omar.nfctoolkit.ui.theme.CardGradients
import com.omar.nfctoolkit.util.hexToBytes
import com.omar.nfctoolkit.viewmodel.MainViewModel
import com.omar.nfctoolkit.viewmodel.NfcAction

@Composable
fun WalletScreen(vm: MainViewModel) {
    val tags by vm.savedTags.collectAsStateSafe()
    val activeId by vm.activeTagId.collectAsStateSafe()

    var awaiting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WriteResult?>(null) }

    LaunchedEffect(Unit) { vm.setAction(NfcAction.Read) }
    LaunchedEffect(Unit) {
        vm.writeResult.collect { res ->
            result = res; awaiting = false; vm.setAction(NfcAction.Read)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Wallet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "Your saved tags. Pick one to write to a blank tag or share over NFC.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(20.dp))

        if (tags.isEmpty()) {
            EmptyWallet()
            return@Column
        }

        val pagerState = rememberPagerState(pageCount = { tags.size })
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            WalletCard(tags[page], isActive = tags[page].id == activeId)
        }

        val current = pagerState.currentPage.coerceIn(0, (tags.size - 1).coerceAtLeast(0))
        val selected = tags.getOrNull(current) ?: return@Column

        Spacer(Modifier.size(20.dp))
        Text(selected.title, style = MaterialTheme.typography.titleLarge)
        selected.preview.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.size(16.dp))
        val isActive = selected.id == activeId
        FilledTonalButton(
            onClick = { vm.setActiveTag(selected.id) },
            enabled = !isActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Verified, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (isActive) "Active for NFC Share" else "Set active for NFC Share")
        }
        Spacer(Modifier.size(10.dp))
        Button(
            onClick = {
                vm.setAction(NfcAction.Write(selected.ndefHex.hexToBytes()))
                awaiting = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Nfc, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Write to a tag")
        }
        Spacer(Modifier.size(10.dp))
        OutlinedButton(
            onClick = { vm.deleteTag(selected.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Delete")
        }

        Spacer(Modifier.size(16.dp))
        InfoNote(
            "\"NFC Share\" presents the active tag's content to a reader using Host Card Emulation — " +
                "the phone acts like an NDEF tag. It shares NDEF data only; it can't impersonate a " +
                "card's UID or emulate an access/transit card."
        )
    }

    if (awaiting) {
        ScanWaitingDialog(
            label = "Hold a writable tag to copy this onto it.",
            onCancel = { awaiting = false; vm.setAction(NfcAction.Read) }
        )
    }
    result?.let { ResultDialog(it) { result = null } }
}

@Composable
private fun WalletCard(tag: SavedTag, isActive: Boolean) {
    val (c1, c2) = CardGradients[tag.colorSeed % CardGradients.size]
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1, c2)))
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(iconFor(tag.kind), null, tint = Color.White, modifier = Modifier.size(30.dp))
                if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("ACTIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column {
                Text(
                    tag.kind.uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    tag.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                tag.preview.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EmptyWallet() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Nfc, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Text("No saved tags yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(6.dp))
            Text(
                "Scan a tag on the Read tab, or compose one on the Write tab, then \"Save to Wallet\". " +
                    "Saved tags appear here as cards you can re-write or share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
