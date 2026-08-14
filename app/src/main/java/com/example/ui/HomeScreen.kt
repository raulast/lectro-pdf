package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.PdfDocumentEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPdfSelected: (Int) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val pdfs by viewModel.pdfs.collectAsState()
    val context = LocalContext.current
    var editingPdf by remember { mutableStateOf<PdfDocumentEntity?>(null) }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val title = it.lastPathSegment ?: "Documento"
            viewModel.addPdf(it, title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lector PDF IA") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.testTag("add_pdf_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir PDF")
            }
        }
    ) { padding ->
        if (pdfs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Añade tu primer PDF pulsando el botón '+'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pdfs) { pdf ->
                    PdfItemCard(
                        pdf = pdf,
                        onClick = { onPdfSelected(pdf.id) },
                        onReprocess = { viewModel.reprocessPdf(pdf) },
                        onEditManual = { editingPdf = pdf },
                        onDelete = { viewModel.deletePdf(pdf.id) }
                    )
                }
            }
        }
        
        editingPdf?.let { pdf ->
            EditCoverDialog(
                pdf = pdf,
                onDismiss = { editingPdf = null },
                onSaveGeneric = { customTitle, color ->
                    viewModel.setGenericCover(pdf, customTitle, color)
                },
                onSaveFromPage = { page ->
                    viewModel.setCoverFromPage(pdf, page)
                }
            )
        }
    }
}

@Composable
fun PdfItemCard(
    pdf: PdfDocumentEntity, 
    onClick: () -> Unit,
    onReprocess: () -> Unit,
    onEditManual: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(onClick = onClick)
            .testTag("pdf_item_${pdf.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (pdf.coverImagePath != null && File(pdf.coverImagePath).exists()) {
                    AsyncImage(
                        model = File(pdf.coverImagePath),
                        contentDescription = "Portada de ${pdf.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = if (pdf.coverColor != null) Color(pdf.coverColor) else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                        Text(
                            text = pdf.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (pdf.coverColor != null) Color(0xFF1D1B20) else MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Menú de opciones
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Re-escanear portada") },
                            onClick = {
                                menuExpanded = false
                                onReprocess()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar portada manual") },
                            onClick = {
                                menuExpanded = false
                                onEditManual()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar PDF") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = pdf.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Pág ${pdf.lastReadPage + 1} / ${pdf.totalPages}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EditCoverDialog(
    pdf: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onSaveGeneric: (String, Int) -> Unit,
    onSaveFromPage: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customTitle by remember { mutableStateOf(pdf.title) }
    var pageInput by remember { mutableStateOf("1") }
    
    val colors = listOf(
        Color(0xFFE8DEF8), // default secondary container
        Color(0xFFFFD8E4), // pink
        Color(0xFFC3E8FF), // blue
        Color(0xFFC2F0C2), // green
        Color(0xFFFFE082), // yellow
        Color(0xFFE0E0E0)  // gray
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Portada") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Genérica") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Página PDF") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Título de la Portada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selecciona un color:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 0.dp,
                                        color = if (selectedColor == color) Color.Black else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    Text("Escribe el número de página:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Página (1 - ${pdf.totalPages})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0) {
                        onSaveGeneric(customTitle, selectedColor.toArgb())
                    } else {
                        val page = pageInput.toIntOrNull() ?: 1
                        onSaveFromPage(page)
                    }
                    onDismiss()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
