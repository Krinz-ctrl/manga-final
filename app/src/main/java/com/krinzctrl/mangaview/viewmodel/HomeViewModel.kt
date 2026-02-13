package com.krinzctrl.mangaview.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krinzctrl.mangaview.data.FakeRepository
import com.krinzctrl.mangaview.model.MangaModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: FakeRepository = FakeRepository()
) : ViewModel() {
    
    private val _mangaList = MutableStateFlow<List<MangaModel>>(emptyList())
    val mangaList: StateFlow<List<MangaModel>> = _mangaList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showImportSheet = MutableStateFlow(false)
    val showImportSheet: StateFlow<Boolean> = _showImportSheet.asStateFlow()

    init {
        loadMangaList()
    }

    private fun loadMangaList() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.mangaList.collect { manga ->
                _mangaList.value = manga
                _isLoading.value = false
            }
        }
    }

    fun onImportClicked() {
        _showImportSheet.value = true
    }

    fun onImportSheetDismissed() {
        _showImportSheet.value = false
    }

    fun importManga(uri: Uri) {
        // For now, just show the import sheet
        onImportClicked()
    }

    suspend fun importManga(manga: MangaModel): Boolean {
        return try {
            repository.addManga(manga)
            onImportSheetDismissed()
            true
        } catch (e: Exception) {
            false
        }
    }
}
