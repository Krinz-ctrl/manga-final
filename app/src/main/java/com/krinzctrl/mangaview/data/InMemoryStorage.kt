package com.krinzctrl.mangaview.data

import com.krinzctrl.mangaview.data.model.MangaEntity
import kotlinx.coroutines.flow.MutableStateFlow

object InMemoryStorage {
    val mangaLibrary = MutableStateFlow<List<MangaEntity>>(emptyList())
}
