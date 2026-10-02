package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BookmarkEntity
import com.example.service.AudioReaderService
import com.example.utils.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    pdfId: Int,
    initialPage: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentPdf by viewModel.currentPdf.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    val pageText by viewModel.pageText.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val virtualPages by viewModel.virtualPages.collectAsState()
    val isSplitDoublePages by viewModel.isSplitDoublePages.collectAsState()
    val hasDoublePages by viewModel.hasDoublePages.collectAsState()
    val bookBookmarks by viewModel.bookBookmarks.collectAsState()

    var isTextMode by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var scale by remember { mutableStateOf(1f) }

    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showBookBookmarksDialog by remember { mutableStateOf(false) }

    val isPlaying by AudioReaderService.isServiceRunning.collectAsState()

    val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("voice_speed", 1.0f)) }
    var voicePitch by remember { mutableFloatStateOf(prefs.getFloat("voice_pitch", 1.0f)) }
    var voiceEngine by remember { mutableStateOf(prefs.getString("voice_engine", "") ?: "") }
    var voiceName by remember { mutableStateOf(prefs.getString("voice_name", "") ?: "") }

    var ttsEngines by remember { mutableStateOf<List<android.speech.tts.TextToSpeech.EngineInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val tts = android.speech.tts.TextToSpeech(context) {}
            ttsEngines = tts.engines ?: emptyList()
            tts.shutdown()
        } catch (e: Exception) {}
    }
    var showVoiceSettings by remember { mutableStateOf(false) }

    LaunchedEffect(pdfId, initialPage) {
        viewModel.loadPdf(pdfId, initialPage)
    }

    val backgroundColor = if (isNightMode) Color(0xFF121212) else Color.White
    val contentColor = if (isNightMode) Color.LightGray else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentPdf?.title ?: Strings.get("reading"),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasDoublePages) {
                            Text(
                                text = if (isSplitDoublePages) Strings.get("split_double_pages") else Strings.get("full_sheet"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.get("back"))
                    }
                },
                actions = {
                    // Add Bookmark Button
                    IconButton(
                        onClick = { showAddBookmarkDialog = true },
                        modifier = Modifier.testTag("add_bookmark_button")
                    ) {
                        Icon(
                            Icons.Filled.BookmarkAdd,
                            contentDescription = Strings.get("add_bookmark"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // View Bookmarks of this book
                    IconButton(
                        onClick = { showBookBookmarksDialog = true },
                        modifier = Modifier.testTag("view_book_bookmarks_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (bookBookmarks.isNotEmpty()) {
                                    Badge { Text("${bookBookmarks.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = Strings.get("book_bookmarks"))
                        }
                    }

                    if (hasDoublePages) {
                        IconButton(onClick = {
                            viewModel.toggleSplitDoublePages()
                            val msg = if (!isSplitDoublePages) Strings.get("split_double_pages") else Strings.get("full_sheet")
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                if (isSplitDoublePages) Icons.Filled.AutoStories else Icons.Filled.MenuBook,
                                contentDescription = if (isSplitDoublePages) Strings.get("split_double_pages") else Strings.get("full_sheet"),
                                tint = if (isSplitDoublePages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { isNightMode = !isNightMode }) {
                        Icon(
                            if (isNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = Strings.get("night_mode")
                        )
                    }
                    IconButton(onClick = { isTextMode = !isTextMode }) {
                        Icon(
                            if (isTextMode) Icons.Filled.Image else Icons.Filled.TextFields,
                            contentDescription = Strings.get("switch_mode")
                        )
                    }
                    if (isTextMode) {
                        IconButton(onClick = { fontSize = (fontSize.value + 2).sp }) {
                            Icon(Icons.Filled.FormatSize, contentDescription = Strings.get("font_increase"))
                        }
                        IconButton(onClick = { fontSize = (fontSize.value - 2).coerceAtLeast(10f).sp }) {
                            Icon(Icons.Filled.TextDecrease, contentDescription = Strings.get("font_decrease"))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { viewModel.previousPage() },
                        modifier = Modifier.testTag("prev_page_button")
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = Strings.get("prev_page"))
                    }

                    // Requirement 1: Clickable Page Indicator to Jump to Specific Page!
                    val currentVP = virtualPages.getOrNull(currentPage)
                    val totalVirtual = virtualPages.size.coerceAtLeast(currentPdf?.totalPages ?: 1)
                    val pageLabel = if (virtualPages.isNotEmpty()) {
                        val base = "${currentPage + 1} / $totalVirtual"
                        if (currentVP != null && isSplitDoublePages && (currentVP.split == com.example.domain.PageSplit.LEFT || currentVP.split == com.example.domain.PageSplit.RIGHT)) {
                            val side = if (currentVP.split == com.example.domain.PageSplit.LEFT) Strings.get("left_page_indicator") else Strings.get("right_page_indicator")
                            "$base ($side)"
                        } else {
                            base
                        }
                    } else {
                        "${(currentPdf?.lastReadPage ?: 0) + 1} / ${(currentPdf?.totalPages ?: 1)}"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clickable { showJumpToPageDialog = true }
                            .testTag("page_indicator_clickable")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pageLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.nextPage() },
                        modifier = Modifier.testTag("next_page_button")
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = Strings.get("next_page"))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
                        if (!isPlaying && pageText.isBlank()) {
                            Toast.makeText(context, "No se pudo extraer texto de esta página o aún está cargando.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        viewModel.toggleAutoRead(voiceSpeed, voicePitch, voiceEngine, voiceName)
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) Strings.get("stop_reading") else Strings.get("read_aloud")
                        )
                    }

                    IconButton(onClick = { showVoiceSettings = true }) {
                        Icon(Icons.Filled.SettingsVoice, contentDescription = Strings.get("voice_settings"))
                    }

                    IconButton(onClick = { viewModel.generateSummaryAndSentiment() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = Strings.get("ai_analysis"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            if (isGeneratingSummary) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            currentPdf?.let { pdf ->
                if (pdf.summary != null || pdf.sentiment != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Strings.get("ai_analysis"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                IconButton(onClick = { viewModel.clearSummary() }) {
                                    Icon(Icons.Filled.Close, contentDescription = Strings.get("close"))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (pdf.sentiment != null) {
                                Text("Sentimiento: ${pdf.sentiment}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (pdf.summary != null) {
                                Text("Resumen: ${pdf.summary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (!isTextMode) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                if (isTextMode) {
                    val scrollState = rememberScrollState()
                    val chunks = remember(pageText) { com.example.utils.TextChunker.parse(pageText) }
                    val currentChunkIndex by viewModel.currentChunkIndex.collectAsState(initial = -1)
                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    val annotatedString = buildAnnotatedString {
                        if (chunks.isEmpty()) {
                            append(pageText.ifEmpty { Strings.get("extracting_text") })
                        } else {
                            chunks.forEachIndexed { index, chunk ->
                                val isRead = isPlaying && index < currentChunkIndex
                                val isReading = isPlaying && index == currentChunkIndex
                                val isNext = isPlaying && index == currentChunkIndex + 1

                                val textColor = if (isRead) Color.Gray else contentColor
                                val bgColor = when {
                                    isReading -> Color(0xFFC8E6C9)
                                    isNext -> Color(0xFFFFF9C4)
                                    else -> Color.Transparent
                                }

                                withStyle(style = SpanStyle(color = textColor, background = bgColor)) {
                                    append(chunk.text)
                                }
                            }
                        }
                    }

                    Text(
                        text = annotatedString,
                        color = contentColor,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.5f).sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                            .pointerInput(chunks, isPlaying, currentChunkIndex) {
                                detectTapGestures { pos ->
                                    textLayoutResult?.let { layoutResult ->
                                        val offset = layoutResult.getOffsetForPosition(pos)
                                        val clickedChunkIndex = chunks.indexOfFirst { offset >= it.start && offset < it.end }
                                        if (clickedChunkIndex != -1) {
                                            viewModel.seekToChunk(clickedChunkIndex)
                                        }
                                    }
                                }
                            },
                        onTextLayout = { textLayoutResult = it }
                    )
                } else {
                    pageBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Página PDF",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                        )
                    }
                }
            }
        }

        // Dialog: Requirement 1 - Jump to Page
        if (showJumpToPageDialog) {
            val total = virtualPages.size.coerceAtLeast(currentPdf?.totalPages ?: 1)
            JumpToPageDialog(
                currentPage = currentPage + 1,
                totalPages = total,
                onDismiss = { showJumpToPageDialog = false },
                onJump = { targetPage ->
                    viewModel.jumpToPage(targetPage - 1)
                    showJumpToPageDialog = false
                }
            )
        }

        // Dialog: Requirement 2 - Add Bookmark with Highlights and Notes
        if (showAddBookmarkDialog) {
            AddBookmarkDialog(
                pageNumber = currentPage + 1,
                suggestedText = if (pageText.isNotBlank()) pageText.take(200).replace("\n", " ").trim() else "",
                onDismiss = { showAddBookmarkDialog = false },
                onSave = { title, quote, note, color ->
                    viewModel.addBookmark(title, quote, note, color)
                    showAddBookmarkDialog = false
                    Toast.makeText(context, Strings.get("bookmark_created"), Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialog: Requirement 2 - View Bookmarks of this Book
        if (showBookBookmarksDialog) {
            BookBookmarksDialog(
                bookmarks = bookBookmarks,
                onDismiss = { showBookBookmarksDialog = false },
                onSelectBookmark = { bm ->
                    viewModel.jumpToPage(bm.pageIndex)
                    showBookBookmarksDialog = false
                },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) }
            )
        }

        // Dialog: Voice Settings
        if (showVoiceSettings) {
            VoiceSettingsDialog(
                speed = voiceSpeed,
                pitch = voicePitch,
                engine = voiceEngine,
                voice = voiceName,
                enginesList = ttsEngines,
                onDismiss = { showVoiceSettings = false },
                onValuesChange = { newSpeed, newPitch, newEngine, newVoice ->
                    voiceSpeed = newSpeed
                    voicePitch = newPitch
                    voiceEngine = newEngine
                    voiceName = newVoice
                    prefs.edit()
                        .putFloat("voice_speed", newSpeed)
                        .putFloat("voice_pitch", newPitch)
                        .putString("voice_engine", newEngine)
                        .putString("voice_name", newVoice)
                        .apply()

                    if (AudioReaderService.isServiceRunning.value) {
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice)
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice)
                    }
                }
            )
        }
    }
}

/**
 * Requirement 1: Jump to Page Dialog
 */
@Composable
fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("jump_to_page"))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Strings.get("jump_prompt", totalPages),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        pageInput = digits
                        isError = false
                    },
                    label = { Text(Strings.get("page_number")) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(Strings.get("invalid_page"), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_page_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick jump buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { pageInput = "1"; isError = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("first_page"), style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { pageInput = "$totalPages"; isError = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("last_page", totalPages), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageInput.toIntOrNull()
                    if (p != null && p in 1..totalPages) {
                        onJump(p)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("jump_confirm_button")
            ) {
                Text(Strings.get("jump"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel"))
            }
        }
    )
}

/**
 * Requirement 2: Add Bookmark Dialog with Quote Text and Written Notes
 */
@Composable
fun AddBookmarkDialog(
    pageNumber: Int,
    suggestedText: String,
    onDismiss: () -> Unit,
    onSave: (title: String, quote: String?, note: String?, color: Int) -> Unit
) {
    var title by remember { mutableStateOf("Página $pageNumber") }
    var highlightedText by remember { mutableStateOf(suggestedText) }
    var note by remember { mutableStateOf("") }

    val colors = listOf(
        Color(0xFFFFD54F), // Amber/Yellow
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFFF8A65), // Coral/Orange
        Color(0xFFBA68C8)  // Purple
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("add_bookmark"))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Strings.get("bookmark_title")) },
                    placeholder = { Text(Strings.get("bookmark_title_hint")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = highlightedText,
                    onValueChange = { highlightedText = it },
                    label = { Text(Strings.get("highlighted_text")) },
                    placeholder = { Text(Strings.get("highlighted_text_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Strings.get("written_note")) },
                    placeholder = { Text(Strings.get("written_note_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(Strings.get("tag_color"), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = col }
                                .border(
                                    width = if (selectedColor == col) 2.dp else 0.dp,
                                    color = if (selectedColor == col) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.ifBlank { "Página $pageNumber" },
                        highlightedText.ifBlank { null },
                        note.ifBlank { null },
                        selectedColor.hashCode()
                    )
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

/**
 * Requirement 2: Bookmarks list dialog for current book
 */
@Composable
fun BookBookmarksDialog(
    bookmarks: List<BookmarkEntity>,
    onDismiss: () -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("book_bookmarks"))
            }
        },
        text = {
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.get("no_bookmarks_book"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        val tagColor = Color(bm.color)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBookmark(bm) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Pág ${bm.pageNumber}:",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = bm.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!bm.highlightedText.isNullOrBlank()) {
                                        Text(
                                            text = "\"${bm.highlightedText}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!bm.note.isNullOrBlank()) {
                                        Text(
                                            text = "Nota: ${bm.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteBookmark(bm.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = Strings.get("delete"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
fun VoiceSettingsDialog(
    speed: Float,
    pitch: Float,
    engine: String,
    voice: String,
    enginesList: List<android.speech.tts.TextToSpeech.EngineInfo>,
    onDismiss: () -> Unit,
    onValuesChange: (Float, Float, String, String) -> Unit
) {
    var currentSpeed by remember { mutableFloatStateOf(speed) }
    var currentPitch by remember { mutableFloatStateOf(pitch) }
    var currentEngine by remember { mutableStateOf(engine) }
    var currentVoice by remember { mutableStateOf(voice) }

    val isGoogleEngine = remember(currentEngine, enginesList) {
        currentEngine.contains("google", ignoreCase = true) ||
                (currentEngine.isEmpty() && (enginesList.isEmpty() || enginesList.any { it.name.contains("google", ignoreCase = true) }))
    }

    var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(currentEngine, isGoogleEngine) {
        if (!isGoogleEngine) {
            availableVoices = emptyList()
            return@LaunchedEffect
        }
        var localTts: android.speech.tts.TextToSpeech? = null
        val latch = kotlinx.coroutines.CompletableDeferred<Boolean>()

        val initListener = android.speech.tts.TextToSpeech.OnInitListener { status ->
            latch.complete(status == android.speech.tts.TextToSpeech.SUCCESS)
        }

        localTts = if (currentEngine.isNotEmpty()) {
            android.speech.tts.TextToSpeech(context, initListener, currentEngine)
        } else {
            android.speech.tts.TextToSpeech(context, initListener)
        }

        try {
            val success = kotlinx.coroutines.withTimeoutOrNull(3000) { latch.await() } ?: false
            if (success) {
                for (i in 0..15) {
                    val voices = localTts?.voices?.toList()
                    if (!voices.isNullOrEmpty()) {
                        availableVoices = voices.sortedBy { it.name }
                        if (currentVoice.isNotEmpty() && availableVoices.none { it.name == currentVoice }) {
                            currentVoice = ""
                        }
                        break
                    }
                    kotlinx.coroutines.delay(200)
                }
            } else {
                availableVoices = emptyList()
            }
        } finally {
            try {
                localTts?.shutdown()
            } catch (e: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("voice_settings")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(Strings.get("tts_engine"), style = MaterialTheme.typography.titleSmall)
                enginesList.forEach { eng ->
                    val isSelected = (currentEngine == eng.name) || (currentEngine.isEmpty() && eng.name.contains("google", ignoreCase = true))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentEngine = eng.name
                                currentVoice = ""
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                currentEngine = eng.name
                                currentVoice = ""
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(eng.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (isGoogleEngine && availableVoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Idioma de la Voz:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    val languages = remember(availableVoices) {
                        availableVoices.map { it.locale.displayName }.distinct().sorted()
                    }

                    var selectedLanguage by remember {
                        val initialVoice = availableVoices.find { it.name == currentVoice }
                        mutableStateOf(initialVoice?.locale?.displayName ?: languages.firstOrNull { it.contains("Español", ignoreCase = true) || it.contains("Spanish", ignoreCase = true) } ?: languages.firstOrNull() ?: "")
                    }

                    val filteredVoices = remember(selectedLanguage, availableVoices) {
                        if (selectedLanguage.isEmpty()) availableVoices
                        else availableVoices.filter { it.locale.displayName == selectedLanguage }
                    }

                    var langExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { langExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedLanguage.ifEmpty { "Seleccionar Idioma" })
                        }
                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.fillMaxHeight(0.5f)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Voz Específica:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentVoice.ifEmpty { "Por Defecto" })
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxHeight(0.5f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Por Defecto") },
                                onClick = {
                                    currentVoice = ""
                                    expanded = false
                                }
                            )
                            filteredVoices.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(v.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(v.locale.displayName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        currentVoice = v.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${Strings.get("tts_speed")}: ${String.format(Locale.US, "%.1f", currentSpeed)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentSpeed,
                    onValueChange = { currentSpeed = it },
                    valueRange = 0.5f..2.5f,
                    steps = 19
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${Strings.get("tts_pitch")}: ${String.format(Locale.US, "%.1f", currentPitch)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { currentPitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValuesChange(currentSpeed, currentPitch, currentEngine, currentVoice)
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
