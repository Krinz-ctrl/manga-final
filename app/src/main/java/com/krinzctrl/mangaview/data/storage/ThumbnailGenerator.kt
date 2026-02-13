package com.krinzctrl.mangaview.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class ThumbnailGenerator(private val context: Context) {
    
    companion object {
        private const val THUMBNAIL_WIDTH = 400
        private const val THUMBNAIL_HEIGHT = 533
        private const val THUMBNAIL_QUALITY = 85
    }
    
    /**
     * Generate thumbnail from the first image in a folder
     */
    fun generateThumbnail(firstImage: DocumentFile): String? {
        return try {
            // Open input stream from DocumentFile
            val inputStream = context.contentResolver.openInputStream(firstImage.uri)
                ?: return null
            
            inputStream.use { stream ->
                // Get original dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(
                    options.outWidth, 
                    options.outHeight,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT
                )
                options.inJustDecodeBounds = false
                
                // Reset stream and decode with sampling
                stream.reset()
                val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    ?: return null
                
                // Create thumbnail file
                val thumbnailFile = File(context.filesDir, "thumbnails/${firstImage.name}_thumb.webp")
                thumbnailFile.parentFile?.mkdirs()
                
                // Save as WEBP
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, out)
                }
                
                bitmap.recycle()
                thumbnailFile.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate appropriate sample size for memory-efficient decoding
     */
    private fun calculateInSampleSize(
        width: Int, 
        height: Int, 
        reqWidth: Int, 
        reqHeight: Int
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
}
