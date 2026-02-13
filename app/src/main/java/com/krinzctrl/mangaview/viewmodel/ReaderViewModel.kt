package com.krinzctrl.mangaview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krinzctrl.mangaview.data.FakeRepository
import com.krinzctrl.mangaview.model.MangaPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val repository: FakeRepository = FakeRepository()
) : ViewModel() {
    
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages.asStateFlow()
    
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPages(mangaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mangaPages = repository.getMangaPages(mangaId)
                _pages.value = mangaPages
                _currentPage.value = 0
            } catch (e: Exception) {
                _pages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
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
