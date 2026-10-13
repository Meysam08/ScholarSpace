package com.example.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CollectionEntity
import com.example.data.model.MaterialEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val currentCollectionId: String? = null,
    val currentCollection: CollectionEntity? = null,
    val breadcrumbs: List<CollectionEntity> = emptyList(),
    val subCollections: List<CollectionEntity> = emptyList(),
    val materials: List<MaterialEntity> = emptyList(),
    val allCollections: List<CollectionEntity> = emptyList(),
    val searchQuery: String = "",
    val searchCollectionResults: List<CollectionEntity> = emptyList(),
    val searchMaterialResults: List<MaterialEntity> = emptyList(),
    val isSearching: Boolean = false,
    val viewMode: String = "GRID", // "GRID" or "LIST"
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentCollectionId = MutableStateFlow<String?>(null)
    val currentCollectionId: StateFlow<String?> = _currentCollectionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _viewMode = MutableStateFlow("GRID")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                settings?.libraryViewMode?.let { _viewMode.value = it }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _currentCollectionId,
        _searchQuery,
        _viewMode,
        _errorMessage,
        _userMessage
    ) { currentId, query, vMode, err, msg ->
        StateTuple(currentId, query, vMode, err, msg)
    }.flatMapLatest { tuple ->
        val currentId = tuple.currentId
        val query = tuple.query
        val isSearching = query.isNotBlank()

        if (isSearching) {
            combine(
                libraryRepository.searchCollections(query),
                libraryRepository.searchMaterials(query),
                libraryRepository.getAllCollections()
            ) { matchedCols, matchedMats, allCols ->
                LibraryUiState(
                    currentCollectionId = currentId,
                    searchQuery = query,
                    isSearching = true,
                    searchCollectionResults = matchedCols,
                    searchMaterialResults = matchedMats,
                    allCollections = allCols,
                    viewMode = tuple.viewMode,
                    errorMessage = tuple.err,
                    successMessage = tuple.msg
                )
            }
        } else if (currentId == null) {
            // Root view
            combine(
                libraryRepository.getRootCollections(),
                libraryRepository.getAllCollections()
            ) { rootCols, allCols ->
                LibraryUiState(
                    currentCollectionId = null,
                    currentCollection = null,
                    breadcrumbs = emptyList(),
                    subCollections = rootCols,
                    materials = emptyList(),
                    allCollections = allCols,
                    viewMode = tuple.viewMode,
                    errorMessage = tuple.err,
                    successMessage = tuple.msg
                )
            }
        } else {
            // Nested collection view
            combine(
                libraryRepository.getCollectionById(currentId),
                libraryRepository.getSubCollections(currentId),
                libraryRepository.getMaterialsForCollection(currentId),
                libraryRepository.getAllCollections()
            ) { col, subs, mats, allCols ->
                LibraryUiState(
                    currentCollectionId = currentId,
                    currentCollection = col,
                    subCollections = subs,
                    materials = mats,
                    allCollections = allCols,
                    viewMode = tuple.viewMode,
                    errorMessage = tuple.err,
                    successMessage = tuple.msg
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    private data class StateTuple(
        val currentId: String?,
        val query: String,
        val viewMode: String,
        val err: String?,
        val msg: String?
    )

    private val _breadcrumbs = MutableStateFlow<List<CollectionEntity>>(emptyList())
    val breadcrumbs: StateFlow<List<CollectionEntity>> = _breadcrumbs.asStateFlow()

    fun navigateToCollection(collectionId: String?) {
        _currentCollectionId.value = collectionId
        _searchQuery.value = ""
        refreshBreadcrumbs(collectionId)
    }

    private fun refreshBreadcrumbs(collectionId: String?) {
        viewModelScope.launch {
            if (collectionId == null) {
                _breadcrumbs.value = emptyList()
            } else {
                _breadcrumbs.value = libraryRepository.getBreadcrumbPath(collectionId)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        val next = if (_viewMode.value == "GRID") "LIST" else "GRID"
        _viewMode.value = next
        viewModelScope.launch {
            settingsRepository.updateLibraryViewMode(next)
        }
    }

    fun createCollection(name: String, coverColorHex: String = "#176B63") {
        if (name.isBlank()) {
            _errorMessage.value = "Collection name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                libraryRepository.createCollection(
                    name = name,
                    parentId = _currentCollectionId.value,
                    coverColorHex = coverColorHex
                )
                _userMessage.value = "Collection created"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create collection: ${e.message}"
            }
        }
    }

    fun renameCollection(collectionId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                libraryRepository.renameCollection(collectionId, newName)
                _userMessage.value = "Collection renamed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename collection: ${e.message}"
            }
        }
    }

    fun updateCoverColor(collectionId: String, hex: String) {
        viewModelScope.launch {
            try {
                libraryRepository.updateCollectionCoverColor(collectionId, hex)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update cover color: ${e.message}"
            }
        }
    }

    fun setCustomCover(collectionId: String, uri: Uri) {
        viewModelScope.launch {
            val result = libraryRepository.setCustomCover(collectionId, uri)
            if (result.isSuccess) {
                _userMessage.value = "Custom cover updated"
            } else {
                _errorMessage.value = "Failed to set custom cover: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun removeCustomCover(collectionId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.removeCustomCover(collectionId)
                _userMessage.value = "Cover reset to default color"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reset cover: ${e.message}"
            }
        }
    }

    fun moveCollection(collectionId: String, targetParentId: String?) {
        viewModelScope.launch {
            val result = libraryRepository.moveCollection(collectionId, targetParentId)
            if (result.isSuccess) {
                _userMessage.value = "Collection moved successfully"
                refreshBreadcrumbs(_currentCollectionId.value)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Cannot move collection"
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                val wasCurrent = _currentCollectionId.value == collectionId
                val parentId = libraryRepository.getCollectionByIdSync(collectionId)?.parentId
                libraryRepository.deleteCollection(collectionId)
                _userMessage.value = "Collection and its contents deleted"
                if (wasCurrent) {
                    navigateToCollection(parentId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete collection: ${e.message}"
            }
        }
    }

    fun importMaterial(uri: Uri) {
        val colId = _currentCollectionId.value
        if (colId == null) {
            _errorMessage.value = "Please open or create a collection to import materials into."
            return
        }

        viewModelScope.launch {
            val result = libraryRepository.importMaterial(colId, uri)
            if (result.isSuccess) {
                _userMessage.value = "Material imported successfully"
            } else {
                _errorMessage.value = "Failed to import file: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun renameMaterial(materialId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                libraryRepository.renameMaterial(materialId, newName)
                _userMessage.value = "Material renamed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename: ${e.message}"
            }
        }
    }

    fun moveMaterial(materialId: String, newCollectionId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.moveMaterial(materialId, newCollectionId)
                _userMessage.value = "Material moved"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to move material: ${e.message}"
            }
        }
    }

    fun deleteMaterial(material: MaterialEntity) {
        viewModelScope.launch {
            try {
                libraryRepository.deleteMaterial(material)
                _userMessage.value = "Material removed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove material: ${e.message}"
            }
        }
    }

    fun openMaterial(material: MaterialEntity): Result<Unit> {
        val result = libraryRepository.openMaterial(material)
        if (result.isFailure) {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Cannot open file"
        }
        return result
    }

    fun clearMessages() {
        _userMessage.value = null
        _errorMessage.value = null
    }

    class Factory(
        private val libraryRepository: LibraryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(libraryRepository, settingsRepository) as T
        }
    }
}
