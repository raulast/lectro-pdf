package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.PdfDocumentEntity
import com.example.data.PdfRepository
import com.example.domain.PdfRendererWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PdfRepository
    val pdfs: StateFlow<List<PdfDocumentEntity>>
    val allBookmarks: StateFlow<List<BookmarkEntity>>

    private val _viewMode = MutableStateFlow("grid")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PdfRepository(db.pdfDao(), db.bookmarkDao())
        
        val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _viewMode.value = prefs.getString("view_mode", "grid") ?: "grid"

        pdfs = repository.allPdfs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBookmarks = repository.allBookmarks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("view_mode", mode).apply()
    }

    fun moveBookUp(pdf: PdfDocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = pdfs.value
            val index = currentList.indexOfFirst { it.id == pdf.id }
            if (index > 0) {
                val prevPdf = currentList[index - 1]
                // Normalize and swap display order
                val newPrevOrder = index
                val newCurrentOrder = index - 1
                repository.updatePdfOrder(pdf.id, newCurrentOrder)
                repository.updatePdfOrder(prevPdf.id, newPrevOrder)
            }
        }
    }

    fun moveBookDown(pdf: PdfDocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = pdfs.value
            val index = currentList.indexOfFirst { it.id == pdf.id }
            if (index != -1 && index < currentList.size - 1) {
                val nextPdf = currentList[index + 1]
                val newNextOrder = index
                val newCurrentOrder = index + 1
                repository.updatePdfOrder(pdf.id, newCurrentOrder)
                repository.updatePdfOrder(nextPdf.id, newNextOrder)
            }
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmarkById(id)
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookmark(bookmark)
        }
    }

    fun addPdf(uri: Uri, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            // Take persistable permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val renderer = PdfRendererWrapper(context, uri)
            val hasWide = renderer.hasAnyWidePages()
            val virtualPages = renderer.buildVirtualPages(splitDouble = hasWide)
            val totalPages = virtualPages.size
            val physicalPages = renderer.pageCount
            
            var finalCoverPath: String? = null
            var detectedTitle = title
            
            // Analizar las primeras 10 páginas físicas buscando una portada real
            val pagesToCheck = minOf(10, physicalPages)
            for (i in 0 until pagesToCheck) {
                val bitmap = renderer.renderPage(i, 800)
                if (bitmap != null) {
                    val text = com.example.domain.PdfTextExtractor.extractTextFromBitmap(bitmap)
                    
                    // Si el título actual es genérico o un archivo, intentamos extraer un título de la primera página
                    if (i == 0 && (detectedTitle.contains(".pdf", ignoreCase = true) || detectedTitle.startsWith("document", ignoreCase = true) || detectedTitle.startsWith("msf", ignoreCase = true))) {
                        val firstLines = text.split("\n\n").firstOrNull { it.isNotBlank() }
                        if (firstLines != null) {
                            detectedTitle = firstLines.take(80).replace('\n', ' ')
                        } else {
                            detectedTitle = detectedTitle.replace(".pdf", "", ignoreCase = true)
                        }
                    }

                    // Una portada real suele tener poco texto (ej. < 600 caracteres) y muchas imágenes o espacios
                    if (text.length < 600) {
                        finalCoverPath = renderer.saveCoverImage(i)
                        bitmap.recycle()
                        break // Encontramos una portada adecuada
                    }
                    bitmap.recycle()
                }
            }

            renderer.close()

            val pdf = PdfDocumentEntity(
                uriString = uri.toString(),
                title = detectedTitle,
                totalPages = totalPages,
                coverImagePath = finalCoverPath
            )
            repository.insertPdf(pdf)
        }
    }

    fun reprocessPdf(pdf: PdfDocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val uri = Uri.parse(pdf.uriString)
            
            try {
                val renderer = PdfRendererWrapper(context, uri)
                val hasWide = renderer.hasAnyWidePages()
                val virtualPages = renderer.buildVirtualPages(splitDouble = hasWide)
                val totalPages = virtualPages.size
                val physicalPages = renderer.pageCount
                
                var finalCoverPath: String? = null
                var detectedTitle = pdf.title
                
                val pagesToCheck = minOf(10, physicalPages)
                for (i in 0 until pagesToCheck) {
                    val bitmap = renderer.renderPage(i, 800)
                    if (bitmap != null) {
                        val text = com.example.domain.PdfTextExtractor.extractTextFromBitmap(bitmap)
                        
                        // Si el título actual es genérico, intentar re-extraer
                        if (i == 0 && (detectedTitle.contains(".pdf", ignoreCase = true) || detectedTitle.startsWith("document", ignoreCase = true) || detectedTitle.startsWith("msf", ignoreCase = true))) {
                            val firstLines = text.split("\n\n").firstOrNull { it.isNotBlank() }
                            if (firstLines != null) {
                                detectedTitle = firstLines.take(80).replace('\n', ' ')
                            } else {
                                detectedTitle = detectedTitle.replace(".pdf", "", ignoreCase = true)
                            }
                        }

                        if (text.length < 600) {
                            finalCoverPath = renderer.saveCoverImage(i)
                            bitmap.recycle()
                            break // Encontramos una portada
                        }
                        bitmap.recycle()
                    }
                }
                renderer.close()

                // Si encontramos una nueva portada, eliminamos la anterior
                if (pdf.coverImagePath != null && pdf.coverImagePath != finalCoverPath) {
                    File(pdf.coverImagePath).delete()
                }

                val updatedPdf = pdf.copy(
                    title = detectedTitle,
                    totalPages = totalPages,
                    coverImagePath = finalCoverPath
                )
                repository.updatePdf(updatedPdf)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setGenericCover(pdf: PdfDocumentEntity, customTitle: String, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (pdf.coverImagePath != null) {
                File(pdf.coverImagePath).delete()
            }
            val updatedPdf = pdf.copy(
                title = customTitle,
                coverImagePath = null,
                coverColor = color
            )
            repository.updatePdf(updatedPdf)
        }
    }

    fun setCoverFromPage(pdf: PdfDocumentEntity, pageNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val uri = Uri.parse(pdf.uriString)
            try {
                val renderer = PdfRendererWrapper(context, uri)
                val pageIndex = (pageNumber - 1).coerceIn(0, renderer.pageCount - 1)
                
                val finalCoverPath = renderer.saveCoverImage(pageIndex)
                renderer.close()

                if (pdf.coverImagePath != null && pdf.coverImagePath != finalCoverPath) {
                    File(pdf.coverImagePath).delete()
                }

                if (finalCoverPath != null) {
                    val updatedPdf = pdf.copy(coverImagePath = finalCoverPath)
                    repository.updatePdf(updatedPdf)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePdf(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePdfById(id)
        }
    }
}
