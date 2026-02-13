package com.krinzctrl.mangaview.model

data class MangaModel(
    val id: String,
    val title: String,
    val thumbnailPath: String,
    val pageCount: Int,
    val addedDate: Long = System.currentTimeMillis()
)

data class MangaPage(
    val id: String,
    val mangaId: String,
    val pageNumber: Int,
    val imagePath: String
)
