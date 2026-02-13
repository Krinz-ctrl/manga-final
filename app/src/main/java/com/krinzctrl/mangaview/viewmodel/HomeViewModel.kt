package com.krinzctrl.mangaview.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krinzctrl.mangaview.data.model.MangaEntity
import com.krinzctrl.mangaview.data.repository.MangaRepository
import com.krinzctrl.mangaview.data.storage.EncryptionManager
import com.krinzctrl.mangaview.data.storage.FileStorageManager
import com.krinzctrl.mangaview.data.storage.ArchiveReader
import com.krinzctrl.mangaview.model.MangaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository = MangaRepository(
        context = application,
        encryptionManager = EncryptionManager(application),
        fileStorageManager = FileStorageManager(application),
        archiveReader = ArchiveReader(application)
    )
    
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
            repository.getLibrary().collect { entities ->
                _mangaList.value = entities.map { entity ->
                    MangaModel(
                        id = entity.id,
                        title = entity.title,
                        thumbnailPath = entity.thumbnailPath,
                        pageCount = entity.pageCount
                    )
                }
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
        // Legacy method for backward compatibility
        importFolder(uri)
    }
    
    fun importFolder(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = repository.importFolder(uri)
                if (result.isSuccess) {
                    onImportSheetDismissed()
                    // Library will be updated automatically via Flow
                } else {
                    // Handle error - could show toast or snackbar
                    result.exceptionOrNull()?.printStackTrace()
                }
            } catch (e: Exception) {
                // Handle error - could show toast or snackbar
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun importManga(manga: MangaModel): Boolean {
        return try {
            // Legacy method for compatibility
            onImportSheetDismissed()
            true
        } catch (e: Exception) {
            false
        }
    }
}
