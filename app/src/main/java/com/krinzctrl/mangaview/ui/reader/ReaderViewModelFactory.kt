package com.krinzctrl.mangaview.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.krinzctrl.mangaview.viewmodel.ReaderViewModel

class ReaderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReaderViewModel(context.applicationContext as android.app.Application) as T
    }
}
