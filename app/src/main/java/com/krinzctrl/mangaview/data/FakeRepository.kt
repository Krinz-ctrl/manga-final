package com.krinzctrl.mangaview.data

import com.krinzctrl.mangaview.model.MangaModel
import com.krinzctrl.mangaview.model.MangaPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRepository {
    private val _mangaList = MutableStateFlow<List<MangaModel>>(emptyList())
    val mangaList: Flow<List<MangaModel>> = _mangaList.asStateFlow()

    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: Flow<List<MangaPage>> = _pages.asStateFlow()

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val sampleManga = listOf(
            MangaModel(
                id = "1",
                title = "Sample Manga 1",
                thumbnailPath = "https://picsum.photos/seed/manga1/300/400.jpg",
                pageCount = 10
            ),
            MangaModel(
                id = "2", 
                title = "Sample Manga 2",
                thumbnailPath = "https://picsum.photos/seed/manga2/300/400.jpg",
                pageCount = 15
            ),
            MangaModel(
                id = "3",
                title = "Sample Manga 3", 
                thumbnailPath = "https://picsum.photos/seed/manga3/300/400.jpg",
                pageCount = 8
            )
        )
        _mangaList.value = sampleManga

        val samplePages = sampleManga.flatMap { manga ->
            (1..manga.pageCount).map { pageNum ->
                MangaPage(
                    id = "${manga.id}_$pageNum",
                    mangaId = manga.id,
                    pageNumber = pageNum,
                    imagePath = "https://picsum.photos/seed/${manga.id}_$pageNum/800/1200.jpg"
                )
            }
        }
        _pages.value = samplePages
    }

    suspend fun addManga(manga: MangaModel): Boolean {
        val currentList = _mangaList.value.toMutableList()
        currentList.add(manga)
        _mangaList.value = currentList
        return true
    }

    suspend fun getMangaPages(mangaId: String): List<MangaPage> {
        return _pages.value.filter { it.mangaId == mangaId }
    }
}
