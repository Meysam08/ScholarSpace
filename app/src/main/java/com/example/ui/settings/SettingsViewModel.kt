package com.example.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.file.BackupManager
import com.example.data.file.FileManager
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val totalStorageBytes: Long = 0L,
    val totalCollectionsCount: Int = 0,
    val totalMaterialsCount: Int = 0,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val backupSuccessMessage: String? = null,
    val backupErrorMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val fileManager: FileManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _storageBytes = MutableStateFlow(0L)
    private val _isExporting = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _backupSuccessMessage = MutableStateFlow<String?>(null)
    private val _backupErrorMessage = MutableStateFlow<String?>(null)

    init {
        refreshStorageUsage()
    }

    private val countsFlow = combine(
        libraryRepository.getAllCollections(),
        libraryRepository.getTotalMaterialsCount()
    ) { cols, matsCount ->
        Pair(cols.size, matsCount)
    }

    private val appStatsFlow = combine(
        settingsRepository.settings,
        _storageBytes,
        countsFlow
    ) { settings, storage, (colsCount, matsCount) ->
        Quadruple(settings ?: UserSettingsEntity(), storage, colsCount, matsCount)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private val backupStateFlow = combine(
        _isExporting,
        _isImporting,
        _backupSuccessMessage,
        _backupErrorMessage
    ) { exporting, importing, successMsg, errMsg ->
        BackupStatus(exporting, importing, successMsg, errMsg)
    }

    private data class BackupStatus(
        val isExporting: Boolean,
        val isImporting: Boolean,
        val successMessage: String?,
        val errorMessage: String?
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        appStatsFlow,
        backupStateFlow
    ) { statsQuad, backupStatus ->
        val (settings, storage, colsCount, matsCount) = statsQuad
        SettingsUiState(
            settings = settings,
            totalStorageBytes = storage,
            totalCollectionsCount = colsCount,
            totalMaterialsCount = matsCount,
            isExporting = backupStatus.isExporting,
            isImporting = backupStatus.isImporting,
            backupSuccessMessage = backupStatus.successMessage,
            backupErrorMessage = backupStatus.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val usage = withContext(Dispatchers.IO) {
                fileManager.calculateTotalStorageUsage()
            }
            _storageBytes.value = usage
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateTimerDurations(focusMin: Int, shortBreakMin: Int, longBreakMin: Int) {
        viewModelScope.launch {
            settingsRepository.updateTimerDurations(focusMin, shortBreakMin, longBreakMin)
        }
    }

    fun setLibraryViewMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateLibraryViewMode(mode)
        }
    }

    fun exportBackup(uri: Uri) {
        _isExporting.value = true
        _backupSuccessMessage.value = null
        _backupErrorMessage.value = null

        viewModelScope.launch {
            val result = backupManager.exportBackup(uri)
            _isExporting.value = false
            if (result.isSuccess) {
                val stats = result.getOrThrow()
                _backupSuccessMessage.value = "Backup created successfully! Saved ${stats.collectionsCount} collections, ${stats.materialsCount} files, and ${stats.tasksCount} tasks."
            } else {
                _backupErrorMessage.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        _isImporting.value = true
        _backupSuccessMessage.value = null
        _backupErrorMessage.value = null

        viewModelScope.launch {
            val result = backupManager.importBackup(uri)
            _isImporting.value = false
            if (result.isSuccess) {
                val stats = result.getOrThrow()
                refreshStorageUsage()
                _backupSuccessMessage.value = "Restored ${stats.collectionsCount} collections, ${stats.materialsCount} materials, and ${stats.tasksCount} tasks successfully!"
            } else {
                _backupErrorMessage.value = "Restore failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearBackupMessages() {
        _backupSuccessMessage.value = null
        _backupErrorMessage.value = null
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val libraryRepository: LibraryRepository,
        private val fileManager: FileManager,
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                libraryRepository,
                fileManager,
                backupManager
            ) as T
        }
    }
}
