package com.krinzctrl.mangaview.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.krinzctrl.mangaview.data.model.MangaEntity
import com.krinzctrl.mangaview.data.model.PageRef
import com.krinzctrl.mangaview.data.storage.EncryptionManager
import com.krinzctrl.mangaview.data.storage.FileStorageManager
import com.krinzctrl.mangaview.data.storage.ArchiveReader
import com.krinzctrl.mangaview.data.storage.ThumbnailGenerator
import com.krinzctrl.mangaview.data.storage.FolderReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.File
import java.util.UUID
import java.io.FileInputStream
import java.io.FileOutputStream

class MangaRepository(
    private val context: Context,
    private val encryptionManager: EncryptionManager,
    private val fileStorageManager: FileStorageManager,
    private val archiveReader: ArchiveReader,
    private val thumbnailGenerator: ThumbnailGenerator = ThumbnailGenerator(context)
) {
    
    private val _mangaLibrary = MutableStateFlow<List<MangaEntity>>(emptyList())
    val mangaLibrary: Flow<List<MangaEntity>> = _mangaLibrary.asStateFlow()
    
    init {
        loadExistingLibrary()
    }
    
    private fun loadExistingLibrary() {
        // For now, we'll use in-memory storage
        // In a real app, this would load from a database
    }
    
    suspend fun importManga(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mangaId = System.currentTimeMillis().toString()
            
            // Step 1: Copy file from URI to temp location
            val tempFile = File(context.cacheDir, "temp_$mangaId")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Step 2: Encrypt file and save to internal storage
            val encryptedFile = fileStorageManager.createEncryptedFile(mangaId)
            FileInputStream(tempFile).use { input ->
                encryptionManager.encrypt(input, FileOutputStream(encryptedFile))
            }
            
            // Step3: Extract thumbnail
            val thumbnailFile = fileStorageManager.createThumbnailFile(mangaId)
            try {
                FileInputStream(tempFile).use { input ->
                    val pages = archiveReader.streamPages(input)
                    if (pages.isNotEmpty()) {
                        val firstPage = pages.first()
                        archiveReader.generateThumbnail(firstPage, input)
                    }
                }
            } catch (e: Exception) {
                // Fallback if thumbnail extraction fails
            }
            
            // Step 4: Create manga entity
            val fileName = getFileName(uri)
            val pageCount = try {
                val pages = archiveReader.streamPages(FileInputStream(tempFile))
                pages.size
            } catch (e: Exception) {
                0
            }
            val mangaEntity = MangaEntity(
                id = mangaId,
                title = fileName,
                thumbnailPath = thumbnailFile.absolutePath,
                pageCount = pageCount,
                folderUri = "",
                encryptedFilePath = encryptedFile.absolutePath
            )
            
            // Step 5: Add to library
            val currentLibrary = _mangaLibrary.value.toMutableList()
            currentLibrary.add(mangaEntity)
            _mangaLibrary.value = currentLibrary
            
            // Step 6: Clean up temp file
            tempFile.delete()
            
            Result.success(mangaId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import manga from folder containing images
     */
    suspend fun importFolder(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        android.util.Log.d("MangaRepository", "importFolder(uri=$uri) START")
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                android.util.Log.d("MangaRepository", "Persistable permission granted for uri=$uri")
            } catch (se: SecurityException) {
                android.util.Log.e("MangaRepository", "takePersistableUriPermission failed for uri=$uri", se)
                // Continue; sometimes picker grants transient permission, but this may break later reads.
            }

            val folderReader = FolderReader()
            val images = folderReader.readFolderImages(context, uri)
            android.util.Log.d("MangaRepository", "Folder images count=${images.size} uri=$uri")

            if (images.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No images found in folder"))
            }

            val first = images.first()
            val thumbnailPath = try {
                thumbnailGenerator.generateThumbnail(first)
            } catch (e: Exception) {
                android.util.Log.e("MangaRepository", "Thumbnail generation failed", e)
                ""
            }

            val mangaId = UUID.randomUUID().toString()
            val title = folderReader.getFolderName(context, uri)

            val mangaEntity = MangaEntity(
                id = mangaId,
                title = title,
                thumbnailPath = thumbnailPath ?: "",
                pageCount = images.size,
                folderUri = uri.toString(),
                encryptedFilePath = null,
                addedDate = System.currentTimeMillis(),
                lastReadDate = null,
                currentPage = 0
            )

            val newList = _mangaLibrary.value.toMutableList().apply { add(mangaEntity) }.toList()
            _mangaLibrary.value = newList
            android.util.Log.d("MangaRepository", "Library updated. newSize=${newList.size} addedId=$mangaId")

            Result.success(mangaId)
        } catch (e: Exception) {
            android.util.Log.e("MangaRepository", "importFolder FAILED", e)
            Result.failure(e)
        } finally {
            android.util.Log.d("MangaRepository", "importFolder END uri=$uri")
        }
    }
    
    /**
     * Get folder images for reader
     */
    suspend fun getFolderImages(mangaId: String): List<PageRef> = withContext(Dispatchers.IO) {
        try {
            val mangaEntity = _mangaLibrary.value.find { it.id == mangaId }
                ?: return@withContext emptyList()
            
            if (mangaEntity.folderUri.isEmpty()) {
                return@withContext emptyList()
            }
            
            val folderUri = Uri.parse(mangaEntity.folderUri)
            val treeDocumentFile = DocumentFile.fromTreeUri(context, folderUri)
            
            val allFiles = treeDocumentFile?.listFiles()
                ?: return@withContext emptyList()
                
            // Filter only image files
            val imageFiles = allFiles.filter { file ->
                file.type?.let { type -> 
                    setOf("image/jpeg", "image/jpg", "image/png", "image/webp").contains(type.lowercase())
                } ?: false || 
                        setOf(".jpg", ".jpeg", ".png", ".webp").any { ext ->
                            file.name?.lowercase()?.endsWith(ext) == true
                        }
                }
            
            // Sort by name ascending (001.jpg, 002.jpg, etc.)
            val sortedFiles = imageFiles.sortedBy { it.name?.lowercase() ?: "" }
            
            sortedFiles.mapIndexed { index, file ->
                PageRef(
                    id = file.uri.toString(),
                    mangaId = mangaId,
                    pageNumber = index + 1,
                    imageUri = file.uri.toString(),
                    entryName = file.name ?: "page_${index + 1}"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getLibrary(): Flow<List<MangaEntity>> {
        return mangaLibrary
    }
    
    suspend fun openManga(mangaId: String): List<PageRef> = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(context.filesDir, "manga/${mangaId}.mgv")
            if (!encryptedFile.exists()) {
                // Try folder-based loading
                return@withContext getFolderImages(mangaId)
            }
            
            val encryptedStream = FileInputStream(encryptedFile)
            val decryptedStream = encryptionManager.decrypt(encryptedStream)
            
            val archivePages = archiveReader.streamPages(decryptedStream)
            
            // Convert ArchiveReader.PageRef to repository PageRef
            archivePages.map { page ->
                PageRef(
                    id = page.id,
                    mangaId = mangaId,
                    pageNumber = page.pageNumber,
                    imageUri = page.id,
                    entryName = page.entryName
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getPageStream(mangaId: String, pageRef: PageRef): InputStream? = withContext(Dispatchers.IO) {
        try {
            // Check if it's a folder-based manga
            val mangaEntity = _mangaLibrary.value.find { it.id == mangaId }
            if (mangaEntity?.folderUri?.isNotEmpty() == true) {
                // Return direct URI for folder images
                return@withContext context.contentResolver.openInputStream(Uri.parse(pageRef.imageUri))
            }
            
            // Fallback to archive-based
            val encryptedFile = File(context.filesDir, "manga/${mangaId}.mgv")
            if (!encryptedFile.exists()) {
                return@withContext null
            }
            
            val encryptedStream = FileInputStream(encryptedFile)
            val decryptedStream = encryptionManager.decrypt(encryptedStream)
            
            val archivePageRef = ArchiveReader.PageRef(
                id = pageRef.id,
                mangaId = pageRef.mangaId,
                pageNumber = pageRef.pageNumber,
                entryName = pageRef.entryName
            )
            archiveReader.getPageStream(decryptedStream, archivePageRef)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun deleteManga(mangaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete encrypted file
            val encryptedFile = File(context.filesDir, "manga/${mangaId}.mgv")
            if (encryptedFile.exists()) {
                encryptedFile.delete()
            }
            
            // Delete thumbnail
            val thumbnailFile = File(context.filesDir, "thumbnails/${mangaId}.webp")
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }
            
            // Remove from library
            val currentLibrary = _mangaLibrary.value.toMutableList()
            currentLibrary.removeAll { it.id == mangaId }
            _mangaLibrary.value = currentLibrary
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getFileName(uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    cursor.getString(displayName) ?: "Imported Manga"
                } else {
                    "Imported Manga"
                }
            } ?: "Imported Manga"
        } catch (e: Exception) {
            "Imported Manga"
        }
    }
}
