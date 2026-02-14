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
        android.util.Log.d("ThumbnailGenerator", "generateThumbnail START uri=${firstImage.uri}")
        return try {
            // First pass: Decode bounds
            var options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(firstImage.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
            
            android.util.Log.d("ThumbnailGenerator", "Original dimensions: ${options.outWidth}x${options.outHeight}")
            
            // Calculate sample size
            val inSampleSize = calculateInSampleSize(
                options.outWidth, 
                options.outHeight,
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT
            )
            
            // Second pass: Decode bitmap
            options = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inJustDecodeBounds = false
            }
            
            val bitmap = context.contentResolver.openInputStream(firstImage.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
            
            android.util.Log.d("ThumbnailGenerator", "Bitmap decoded: ${bitmap.width}x${bitmap.height}")
            
            // Create thumbnail file
            val thumbnailFile = File(context.filesDir, "thumbnails/${firstImage.name}_thumb.webp")
            thumbnailFile.parentFile?.mkdirs()
            
            android.util.Log.d("ThumbnailGenerator", "Saving thumbnail to: ${thumbnailFile.absolutePath}")
            
            // Save as WEBP
            FileOutputStream(thumbnailFile).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, out)
                android.util.Log.d("ThumbnailGenerator", "Compress success: $success")
            }
            
            bitmap.recycle()
            val path = thumbnailFile.absolutePath
            android.util.Log.d("ThumbnailGenerator", "Thumbnail saved: $path")
            path

        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Thumbnail generation FAILED", e)
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
