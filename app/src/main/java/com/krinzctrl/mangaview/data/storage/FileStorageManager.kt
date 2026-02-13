package com.krinzctrl.mangaview.data.storage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class FileStorageManager(private val context: Context) {
    
    companion object {
        private const val MANGA_FOLDER = "manga"
        private const val THUMBNAILS_FOLDER = "thumbnails"
        private const val ENCRYPTED_EXTENSION = ".mgv"
        private const val THUMBNAIL_EXTENSION = ".webp"
    }
    
    private val mangaDir: File by lazy {
        File(context.filesDir, MANGA_FOLDER).apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val thumbnailsDir: File by lazy {
        File(context.filesDir, THUMBNAILS_FOLDER).apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun createEncryptedFile(mangaId: String): File {
        return File(mangaDir, "$mangaId$ENCRYPTED_EXTENSION")
    }
    
    fun createThumbnailFile(mangaId: String): File {
        return File(thumbnailsDir, "$mangaId$THUMBNAIL_EXTENSION")
    }
    
    fun getEncryptedFile(mangaId: String): File {
        return File(mangaDir, "$mangaId$ENCRYPTED_EXTENSION")
    }
    
    fun getThumbnailFile(mangaId: String): File {
        return File(thumbnailsDir, "$mangaId$THUMBNAIL_EXTENSION")
    }
    
    fun copyUriToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    fun getEncryptedFileStream(mangaId: String): InputStream {
        return FileInputStream(getEncryptedFile(mangaId))
    }
    
    fun deleteMangaFiles(mangaId: String) {
        getEncryptedFile(mangaId).delete()
        getThumbnailFile(mangaId).delete()
    }
    
    fun getAllMangaFiles(): List<File> {
        return mangaDir.listFiles { file -> 
            file.extension == "mgv" 
        }?.toList() ?: emptyList()
    }
}
