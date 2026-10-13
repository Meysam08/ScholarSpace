package com.example.data.file

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FileManager(private val context: Context) {

    private val baseFilesDir: File = context.filesDir.resolve("library_files")
    private val materialsDir: File = baseFilesDir.resolve("materials")
    private val coversDir: File = baseFilesDir.resolve("covers")

    init {
        if (!materialsDir.exists()) materialsDir.mkdirs()
        if (!coversDir.exists()) coversDir.mkdirs()
    }

    data class ImportedFileResult(
        val displayName: String,
        val mimeType: String,
        val relativeFilePath: String,
        val fileSizeBytes: Long
    )

    /**
     * Copies a file chosen via SAF into app-managed internal storage using streamed I/O.
     * Prevents loading the whole file into RAM. Cleans up temp file on failure.
     */
    suspend fun importFileFromUri(uri: Uri): Result<ImportedFileResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val (displayName, size) = queryFileInfo(uri)
            val mimeType = queryMimeType(uri, displayName)

            val safeId = UUID.randomUUID().toString()
            val extension = displayName.substringAfterLast('.', "")
            val targetFileName = if (extension.isNotEmpty()) "$safeId.$extension" else safeId
            val targetFile = File(materialsDir, targetFileName)

            val tempFile = File(materialsDir, "$targetFileName.tmp")

            var totalBytesCopied = 0L
            val buffer = ByteArray(8 * 1024)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesCopied += bytesRead
                    }
                    outputStream.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for $uri"))

            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if renameTo fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val finalSize = if (size > 0) size else targetFile.length()
            val relativePath = "library_files/materials/$targetFileName"

            Result.success(
                ImportedFileResult(
                    displayName = displayName,
                    mimeType = mimeType,
                    relativeFilePath = relativePath,
                    fileSizeBytes = finalSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports a custom collection cover image into the covers directory.
     */
    suspend fun importCoverImage(uri: Uri, collectionId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetFileName = "cover_${collectionId}.jpg"
            val targetFile = File(coversDir, targetFileName)
            val tempFile = File(coversDir, "${targetFileName}.tmp")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open image stream"))

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val relativePath = "library_files/covers/$targetFileName"
            Result.success(relativePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves a relative path to an absolute File.
     */
    fun resolveFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    /**
     * Permanently deletes a managed file if it exists.
     */
    suspend fun deleteManagedFile(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = resolveFile(relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens an external viewer intent for a given material using secure FileProvider.
     */
    fun openFileExternally(relativePath: String, mimeType: String): Result<Unit> {
        val file = resolveFile(relativePath)
        if (!file.exists()) {
            return Result.failure(IllegalStateException("File does not exist in local storage"))
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(IllegalStateException("No compatible application installed to open this file type ($mimeType)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var displayName = "file_${System.currentTimeMillis()}"
        var size = 0L

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (ignored: Exception) {}

        return Pair(displayName, size)
    }

    private fun queryMimeType(uri: Uri, displayName: String): String {
        val resolverType = context.contentResolver.getType(uri)
        if (!resolverType.isNullOrBlank()) return resolverType

        val extension = displayName.substringAfterLast('.', "")
        if (extension.isNotEmpty()) {
            val mapType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            if (!mapType.isNullOrBlank()) return mapType
        }

        return "application/octet-stream"
    }

    fun calculateTotalStorageUsage(): Long {
        return getFolderSize(baseFilesDir)
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var length = 0L
        file.listFiles()?.forEach { child ->
            length += getFolderSize(child)
        }
        return length
    }
}
