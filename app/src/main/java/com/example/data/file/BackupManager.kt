package com.example.data.file

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.CollectionEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.MaterialEntity
import com.example.data.model.UserSettingsEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val fileManager: FileManager
) {

    companion object {
        const val BACKUP_VERSION = 1
        const val MANIFEST_ENTRY = "backup_manifest.json"
        const val COLLECTIONS_ENTRY = "collections.json"
        const val MATERIALS_ENTRY = "materials.json"
        const val TASKS_ENTRY = "tasks.json"
        const val SESSIONS_ENTRY = "focus_sessions.json"
        const val SETTINGS_ENTRY = "settings.json"
        const val FILES_PREFIX = "files/"
    }

    data class BackupStats(
        val collectionsCount: Int,
        val materialsCount: Int,
        val tasksCount: Int,
        val sessionsCount: Int
    )

    suspend fun exportBackup(targetUri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val collections = database.collectionDao().getAllCollectionsList()
            val materials = database.materialDao().getAllMaterialsList()
            val tasks = database.taskDao().getAllTasksList()
            val sessions = database.focusDao().getAllSessionsList()
            val settings = database.settingsDao().getSettingsSync() ?: UserSettingsEntity()

            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream for target URI"))

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 1. Write Manifest
                val manifestJson = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("createdAt", System.currentTimeMillis())
                    put("appName", "ScholarSpace")
                    put("collectionsCount", collections.size)
                    put("materialsCount", materials.size)
                    put("tasksCount", tasks.size)
                    put("sessionsCount", sessions.size)
                }
                writeZipString(zipOut, MANIFEST_ENTRY, manifestJson.toString(2))

                // 2. Write Collections
                val collectionsArray = JSONArray()
                collections.forEach { col ->
                    collectionsArray.put(JSONObject().apply {
                        put("id", col.id)
                        put("name", col.name)
                        put("parentId", col.parentId ?: JSONObject.NULL)
                        put("coverColorHex", col.coverColorHex)
                        put("customCoverPath", col.customCoverPath ?: JSONObject.NULL)
                        put("sortOrder", col.sortOrder)
                        put("createdAt", col.createdAt)
                        put("updatedAt", col.updatedAt)
                    })
                }
                writeZipString(zipOut, COLLECTIONS_ENTRY, collectionsArray.toString(2))

                // 3. Write Materials
                val materialsArray = JSONArray()
                materials.forEach { mat ->
                    materialsArray.put(JSONObject().apply {
                        put("id", mat.id)
                        put("collectionId", mat.collectionId)
                        put("displayName", mat.displayName)
                        put("mimeType", mat.mimeType)
                        put("relativeFilePath", mat.relativeFilePath)
                        put("fileSizeBytes", mat.fileSizeBytes)
                        put("thumbnailPath", mat.thumbnailPath ?: JSONObject.NULL)
                        put("sortOrder", mat.sortOrder)
                        put("createdAt", mat.createdAt)
                        put("updatedAt", mat.updatedAt)
                    })
                }
                writeZipString(zipOut, MATERIALS_ENTRY, materialsArray.toString(2))

                // 4. Write Tasks
                val tasksArray = JSONArray()
                tasks.forEach { t ->
                    tasksArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("notes", t.notes ?: JSONObject.NULL)
                        put("dueDate", t.dueDate ?: JSONObject.NULL)
                        put("isCompleted", t.isCompleted)
                        put("completedAt", t.completedAt ?: JSONObject.NULL)
                        put("isMyDay", t.isMyDay)
                        put("myDayDate", t.myDayDate ?: JSONObject.NULL)
                        put("collectionId", t.collectionId ?: JSONObject.NULL)
                        put("materialId", t.materialId ?: JSONObject.NULL)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
                writeZipString(zipOut, TASKS_ENTRY, tasksArray.toString(2))

                // 5. Write Focus Sessions
                val sessionsArray = JSONArray()
                sessions.forEach { s ->
                    sessionsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("sessionType", s.sessionType)
                        put("plannedDurationSeconds", s.plannedDurationSeconds)
                        put("actualDurationSeconds", s.actualDurationSeconds)
                        put("startedAt", s.startedAt)
                        put("completedAt", s.completedAt ?: JSONObject.NULL)
                        put("isCompleted", s.isCompleted)
                        put("associatedTaskId", s.associatedTaskId ?: JSONObject.NULL)
                        put("associatedCollectionId", s.associatedCollectionId ?: JSONObject.NULL)
                    })
                }
                writeZipString(zipOut, SESSIONS_ENTRY, sessionsArray.toString(2))

                // 6. Write Settings
                val settingsJson = JSONObject().apply {
                    put("themeMode", settings.themeMode)
                    put("focusDurationMinutes", settings.focusDurationMinutes)
                    put("shortBreakMinutes", settings.shortBreakMinutes)
                    put("longBreakMinutes", settings.longBreakMinutes)
                    put("longBreakInterval", settings.longBreakInterval)
                    put("libraryViewMode", settings.libraryViewMode)
                    put("autoStartBreaks", settings.autoStartBreaks)
                    put("soundEnabled", settings.soundEnabled)
                }
                writeZipString(zipOut, SETTINGS_ENTRY, settingsJson.toString(2))

                // 7. Write Managed Files (Materials & Custom Covers)
                val buffer = ByteArray(8 * 1024)
                materials.forEach { mat ->
                    val file = fileManager.resolveFile(mat.relativeFilePath)
                    if (file.exists() && file.isFile) {
                        val entryName = "$FILES_PREFIX${mat.relativeFilePath}"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { fis ->
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                zipOut.write(buffer, 0, read)
                            }
                        }
                        zipOut.closeEntry()
                    }
                }

                collections.forEach { col ->
                    col.customCoverPath?.let { coverPath ->
                        val coverFile = fileManager.resolveFile(coverPath)
                        if (coverFile.exists() && coverFile.isFile) {
                            val entryName = "$FILES_PREFIX$coverPath"
                            zipOut.putNextEntry(ZipEntry(entryName))
                            FileInputStream(coverFile).use { fis ->
                                var read: Int
                                while (fis.read(buffer).also { read = it } != -1) {
                                    zipOut.write(buffer, 0, read)
                                }
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
                zipOut.flush()
            }

            Result.success(
                BackupStats(
                    collectionsCount = collections.size,
                    materialsCount = materials.size,
                    tasksCount = tasks.size,
                    sessionsCount = sessions.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(sourceUri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "backup_staging_${System.currentTimeMillis()}")
        try {
            if (!stagingDir.mkdirs()) {
                return@withContext Result.failure(IllegalStateException("Could not create staging directory"))
            }

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for source backup"))

            // Extract to staging while validating against Zip Slip / Path Traversal
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                val buffer = ByteArray(8 * 1024)

                while (entry != null) {
                    val entryName = entry.name
                    // Security: Reject path traversal attempts
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Malformed archive: Path traversal detected in entry $entryName")
                    }

                    val targetFile = File(stagingDir, entryName)
                    // Ensure canonical path is strictly inside stagingDir
                    if (!targetFile.canonicalPath.startsWith(stagingDir.canonicalPath)) {
                        throw SecurityException("Malformed archive: File outside staging directory")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            var read: Int
                            while (zipIn.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Validate Manifest
            val manifestFile = File(stagingDir, MANIFEST_ENTRY)
            if (!manifestFile.exists()) {
                throw IllegalArgumentException("Invalid backup archive: Missing $MANIFEST_ENTRY")
            }

            val manifestJson = JSONObject(manifestFile.readText())
            val version = manifestJson.optInt("version", 0)
            if (version > BACKUP_VERSION) {
                throw IllegalArgumentException("Backup was created with a newer version ($version). Please update ScholarSpace.")
            }

            // Parse Entities from staging files
            val collectionsFile = File(stagingDir, COLLECTIONS_ENTRY)
            val parsedCollections = mutableListOf<CollectionEntity>()
            if (collectionsFile.exists()) {
                val array = JSONArray(collectionsFile.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedCollections.add(
                        CollectionEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            parentId = if (obj.isNull("parentId")) null else obj.getString("parentId"),
                            coverColorHex = obj.optString("coverColorHex", "#176B63"),
                            customCoverPath = if (obj.isNull("customCoverPath")) null else obj.getString("customCoverPath"),
                            sortOrder = obj.optInt("sortOrder", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val materialsFile = File(stagingDir, MATERIALS_ENTRY)
            val parsedMaterials = mutableListOf<MaterialEntity>()
            if (materialsFile.exists()) {
                val array = JSONArray(materialsFile.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedMaterials.add(
                        MaterialEntity(
                            id = obj.getString("id"),
                            collectionId = obj.getString("collectionId"),
                            displayName = obj.getString("displayName"),
                            mimeType = obj.getString("mimeType"),
                            relativeFilePath = obj.getString("relativeFilePath"),
                            fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                            thumbnailPath = if (obj.isNull("thumbnailPath")) null else obj.getString("thumbnailPath"),
                            sortOrder = obj.optInt("sortOrder", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val tasksFile = File(stagingDir, TASKS_ENTRY)
            val parsedTasks = mutableListOf<TaskEntity>()
            if (tasksFile.exists()) {
                val array = JSONArray(tasksFile.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedTasks.add(
                        TaskEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                            dueDate = if (obj.isNull("dueDate")) null else obj.getLong("dueDate"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            completedAt = if (obj.isNull("completedAt")) null else obj.getLong("completedAt"),
                            isMyDay = obj.optBoolean("isMyDay", false),
                            myDayDate = if (obj.isNull("myDayDate")) null else obj.getLong("myDayDate"),
                            collectionId = if (obj.isNull("collectionId")) null else obj.getString("collectionId"),
                            materialId = if (obj.isNull("materialId")) null else obj.getString("materialId"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val sessionsFile = File(stagingDir, SESSIONS_ENTRY)
            val parsedSessions = mutableListOf<FocusSessionEntity>()
            if (sessionsFile.exists()) {
                val array = JSONArray(sessionsFile.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedSessions.add(
                        FocusSessionEntity(
                            id = obj.getString("id"),
                            sessionType = obj.getString("sessionType"),
                            plannedDurationSeconds = obj.getInt("plannedDurationSeconds"),
                            actualDurationSeconds = obj.getInt("actualDurationSeconds"),
                            startedAt = obj.getLong("startedAt"),
                            completedAt = if (obj.isNull("completedAt")) null else obj.getLong("completedAt"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            associatedTaskId = if (obj.isNull("associatedTaskId")) null else obj.getString("associatedTaskId"),
                            associatedCollectionId = if (obj.isNull("associatedCollectionId")) null else obj.getString("associatedCollectionId")
                        )
                    )
                }
            }

            // Copy extracted staged files to real app storage
            val stagedFilesDir = File(stagingDir, "files")
            if (stagedFilesDir.exists()) {
                stagedFilesDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relPath = file.relativeTo(stagedFilesDir).path
                        val destFile = File(context.filesDir, relPath)
                        destFile.parentFile?.mkdirs()
                        file.copyTo(destFile, overwrite = true)
                    }
                }
            }

            // Transactional database update
            database.withTransaction {
                database.collectionDao().insertAll(parsedCollections)
                database.materialDao().insertAll(parsedMaterials)
                database.taskDao().insertAll(parsedTasks)
                database.focusDao().insertAll(parsedSessions)
            }

            val stats = BackupStats(
                collectionsCount = parsedCollections.size,
                materialsCount = parsedMaterials.size,
                tasksCount = parsedTasks.size,
                sessionsCount = parsedSessions.size
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun writeZipString(zipOut: ZipOutputStream, entryName: String, content: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        zipOut.write(content.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }
}
