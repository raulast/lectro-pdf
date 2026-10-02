package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pdfId: Int,
    val bookTitle: String,
    val pageIndex: Int, // 0-based page index
    val pageNumber: Int, // 1-based page number for display
    val title: String,
    val highlightedText: String? = null,
    val note: String? = null,
    val color: Int = 0xFFFFD54F.toInt(), // Warm yellow default
    val createdAt: Long = System.currentTimeMillis()
)
