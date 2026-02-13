package com.krinzctrl.mangaview.data.model

data class MangaEntity(
    val id: String,
    val title: String,
    val thumbnailPath: String,
    val pageCount: Int,
    val folderUri: String, // Store folder URI instead of encrypted file path
    val encryptedFilePath: String? = null, // Keep for backward compatibility
    val addedDate: Long = System.currentTimeMillis(),
    val lastReadDate: Long? = null,
    val currentPage: Int = 0
)

data class PageRef(
    val id: String,
    val mangaId: String,
    val pageNumber: Int,
    val imageUri: String, // Direct image URI for folder-based storage
    val archivePath: String? = null, // Keep for backward compatibility
    val offset: Long? = null,
    val size: Long? = null,
    val entryName: String
)
