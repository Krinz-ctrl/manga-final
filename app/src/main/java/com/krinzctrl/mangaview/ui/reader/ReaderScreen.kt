package com.krinzctrl.mangaview.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.krinzctrl.mangaview.ui.components.ReaderOverlay
import com.krinzctrl.mangaview.ui.theme.Background
import com.krinzctrl.mangaview.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay

@Composable
fun ReaderScreen(
    mangaId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModelFactory(LocalContext.current))
) {
    val pages by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showOverlay by viewModel.showOverlay.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    LaunchedEffect(mangaId) {
        viewModel.loadPages(mangaId)
    }
    
    LaunchedEffect(currentPage) {
        if (currentPage >= 0 && currentPage < pages.size) {
            listState.animateScrollToItem(currentPage)
        }
    }
    
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleItem = listState.firstVisibleItemIndex
        if (visibleItem != currentPage) {
            viewModel.setCurrentPage(visibleItem)
        }
    }
    
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(2000)
            viewModel.hideOverlay()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val tapY = offset.y
                        val tapX = offset.x
                        val screenWidth = size.width
                        
                        if (tapY < screenHeight.value * 0.2f) {
                            onBack()
                        } else if (tapX > screenWidth * 0.8f) {
                            viewModel.nextPage()
                        } else if (tapX < screenWidth * 0.2f) {
                            viewModel.previousPage()
                        } else {
                            viewModel.toggleOverlay()
                        }
                    }
                )
            }
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = pages,
                    key = { index, page -> page.id }
                ) { index, page ->
                    MangaPage(
                        imagePath = page.imagePath,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        ReaderOverlay(
            isVisible = showOverlay && !isLoading,
            currentPageText = viewModel.currentPageText,
            onBack = onBack
        )
    }
}

@Composable
private fun MangaPage(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("MangaPage", "Rendering page: imagePath=$imagePath")
    AsyncImage(
        model = imagePath,
        contentDescription = "Manga Page",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        onError = { error ->
            android.util.Log.e("MangaPage", "Image load failed: $imagePath", error.result.throwable)
        },
        onSuccess = { success ->
            android.util.Log.d("MangaPage", "Image loaded successfully: $imagePath")
        }
    )
}
