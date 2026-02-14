package com.krinzctrl.mangaview.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InternalStorageManager(private val context: Context) {

    /**
     * Get or create internal directory for a manga
     */
    fun getMangaInternalDir(mangaId: String): File {
        val dir = File(context.filesDir, "manga_internal/$mangaId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copy image from SAF Uri to internal file
     */
    suspend fun copySafImageToInternal(sourceUri: Uri, destFile: File) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get list of image files in internal directory
     */
    fun getInternalMangaImages(internalPath: String): List<File> {
        val dir = File(internalPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        
        return dir.listFiles()?.filter { file ->
             val name = file.name.lowercase()
             name.endsWith(".jpg") || name.endsWith(".jpeg") || 
             name.endsWith(".png") || name.endsWith(".webp")
        }?.sortedBy { it.name } ?: emptyList()
    }
}
