package com.krinzctrl.mangaview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krinzctrl.mangaview.ui.home.HomeScreen
import com.krinzctrl.mangaview.ui.reader.ReaderScreen
import com.krinzctrl.mangaview.ui.theme.MangaViewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MangaViewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MangaNavigation()
                }
            }
        }
    }
}

@Composable
fun MangaNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + 
            scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + 
            scaleOut(
                targetScale = 1.1f,
                animationSpec = tween(300)
            )
        }
    ) {
        composable("home") {
            HomeScreen(
                onMangaClick = { mangaId ->
                    navController.navigate("reader/$mangaId")
                }
            )
        }
        
        composable(
            route = "reader/{mangaId}",
            arguments = listOf(
                navArgument("mangaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: return@composable
            
            ReaderScreen(
                mangaId = mangaId,
                onBack = { navController.navigateUp() }
            )
        }
    }
}
