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
        return try {
            val treeDocumentFile = DocumentFile.fromTreeUri(context, uri)
                ?: return emptyList()
            
            // List all files in the folder
            val allFiles = treeDocumentFile.listFiles()
                ?: return emptyList()
            
            // Filter only image files
            val imageFiles = allFiles.filter { file ->
                file.type?.let { type -> 
                    SUPPORTED_IMAGE_TYPES.contains(type.lowercase())
                } ?: false || 
                SUPPORTED_EXTENSIONS.any { ext ->
                    file.name?.lowercase()?.endsWith(ext) == true
                }
            }
            
            // Sort by name ascending (001.jpg, 002.jpg, etc.)
            imageFiles.sortedBy { it.name?.lowercase() ?: "" }
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get the display name of a folder from its URI
     */
    fun getFolderName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    "Imported Manga"
                }
            } ?: "Imported Manga"
        } catch (e: Exception) {
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
