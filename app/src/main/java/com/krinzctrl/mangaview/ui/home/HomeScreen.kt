package com.krinzctrl.mangaview.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krinzctrl.mangaview.model.MangaModel
import com.krinzctrl.mangaview.ui.components.ImportSheet
import com.krinzctrl.mangaview.ui.components.MangaThumbnailCard
import com.krinzctrl.mangaview.ui.theme.*
import com.krinzctrl.mangaview.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onMangaClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val mangaList by viewModel.mangaList.collectAsState()
    val showImportSheet by viewModel.showImportSheet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        android.util.Log.d("HomeScreen", "OpenDocumentTree result uri=$uri")
        uri?.let { picked ->
            viewModel.importFolder(picked)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (mangaList.isEmpty()) {
            // Show empty state when no manga
            EmptyState(
                onImportClick = viewModel::onImportClicked,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Show manga grid when items exist
            MangaGrid(
                mangaList = mangaList,
                onMangaClick = onMangaClick,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // FAB with folder picker
        FloatingActionButton(
            onClick = { 
                folderPickerLauncher.launch(null)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp),
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = WhitePrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import",
                modifier = Modifier.rotate(90f)
            )
        }
        
        if (showImportSheet) {
            ImportSheet(
                onDismiss = viewModel::onImportSheetDismissed,
                onImport = { 
                    folderPickerLauncher.launch(null)
                }
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // Block clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Importing manga...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = WhiteSecondary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tap + to import manga",
            color = WhiteSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun MangaGrid(
    mangaList: List<MangaModel>,
    onMangaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = mangaList,
            key = { it.id }
        ) { manga ->
            MangaThumbnailCard(
                title = manga.title,
                thumbnailPath = manga.thumbnailPath,
                onClick = { onMangaClick(manga.id) },
                onLongPress = { }
            )
        }
    }
}
