package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.BookmarksScreen
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.ReaderScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.LanguageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.init(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfReaderApp()
                }
            }
        }
    }
}

@Composable
fun PdfReaderApp(mainViewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val pdfs by mainViewModel.pdfs.collectAsState()
    val bookmarks by mainViewModel.allBookmarks.collectAsState()
    val viewMode by mainViewModel.viewMode.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPdfSelected = { id ->
                    navController.navigate("reader/$id")
                },
                onNavigateToBookmarks = {
                    navController.navigate("bookmarks")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                viewModel = mainViewModel
            )
        }

        composable(
            route = "reader/{pdfId}?page={page}",
            arguments = listOf(
                navArgument("pdfId") { type = NavType.IntType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getInt("pdfId") ?: return@composable
            val pageArg = backStackEntry.arguments?.getInt("page") ?: -1
            val initialPage = if (pageArg >= 0) pageArg else null

            ReaderScreen(
                pdfId = pdfId,
                initialPage = initialPage,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("bookmarks") {
            BookmarksScreen(
                bookmarks = bookmarks,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPage = { pdfId, pageIndex ->
                    navController.navigate("reader/$pdfId?page=$pageIndex")
                },
                onDeleteBookmark = { id ->
                    mainViewModel.deleteBookmark(id)
                },
                onUpdateBookmark = { bm ->
                    mainViewModel.updateBookmark(bm)
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarks = {
                    navController.navigate("bookmarks")
                },
                totalBooks = pdfs.size,
                totalBookmarks = bookmarks.size,
                currentViewMode = viewMode,
                onViewModeChanged = { newMode ->
                    mainViewModel.setViewMode(newMode)
                }
            )
        }
    }
}
