package com.krinzctrl.mangaview.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ArchiveReader(private val context: Context) {
    
    data class PageRef(
        val id: String,
        val mangaId: String,
        val pageNumber: Int,
        val entryName: String
    )
    
    private val supportedExtensions = setOf(".jpg", ".jpeg", ".png", ".webp")
    
    fun streamPages(encryptedStream: InputStream): List<PageRef> {
        return try {
            val zipStream = ZipInputStream(encryptedStream)
            val pages = mutableListOf<PageRef>()
            var entry: ZipEntry? = zipStream.nextEntry
            var pageNumber = 1
            
            while (entry != null) {
                val fileName = entry.name
                if (isImageFile(fileName)) {
                    pages.add(
                        PageRef(
                            id = fileName,
                            mangaId = "",
                            pageNumber = pageNumber++,
                            entryName = fileName
                        )
                    )
                }
                entry = zipStream.nextEntry
            }
            
            pages
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getPageStream(encryptedStream: InputStream, pageRef: PageRef): InputStream {
        val zipStream = ZipInputStream(encryptedStream)
        var entry: ZipEntry? = zipStream.nextEntry
        
        while (entry != null && entry.name != pageRef.entryName) {
            entry = zipStream.nextEntry
        }
        
        return if (entry != null && entry.name == pageRef.entryName) {
            zipStream
        } else {
            throw IllegalArgumentException("Page not found: ${pageRef.entryName}")
        }
    }
    
    fun generateThumbnail(pageRef: PageRef, encryptedStream: InputStream): String {
        val pageStream = getPageStream(encryptedStream, pageRef)
        val bitmap = decodeBitmapForThumbnail(pageStream)
        
        val thumbnailFile = File(context.filesDir, "thumbnails/${pageRef.mangaId}.webp")
        thumbnailFile.parentFile?.mkdirs()
        
        FileOutputStream(thumbnailFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
        
        return thumbnailFile.absolutePath
    }
    
    private fun decodeBitmapForThumbnail(inputStream: InputStream): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        // Reset stream and get bounds
        inputStream.mark(inputStream.available())
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.reset()
        
        // Calculate sample size for ~400x533 thumbnail (3:4 ratio)
        val targetWidth = 400
        val targetHeight = 533
        
        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight,
            targetWidth, targetHeight
        )
        options.inJustDecodeBounds = false
        
        return BitmapFactory.decodeStream(inputStream, null, options) 
            ?: throw IllegalArgumentException("Failed to decode image")
    }
    
    private fun calculateInSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && 
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return supportedExtensions.contains(".$extension")
    }
}
