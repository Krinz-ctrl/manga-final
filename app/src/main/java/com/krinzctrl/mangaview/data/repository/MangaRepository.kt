package com.krinzctrl.mangaview.data.repository

import android.content.Context
import android.net.Uri
import com.krinzctrl.mangaview.data.model.MangaEntity
import com.krinzctrl.mangaview.data.model.PageRef
import com.krinzctrl.mangaview.data.storage.ArchiveReader
import com.krinzctrl.mangaview.data.storage.EncryptionManager
import com.krinzctrl.mangaview.data.storage.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MangaRepository(
    private val context: Context,
    private val encryptionManager: EncryptionManager,
    private val fileStorageManager: FileStorageManager,
    private val archiveReader: ArchiveReader
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
                fileStorageManager.copyUriToFile(input, tempFile)
            } ?: throw IllegalArgumentException("Failed to open URI")
            
            // Step 2: Encrypt the file
            val encryptedFile = fileStorageManager.createEncryptedFile(mangaId)
            FileInputStream(tempFile).use { input ->
                FileOutputStream(encryptedFile).use { output ->
                    encryptionManager.encrypt(input, output)
                }
            }
            
            // Step 3: Extract thumbnail
            val thumbnailPath = extractThumbnail(mangaId)
            
            // Step 4: Count pages
            val pageCount = countPages(mangaId)
            
            // Step 5: Create manga entity
            val mangaEntity = MangaEntity(
                id = mangaId,
                title = extractTitleFromUri(uri) ?: "Manga $mangaId",
                thumbnailPath = thumbnailPath,
                encryptedFilePath = encryptedFile.absolutePath,
                pageCount = pageCount
            )
            
            // Step 6: Add to library
            val currentLibrary = _mangaLibrary.value.toMutableList()
            currentLibrary.add(mangaEntity)
            _mangaLibrary.value = currentLibrary
            
            // Step 7: Clean up temp file
            tempFile.delete()
            
            Result.success(mangaId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLibrary(): Flow<List<MangaEntity>> {
        return mangaLibrary
    }
    
    suspend fun openManga(mangaId: String): List<PageRef> = withContext(Dispatchers.IO) {
        try {
            val encryptedStream = fileStorageManager.getEncryptedFileStream(mangaId)
            val decryptedStream = encryptionManager.decrypt(encryptedStream)
            
            val archivePages = archiveReader.streamPages(decryptedStream)
            
            // Convert ArchiveReader.PageRef to repository PageRef
            archivePages.map { page ->
                PageRef(
                    id = page.id,
                    mangaId = mangaId,
                    pageNumber = page.pageNumber,
                    entryName = page.entryName
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getPageStream(mangaId: String, pageRef: PageRef) = withContext(Dispatchers.IO) {
        val encryptedStream = fileStorageManager.getEncryptedFileStream(mangaId)
        val decryptedStream = encryptionManager.decrypt(encryptedStream)
        
        // Convert repository PageRef to ArchiveReader.PageRef
        val archivePageRef = ArchiveReader.PageRef(
            id = pageRef.id,
            mangaId = pageRef.mangaId,
            pageNumber = pageRef.pageNumber,
            entryName = pageRef.entryName
        )
        
        archiveReader.getPageStream(decryptedStream, archivePageRef)
    }
    
    private suspend fun extractThumbnail(mangaId: String): String = withContext(Dispatchers.IO) {
        val encryptedStream = fileStorageManager.getEncryptedFileStream(mangaId)
        val decryptedStream = encryptionManager.decrypt(encryptedStream)
        
        val thumbnailFile = fileStorageManager.createThumbnailFile(mangaId)
        
        // Get first page and generate thumbnail
        val pages = archiveReader.streamPages(decryptedStream)
        if (pages.isNotEmpty()) {
            val firstPage = pages.first().copy(mangaId = mangaId)
            archiveReader.extractThumbnail(decryptedStream) ?: ""
        } else {
            ""
        }
    }
    
    private suspend fun countPages(mangaId: String): Int = withContext(Dispatchers.IO) {
        val encryptedStream = fileStorageManager.getEncryptedFileStream(mangaId)
        val decryptedStream = encryptionManager.decrypt(encryptedStream)
        archiveReader.streamPages(decryptedStream).size
    }
    
    private fun extractTitleFromUri(uri: Uri): String? {
        return uri.lastPathSegment?.substringBeforeLast('.')?.replace("_", " ")
    }
    
    suspend fun deleteManga(mangaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fileStorageManager.deleteMangaFiles(mangaId)
            
            val currentLibrary = _mangaLibrary.value.toMutableList()
            currentLibrary.removeAll { it.id == mangaId }
            _mangaLibrary.value = currentLibrary
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
