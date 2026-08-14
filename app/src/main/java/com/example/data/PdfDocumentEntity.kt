package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdfs")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uriString: String,
    val title: String,
    val lastReadPage: Int = 0,
    val totalPages: Int = 0,
    val coverImagePath: String? = null,
    val summary: String? = null,
    val sentiment: String? = null,
    val coverColor: Int? = null
)
