package com.krinzctrl.mangaview.data.model

data class MangaEntity(
    val id: String,
    val title: String,
    val thumbnailPath: String,
    val encryptedFilePath: String,
    val pageCount: Int,
    val addedDate: Long = System.currentTimeMillis(),
    val lastReadDate: Long? = null,
    val currentPage: Int = 0
)

data class PageRef(
    val id: String,
    val mangaId: String,
    val pageNumber: Int,
    val entryName: String
)
