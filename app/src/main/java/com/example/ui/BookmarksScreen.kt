package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookmarkEntity
import com.example.utils.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onNavigateBack: () -> Unit,
    onNavigateToPage: (pdfId: Int, pageIndex: Int) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onUpdateBookmark: (BookmarkEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBookFilter by remember { mutableStateOf<String?>(null) }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var deletingBookmarkId by remember { mutableStateOf<Int?>(null) }

    val distinctBooks = remember(bookmarks) {
        bookmarks.map { it.bookTitle }.distinct().sorted()
    }

    val filteredBookmarks = remember(bookmarks, searchQuery, selectedBookFilter) {
        bookmarks.filter { bm ->
            val matchesBook = selectedBookFilter == null || bm.bookTitle == selectedBookFilter
            val matchesSearch = searchQuery.isBlank() ||
                    bm.title.contains(searchQuery, ignoreCase = true) ||
                    bm.bookTitle.contains(searchQuery, ignoreCase = true) ||
                    (bm.highlightedText?.contains(searchQuery, ignoreCase = true) == true) ||
                    (bm.note?.contains(searchQuery, ignoreCase = true) == true)
            matchesBook && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("manage_bookmarks")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.get("back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(Strings.get("search_bookmarks")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Book Filter Chips if multiple books have bookmarks
            if (distinctBooks.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedBookFilter == null) 0 else distinctBooks.indexOf(selectedBookFilter) + 1,
                    edgePadding = 16.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Tab(
                        selected = selectedBookFilter == null,
                        onClick = { selectedBookFilter = null },
                        text = { Text(Strings.get("filter_all_books")) }
                    )
                    distinctBooks.forEach { bookTitle ->
                        Tab(
                            selected = selectedBookFilter == bookTitle,
                            onClick = { selectedBookFilter = bookTitle },
                            text = { Text(bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            // Bookmarks List
            if (filteredBookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) Strings.get("no_results") else Strings.get("no_bookmarks_all"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookmarks, key = { it.id }) { bookmark ->
                        BookmarkItemCard(
                            bookmark = bookmark,
                            onClick = { onNavigateToPage(bookmark.pdfId, bookmark.pageIndex) },
                            onEdit = { editingBookmark = bookmark },
                            onDelete = { deletingBookmarkId = bookmark.id }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deletingBookmarkId?.let { bmId ->
        AlertDialog(
            onDismissRequest = { deletingBookmarkId = null },
            title = { Text(Strings.get("delete")) },
            text = { Text(Strings.get("delete_bookmark_confirm")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBookmark(bmId)
                        deletingBookmarkId = null
                    }
                ) {
                    Text(Strings.get("delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBookmarkId = null }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }

    // Edit Bookmark Dialog
    editingBookmark?.let { bm ->
        EditBookmarkDialog(
            bookmark = bm,
            onDismiss = { editingBookmark = null },
            onSave = { updated ->
                onUpdateBookmark(updated)
                editingBookmark = null
            }
        )
    }
}

@Composable
fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(bookmark.createdAt) { dateFormat.format(Date(bookmark.createdAt)) }
    val tagColor = Color(bookmark.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Book title & Page tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bookmark.bookTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${Strings.get("page_indicator", bookmark.pageNumber, 0).split("/")[0].trim()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bookmark Title
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Highlighted Text Quote
            if (!bookmark.highlightedText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(IntrinsicSize.Min)
                            .background(tagColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\"${bookmark.highlightedText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Written Note
            if (!bookmark.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bookmark.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom bar: date & action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = Strings.get("edit"),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = Strings.get("delete"),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = Strings.get("go_to_page"),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditBookmarkDialog(
    bookmark: BookmarkEntity,
    onDismiss: () -> Unit,
    onSave: (BookmarkEntity) -> Unit
) {
    var title by remember { mutableStateOf(bookmark.title) }
    var highlightedText by remember { mutableStateOf(bookmark.highlightedText ?: "") }
    var note by remember { mutableStateOf(bookmark.note ?: "") }
    
    val colors = listOf(
        Color(0xFFFFD54F), // Yellow/Amber
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFFF8A65), // Coral/Orange
        Color(0xFFBA68C8)  // Purple
    )
    var selectedColor by remember { mutableStateOf(Color(bookmark.color)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("edit")) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Strings.get("bookmark_title")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = highlightedText,
                    onValueChange = { highlightedText = it },
                    label = { Text(Strings.get("highlighted_text")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Strings.get("written_note")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(Strings.get("tag_color"), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
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
                        bookmark.copy(
                            title = title.ifBlank { "Pág ${bookmark.pageNumber}" },
                            highlightedText = highlightedText.ifBlank { null },
                            note = note.ifBlank { null },
                            color = selectedColor.hashCode()
                        )
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
