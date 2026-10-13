package com.example.data.repository

import android.net.Uri
import com.example.data.dao.CollectionDao
import com.example.data.dao.MaterialDao
import com.example.data.file.FileManager
import com.example.data.model.CollectionEntity
import com.example.data.model.MaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class LibraryRepository(
    private val collectionDao: CollectionDao,
    private val materialDao: MaterialDao,
    private val fileManager: FileManager
) {

    fun getRootCollections(): Flow<List<CollectionEntity>> = collectionDao.getRootCollections()

    fun getSubCollections(parentId: String): Flow<List<CollectionEntity>> =
        collectionDao.getSubCollections(parentId)

    fun getMaterialsForCollection(collectionId: String): Flow<List<MaterialEntity>> =
        materialDao.getMaterialsForCollection(collectionId)

    fun getAllCollections(): Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    fun getCollectionById(id: String): Flow<CollectionEntity?> = collectionDao.getCollectionById(id)

    suspend fun getCollectionByIdSync(id: String): CollectionEntity? =
        collectionDao.getCollectionByIdSync(id)

    suspend fun getBreadcrumbPath(collectionId: String): List<CollectionEntity> = withContext(Dispatchers.IO) {
        val path = mutableListOf<CollectionEntity>()
        var currentId: String? = collectionId

        while (currentId != null) {
            val entity = collectionDao.getCollectionByIdSync(currentId) ?: break
            path.add(0, entity)
            currentId = entity.parentId
        }
        path
    }

    suspend fun createCollection(
        name: String,
        parentId: String? = null,
        coverColorHex: String = "#176B63"
    ): CollectionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val count = if (parentId == null) 0 else collectionDao.getSubCollectionCount(parentId)
        val collection = CollectionEntity(
            id = id,
            name = name.trim(),
            parentId = parentId,
            coverColorHex = coverColorHex,
            sortOrder = count,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        collectionDao.insert(collection)
        collection
    }

    suspend fun renameCollection(id: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = collectionDao.getCollectionByIdSync(id) ?: return@withContext
        collectionDao.update(
            existing.copy(
                name = newName.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateCollectionCoverColor(id: String, colorHex: String) = withContext(Dispatchers.IO) {
        val existing = collectionDao.getCollectionByIdSync(id) ?: return@withContext
        collectionDao.update(
            existing.copy(
                coverColorHex = colorHex,
                customCoverPath = null, // Clearing custom image cover reverts to color
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setCustomCover(id: String, imageUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = collectionDao.getCollectionByIdSync(id)
            ?: return@withContext Result.failure(IllegalStateException("Collection not found"))

        val importResult = fileManager.importCoverImage(imageUri, id)
        if (importResult.isSuccess) {
            val relativePath = importResult.getOrThrow()
            collectionDao.update(
                existing.copy(
                    customCoverPath = relativePath,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } else {
            Result.failure(importResult.exceptionOrNull() ?: Exception("Failed to import cover"))
        }
    }

    suspend fun removeCustomCover(id: String) = withContext(Dispatchers.IO) {
        val existing = collectionDao.getCollectionByIdSync(id) ?: return@withContext
        existing.customCoverPath?.let { path ->
            fileManager.deleteManagedFile(path)
        }
        collectionDao.update(
            existing.copy(
                customCoverPath = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Prevents circular hierarchy relationships.
     * A collection cannot be moved into itself or into any of its descendants.
     */
    suspend fun canMoveCollection(collectionId: String, targetParentId: String?): Boolean = withContext(Dispatchers.IO) {
        if (targetParentId == null) return@withContext true // Moving to root is always safe
        if (collectionId == targetParentId) return@withContext false // Cannot move into self

        // Traverse upwards from targetParentId to see if collectionId is an ancestor
        var current: String? = targetParentId
        while (current != null) {
            if (current == collectionId) return@withContext false
            val parent = collectionDao.getCollectionByIdSync(current)
            current = parent?.parentId
        }
        true
    }

    suspend fun moveCollection(collectionId: String, targetParentId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (!canMoveCollection(collectionId, targetParentId)) {
            return@withContext Result.failure(IllegalArgumentException("Cannot move a collection into itself or its descendants."))
        }
        val existing = collectionDao.getCollectionByIdSync(collectionId)
            ?: return@withContext Result.failure(IllegalStateException("Collection not found"))

        collectionDao.update(
            existing.copy(
                parentId = targetParentId,
                updatedAt = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    }

    /**
     * Deletes a collection and cascades to all its descendants and materials,
     * safely deleting all associated managed files from disk.
     */
    suspend fun deleteCollection(collectionId: String): Unit = withContext(Dispatchers.IO) {
        val allCollections = collectionDao.getAllCollectionsList()
        val toDeleteIds = mutableSetOf(collectionId)

        // Find all recursive descendants
        var addedMore: Boolean
        do {
            addedMore = false
            for (col in allCollections) {
                if (col.parentId != null && col.parentId in toDeleteIds && col.id !in toDeleteIds) {
                    toDeleteIds.add(col.id)
                    addedMore = true
                }
            }
        } while (addedMore)

        // Delete all managed files for materials in these collections
        val allMaterials = materialDao.getAllMaterialsList()
        allMaterials.filter { it.collectionId in toDeleteIds }.forEach { mat ->
            fileManager.deleteManagedFile(mat.relativeFilePath)
            mat.thumbnailPath?.let { fileManager.deleteManagedFile(it) }
            materialDao.deleteById(mat.id)
        }

        // Delete custom covers
        allCollections.filter { it.id in toDeleteIds }.forEach { col ->
            col.customCoverPath?.let { fileManager.deleteManagedFile(it) }
            collectionDao.deleteById(col.id)
        }
    }

    suspend fun importMaterial(collectionId: String, uri: Uri): Result<MaterialEntity> = withContext(Dispatchers.IO) {
        val result = fileManager.importFileFromUri(uri)
        if (result.isFailure) {
            return@withContext Result.failure(result.exceptionOrNull() ?: Exception("File import failed"))
        }

        val fileInfo = result.getOrThrow()
        val material = MaterialEntity(
            id = UUID.randomUUID().toString(),
            collectionId = collectionId,
            displayName = fileInfo.displayName,
            mimeType = fileInfo.mimeType,
            relativeFilePath = fileInfo.relativeFilePath,
            fileSizeBytes = fileInfo.fileSizeBytes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        materialDao.insert(material)
        Result.success(material)
    }

    suspend fun renameMaterial(id: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = materialDao.getMaterialByIdSync(id) ?: return@withContext
        materialDao.update(
            existing.copy(
                displayName = newName.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveMaterial(id: String, newCollectionId: String) = withContext(Dispatchers.IO) {
        val existing = materialDao.getMaterialByIdSync(id) ?: return@withContext
        materialDao.update(
            existing.copy(
                collectionId = newCollectionId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMaterial(material: MaterialEntity) = withContext(Dispatchers.IO) {
        fileManager.deleteManagedFile(material.relativeFilePath)
        material.thumbnailPath?.let { fileManager.deleteManagedFile(it) }
        materialDao.deleteById(material.id)
    }

    fun openMaterial(material: MaterialEntity): Result<Unit> {
        return fileManager.openFileExternally(material.relativeFilePath, material.mimeType)
    }

    fun searchCollections(query: String): Flow<List<CollectionEntity>> = collectionDao.searchCollections(query)

    fun searchMaterials(query: String): Flow<List<MaterialEntity>> = materialDao.searchMaterials(query)

    fun getTotalMaterialsCount(): Flow<Int> = materialDao.getTotalMaterialsCount()
}
