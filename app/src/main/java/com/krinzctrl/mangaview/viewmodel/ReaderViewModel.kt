package com.krinzctrl.mangaview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krinzctrl.mangaview.data.model.PageRef
import com.krinzctrl.mangaview.data.repository.MangaRepository
import com.krinzctrl.mangaview.data.storage.EncryptionManager
import com.krinzctrl.mangaview.data.storage.FileStorageManager
import com.krinzctrl.mangaview.data.storage.ArchiveReader
import com.krinzctrl.mangaview.model.MangaPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class ReaderViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository = MangaRepository(
        context = application,
        encryptionManager = EncryptionManager(application),
        fileStorageManager = FileStorageManager(application),
        archiveReader = ArchiveReader(application)
    )
    
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages.asStateFlow()
    
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _pageRefs = MutableStateFlow<List<PageRef>>(emptyList())

    fun loadPages(mangaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val pageReferences = repository.openManga(mangaId)
                _pageRefs.value = pageReferences
                
                // Convert to MangaPage for UI compatibility
                val mangaPages = pageReferences.map { pageRef ->
                    MangaPage(
                        id = pageRef.id,
                        mangaId = pageRef.mangaId,
                        pageNumber = pageRef.pageNumber,
                        imagePath = "stream://${pageRef.id}" // Special stream indicator
                    )
                }
                
                _pages.value = mangaPages
                _currentPage.value = 0
            } catch (e: Exception) {
                _pages.value = emptyList()
                _pageRefs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getPageStream(pageId: String): InputStream? {
        val pageRef = _pageRefs.value.find { it.id == pageId }
        return if (pageRef != null) {
            try {
                repository.getPageStream(pageRef.mangaId, pageRef)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun setCurrentPage(page: Int) {
        if (page >= 0 && page < _pages.value.size) {
            _currentPage.value = page
        }
    }

    fun toggleOverlay() {
        _showOverlay.value = !_showOverlay.value
    }

    fun hideOverlay() {
        _showOverlay.value = false
    }

    fun showOverlay() {
        _showOverlay.value = true
    }

    fun nextPage() {
        val nextPage = _currentPage.value + 1
        if (nextPage < _pages.value.size) {
            setCurrentPage(nextPage)
        }
    }

    fun previousPage() {
        val prevPage = _currentPage.value - 1
        if (prevPage >= 0) {
            setCurrentPage(prevPage)
        }
    }

    val currentPageText: String
        get() = "${_currentPage.value + 1} of ${_pages.value.size}"
}
