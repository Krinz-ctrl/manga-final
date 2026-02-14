package com.krinzctrl.mangaview.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

class FolderReader {
    
    companion object {
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/webp"
        )
        
        private val SUPPORTED_EXTENSIONS = setOf(
            ".jpg", ".jpeg", ".png", ".webp"
        )
    }
    
    /**
     * Read all images from a folder URI and return sorted DocumentFile list
     */
    fun readFolderImages(context: Context, uri: Uri): List<DocumentFile> {
        android.util.Log.d("FolderReader", "readFolderImages(uri=$uri) START")
        return try {
            val tree = DocumentFile.fromTreeUri(context, uri)
            if (tree == null) {
                android.util.Log.e("FolderReader", "fromTreeUri returned null for uri=$uri")
                return emptyList()
            }

            val all = tree.listFiles().toList()
            android.util.Log.d("FolderReader", "listFiles count=${all.size} uri=$uri")

            val images = all
                .asSequence()
                .filter { it.isFile }
                .filter { file ->
                    val mimeOk = file.type?.lowercase()?.let { SUPPORTED_IMAGE_TYPES.contains(it) } == true
                    val nameOk = file.name
                        ?.lowercase()
                        ?.let { name -> SUPPORTED_EXTENSIONS.any { ext -> name.endsWith(ext) } } == true
                    mimeOk || nameOk
                }
                .sortedBy { it.name?.lowercase().orEmpty() }
                .toList()

            android.util.Log.d("FolderReader", "filtered image count=${images.size} uri=$uri")
            images
        } catch (e: Exception) {
            android.util.Log.e("FolderReader", "readFolderImages FAILED uri=$uri", e)
            emptyList()
        } finally {
            android.util.Log.d("FolderReader", "readFolderImages END uri=$uri")
        }
    }
    
    /**
     * Get the display name of a folder from its URI
     */
    fun getFolderName(context: Context, uri: Uri): String {
        return try {
            val doc = DocumentFile.fromTreeUri(context, uri)
            doc?.name ?: "Imported Manga"
        } catch (_: Exception) {
            "Imported Manga"
        }
    }
    
    /**
     * Check if a DocumentFile is an image
     */
    fun isImageFile(file: DocumentFile): Boolean {
        return file.type?.let { type ->
            SUPPORTED_IMAGE_TYPES.contains(type.lowercase())
        } ?: false || 
        SUPPORTED_EXTENSIONS.any { ext ->
            file.name?.lowercase()?.endsWith(ext) == true
        }
    }
}
