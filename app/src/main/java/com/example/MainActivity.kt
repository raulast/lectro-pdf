package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.HomeScreen
import com.example.ui.ReaderScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
fun PdfReaderApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPdfSelected = { id ->
                    navController.navigate("reader/$id")
                }
            )
        }
        composable("reader/{pdfId}") { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getString("pdfId")?.toIntOrNull()
            if (pdfId != null) {
                ReaderScreen(
                    pdfId = pdfId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
