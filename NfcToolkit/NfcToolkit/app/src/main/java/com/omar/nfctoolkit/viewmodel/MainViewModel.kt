package com.omar.nfctoolkit.viewmodel

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omar.nfctoolkit.data.SavedTag
import com.omar.nfctoolkit.data.SettingsRepository
import com.omar.nfctoolkit.data.TagRepository
import com.omar.nfctoolkit.nfc.NfcOps
import com.omar.nfctoolkit.nfc.TagInfo
import com.omar.nfctoolkit.nfc.WriteResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NfcAction {
    data object Read : NfcAction
    data class Write(val bytes: ByteArray, val lockAfter: Boolean = false) : NfcAction
    data object CopySource : NfcAction
    data class CopyTarget(val bytes: ByteArray) : NfcAction
    data object Lock : NfcAction
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val tagRepo = TagRepository(app)
    val settingsRepo = SettingsRepository(app)

    private val _action = MutableStateFlow<NfcAction>(NfcAction.Read)
    val action: StateFlow<NfcAction> = _action
    fun setAction(a: NfcAction) { _action.value = a }
    fun resetToRead() { _action.value = NfcAction.Read }

    private val _lastRead = MutableStateFlow<TagInfo?>(null)
    val lastRead: StateFlow<TagInfo?> = _lastRead

    private val _copySource = MutableStateFlow<TagInfo?>(null)
    val copySource: StateFlow<TagInfo?> = _copySource
    fun clearCopySource() { _copySource.value = null }

    private val _history = MutableStateFlow<List<TagInfo>>(emptyList())
    val history: StateFlow<List<TagInfo>> = _history

    private val _writeResult = MutableSharedFlow<WriteResult>(extraBufferCapacity = 1)
    val writeResult = _writeResult.asSharedFlow()

    val savedTags: StateFlow<List<SavedTag>> =
        tagRepo.tagsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeTagId: StateFlow<String?> =
        tagRepo.activeIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Called from the Activity's NFC reader callback (background thread). */
    fun handleTag(tag: Tag) {
        when (val a = _action.value) {
            is NfcAction.Read -> {
                val info = NfcOps.readTag(tag)
                _lastRead.value = info
                _history.value = (listOf(info) + _history.value).take(30)
            }
            is NfcAction.Write -> {
                var res = NfcOps.writeNdef(tag, a.bytes)
                if (res.success && a.lockAfter) res = NfcOps.makeReadOnly(tag)
                _writeResult.tryEmit(res)
            }
            is NfcAction.CopySource -> _copySource.value = NfcOps.readTag(tag)
            is NfcAction.CopyTarget -> _writeResult.tryEmit(NfcOps.writeNdef(tag, a.bytes))
            is NfcAction.Lock -> _writeResult.tryEmit(NfcOps.makeReadOnly(tag))
        }
    }

    fun saveTag(tag: SavedTag) = viewModelScope.launch { tagRepo.add(tag) }
    fun deleteTag(id: String) = viewModelScope.launch { tagRepo.delete(id) }
    fun setActiveTag(id: String) = viewModelScope.launch { tagRepo.setActive(id) }

    fun setThemeMode(mode: Int) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setDynamicColor(on: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(on) }
    fun setHaptics(on: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(on) }
}
