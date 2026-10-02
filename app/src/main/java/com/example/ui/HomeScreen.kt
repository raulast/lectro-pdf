package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.PdfDocumentEntity
import com.example.utils.LanguageManager
import com.example.utils.Strings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPdfSelected: (Int) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val pdfs by viewModel.pdfs.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val currentLangCode by LanguageManager.currentLanguage.collectAsState()
    val context = LocalContext.current

    var editingPdf by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var deletingPdf by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }

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
                title = {
                    Text(
                        text = Strings.get("app_name"),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Reorder books dialog button
                    if (pdfs.size > 1) {
                        IconButton(
                            onClick = { showReorderDialog = true },
                            modifier = Modifier.testTag("reorder_books_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = Strings.get("order_books")
                            )
                        }
                    }

                    // View Mode toggle button (Cards / List)
                    IconButton(
                        onClick = {
                            val nextMode = if (viewMode == "grid") "list" else "grid"
                            viewModel.setViewMode(nextMode)
                        },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            if (viewMode == "grid") Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = if (viewMode == "grid") Strings.get("view_list") else Strings.get("view_cards")
                        )
                    }

                    // Bookmarks management button
                    IconButton(
                        onClick = onNavigateToBookmarks,
                        modifier = Modifier.testTag("bookmarks_manager_button")
                    ) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = Strings.get("manage_bookmarks")
                        )
                    }

                    // Settings screen button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = Strings.get("settings")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.testTag("add_pdf_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = Strings.get("add_pdf"))
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings.get("empty_library"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            if (viewMode == "grid") {
                // Tarjetas (Grid View)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(pdfs, key = { _, pdf -> pdf.id }) { index, pdf ->
                        PdfItemCard(
                            pdf = pdf,
                            isFirst = index == 0,
                            isLast = index == pdfs.size - 1,
                            onClick = { onPdfSelected(pdf.id) },
                            onReprocess = { viewModel.reprocessPdf(pdf) },
                            onEditManual = { editingPdf = pdf },
                            onMoveUp = { viewModel.moveBookUp(pdf) },
                            onMoveDown = { viewModel.moveBookDown(pdf) },
                            onDelete = { deletingPdf = pdf }
                        )
                    }
                }
            } else {
                // Lista (List View)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pdfs, key = { _, pdf -> pdf.id }) { index, pdf ->
                        PdfItemRow(
                            pdf = pdf,
                            isFirst = index == 0,
                            isLast = index == pdfs.size - 1,
                            onClick = { onPdfSelected(pdf.id) },
                            onReprocess = { viewModel.reprocessPdf(pdf) },
                            onEditManual = { editingPdf = pdf },
                            onMoveUp = { viewModel.moveBookUp(pdf) },
                            onMoveDown = { viewModel.moveBookDown(pdf) },
                            onDelete = { deletingPdf = pdf }
                        )
                    }
                }
            }
        }

        // Edit Manual Cover Dialog
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

        // Delete Confirmation Dialog
        deletingPdf?.let { pdf ->
            AlertDialog(
                onDismissRequest = { deletingPdf = null },
                title = { Text(Strings.get("delete_confirm_title")) },
                text = { Text("${Strings.get("delete_confirm_msg")}\n\n\"${pdf.title}\"") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePdf(pdf.id)
                            deletingPdf = null
                        }
                    ) {
                        Text(Strings.get("delete"), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingPdf = null }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Reorder Books Dialog (Flechas de organización)
        if (showReorderDialog) {
            ReorderBooksDialog(
                books = pdfs,
                onDismiss = { showReorderDialog = false },
                onMoveUp = { pdf -> viewModel.moveBookUp(pdf) },
                onMoveDown = { pdf -> viewModel.moveBookDown(pdf) }
            )
        }
    }
}

@Composable
fun PdfItemCard(
    pdf: PdfDocumentEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onReprocess: () -> Unit,
    onEditManual: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(245.dp)
            .clickable(onClick = onClick)
            .testTag("pdf_item_${pdf.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Actions: Order Arrow chips and Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Move Up/Down arrows badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = onMoveUp,
                                enabled = !isFirst,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = Strings.get("move_up"),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (!isFirst) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = onMoveDown,
                                enabled = !isLast,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = Strings.get("move_down"),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (!isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Options Menu Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("rescan_cover")) },
                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onReprocess()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("edit_cover")) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditManual()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("move_up")) },
                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                        enabled = !isFirst,
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("move_down")) },
                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                        enabled = !isLast,
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Strings.get("delete_pdf"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = pdf.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Strings.get("page_indicator", pdf.lastReadPage + 1, pdf.totalPages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PdfItemRow(
    pdf: PdfDocumentEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onReprocess: () -> Unit,
    onEditManual: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pdf_row_${pdf.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 72.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (pdf.coverImagePath != null && File(pdf.coverImagePath).exists()) {
                    AsyncImage(
                        model = File(pdf.coverImagePath),
                        contentDescription = "Portada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = if (pdf.coverColor != null) Color(pdf.coverColor) else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.get("page_indicator", pdf.lastReadPage + 1, pdf.totalPages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Progress Bar
                val progress = if (pdf.totalPages > 0) (pdf.lastReadPage + 1).toFloat() / pdf.totalPages else 0f
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Arrow Reorder Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = Strings.get("move_up"),
                        tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = Strings.get("move_down"),
                        tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }

            // Options menu button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("rescan_cover")) },
                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onReprocess()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("edit_cover")) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditManual()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Strings.get("delete_pdf"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderBooksDialog(
    books: List<PdfDocumentEntity>,
    onDismiss: () -> Unit,
    onMoveUp: (PdfDocumentEntity) -> Unit,
    onMoveDown: (PdfDocumentEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("order_books"))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(books, key = { _, it -> it.id }) { index, book ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onMoveUp(book) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    contentDescription = Strings.get("move_up"),
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown(book) },
                                enabled = index < books.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = Strings.get("move_down"),
                                    tint = if (index < books.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("close"))
            }
        }
    )
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
        title = { Text(Strings.get("edit_cover_title")) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(Strings.get("tab_generic")) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(Strings.get("tab_pdf_page")) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text(Strings.get("cover_title_label")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(Strings.get("select_color"), style = MaterialTheme.typography.bodyMedium)
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
                    Text(Strings.get("enter_page_number"))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        label = { Text(Strings.get("page_range", pdf.totalPages)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                Text(Strings.get("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel"))
            }
        }
    )
}
