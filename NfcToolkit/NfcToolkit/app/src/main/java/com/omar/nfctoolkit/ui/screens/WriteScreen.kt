package com.omar.nfctoolkit.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import android.nfc.NdefMessage
import com.omar.nfctoolkit.nfc.NdefUtils
import com.omar.nfctoolkit.nfc.WriteResult
import com.omar.nfctoolkit.ui.ResultDialog
import com.omar.nfctoolkit.ui.ScanWaitingDialog
import com.omar.nfctoolkit.ui.savedTagFromBytes
import com.omar.nfctoolkit.viewmodel.MainViewModel
import com.omar.nfctoolkit.viewmodel.NfcAction

private enum class RecordType(val label: String) {
    TEXT("Text"), URL("URL"), WIFI("Wi-Fi"), CONTACT("Contact"),
    PHONE("Phone"), EMAIL("Email"), APP("App")
}

@Composable
fun WriteScreen(vm: MainViewModel) {
    LaunchedEffectReset(vm)

    var type by remember { mutableStateOf(RecordType.TEXT) }
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var ssid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }
    var lockAfter by remember { mutableStateOf(false) }

    var awaiting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WriteResult?>(null) }
    var showSave by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.writeResult.collect { res ->
            result = res
            awaiting = false
            vm.resetToRead()
        }
    }

    fun buildMessage(): NdefMessage? = runCatching {
        when (type) {
            RecordType.TEXT -> NdefUtils.text(text)
            RecordType.URL -> NdefUtils.uri(url)
            RecordType.WIFI -> NdefUtils.wifi(ssid, wifiPass)
            RecordType.CONTACT -> NdefUtils.contact(name, phone, email)
            RecordType.PHONE -> NdefUtils.phone(phone)
            RecordType.EMAIL -> NdefUtils.email(email)
            RecordType.APP -> NdefUtils.app(pkg)
        }
    }.getOrNull()

    fun previewText(): String = when (type) {
        RecordType.TEXT -> text
        RecordType.URL -> url
        RecordType.WIFI -> "SSID: $ssid"
        RecordType.CONTACT -> name
        RecordType.PHONE -> phone
        RecordType.EMAIL -> email
        RecordType.APP -> pkg
    }

    val isValid = when (type) {
        RecordType.TEXT -> text.isNotBlank()
        RecordType.URL -> url.length > "https://".length
        RecordType.WIFI -> ssid.isNotBlank()
        RecordType.CONTACT -> name.isNotBlank()
        RecordType.PHONE -> phone.isNotBlank()
        RecordType.EMAIL -> email.isNotBlank()
        RecordType.APP -> pkg.contains(".")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Write a tag", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "Compose a record, then hold a writable tag to your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.size(16.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordType.entries.forEach { rt ->
                FilterChip(
                    selected = type == rt,
                    onClick = { type = rt },
                    label = { Text(rt.label) }
                )
            }
        }

        Spacer(Modifier.size(16.dp))
        when (type) {
            RecordType.TEXT -> Field(text, { text = it }, "Text", singleLine = false)
            RecordType.URL -> Field(url, { url = it }, "URL", KeyboardType.Uri)
            RecordType.WIFI -> {
                Field(ssid, { ssid = it }, "Network name (SSID)")
                Spacer(Modifier.size(10.dp))
                Field(wifiPass, { wifiPass = it }, "Password")
            }
            RecordType.CONTACT -> {
                Field(name, { name = it }, "Full name")
                Spacer(Modifier.size(10.dp))
                Field(phone, { phone = it }, "Phone", KeyboardType.Phone)
                Spacer(Modifier.size(10.dp))
                Field(email, { email = it }, "Email", KeyboardType.Email)
            }
            RecordType.PHONE -> Field(phone, { phone = it }, "Phone number", KeyboardType.Phone)
            RecordType.EMAIL -> Field(email, { email = it }, "Email address", KeyboardType.Email)
            RecordType.APP -> Field(pkg, { pkg = it }, "Package name (e.g. com.spotify.music)")
        }

        Spacer(Modifier.size(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(checked = lockAfter, onCheckedChange = { lockAfter = it })
            Spacer(Modifier.size(12.dp))
            Column {
                Text("Lock after writing", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Makes the tag permanently read-only. Can't be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.size(20.dp))
        Button(
            onClick = {
                buildMessage()?.let {
                    vm.setAction(NfcAction.Write(it.toByteArray(), lockAfter))
                    awaiting = true
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Nfc, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Write to tag")
        }
        Spacer(Modifier.size(10.dp))
        OutlinedButton(
            onClick = { showSave = true },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Save to Wallet")
        }
    }

    if (awaiting) {
        ScanWaitingDialog(
            label = if (lockAfter) "The tag will be written and then locked." else "The record will be written to the tag.",
            onCancel = { awaiting = false; vm.resetToRead() }
        )
    }
    result?.let { ResultDialog(it) { result = null } }

    if (showSave) {
        SaveDialog(
            defaultTitle = type.label,
            onDismiss = { showSave = false },
            onConfirm = { title ->
                buildMessage()?.let {
                    vm.saveTag(savedTagFromBytes(it.toByteArray(), type.label, previewText(), title))
                }
                showSave = false
            }
        )
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LaunchedEffectReset(vm: MainViewModel) {
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.setAction(NfcAction.Read) }
}
