package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.PdfDocumentEntity
import com.example.data.PdfRepository
import com.example.domain.PageSplit
import com.example.domain.PdfRendererWrapper
import com.example.domain.PdfTextExtractor
import com.example.domain.VirtualPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PdfRepository
    
    private val _currentPdf = MutableStateFlow<PdfDocumentEntity?>(null)
    val currentPdf: StateFlow<PdfDocumentEntity?> = _currentPdf.asStateFlow()

    private val _virtualPages = MutableStateFlow<List<VirtualPage>>(emptyList())
    val virtualPages: StateFlow<List<VirtualPage>> = _virtualPages.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    private val _pageText = MutableStateFlow("")
    val pageText: StateFlow<String> = _pageText.asStateFlow()
    
    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _isSplitDoublePages = MutableStateFlow(true)
    val isSplitDoublePages: StateFlow<Boolean> = _isSplitDoublePages.asStateFlow()

    private val _hasDoublePages = MutableStateFlow(false)
    val hasDoublePages: StateFlow<Boolean> = _hasDoublePages.asStateFlow()

    private var renderer: PdfRendererWrapper? = null

    private var isAutoReading = false
    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f
    private var currentEngine = ""
    private val _bookBookmarks = MutableStateFlow<List<com.example.data.BookmarkEntity>>(emptyList())
    val bookBookmarks: StateFlow<List<com.example.data.BookmarkEntity>> = _bookBookmarks.asStateFlow()

    private var currentVoiceName = ""
    private val preloadedTexts = mutableMapOf<Int, String>()

    val currentChunkIndex = com.example.service.AudioReaderService.currentChunkIndex
    var lastKnownChunkIndex = 0

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PdfRepository(db.pdfDao(), db.bookmarkDao())
        
        val prefs = application.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
        currentSpeed = prefs.getFloat("voice_speed", 1.0f)
        currentPitch = prefs.getFloat("voice_pitch", 1.0f)
        currentEngine = prefs.getString("voice_engine", "") ?: ""
        currentVoiceName = prefs.getString("voice_name", "") ?: ""
        
        viewModelScope.launch(Dispatchers.Main) {
            com.example.service.AudioReaderService.pageFinishedEvent.collect {
                if (isAutoReading) {
                    advanceAndReadNextPage()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            currentChunkIndex.collect { idx ->
                if (idx >= 0) lastKnownChunkIndex = idx
            }
        }
    }

    fun loadPdf(id: Int, targetPage: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdf = repository.getPdfById(id)
            if (pdf != null) {
                _currentPdf.value = pdf

                // Observe bookmarks for this book
                launch {
                    repository.getBookmarksForPdf(id).collect { bms ->
                        _bookBookmarks.value = bms
                    }
                }

                val r = PdfRendererWrapper(getApplication(), Uri.parse(pdf.uriString))
                renderer = r
                
                val hasWide = r.hasAnyWidePages()
                _hasDoublePages.value = hasWide

                val prefs = getApplication<Application>().getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
                // Por defecto, si el PDF tiene hojas dobles (2 en 1), se activa la división automáticamente
                val splitPref = prefs.getBoolean("split_double_pages_${pdf.id}", hasWide)
                _isSplitDoublePages.value = splitPref

                val pages = r.buildVirtualPages(splitPref)
                _virtualPages.value = pages

                val pageToOpen = (targetPage ?: pdf.lastReadPage).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                _currentPage.value = pageToOpen
                
                // Si el total de páginas virtuales difiere de la BD, actualizarlo
                if (pages.isNotEmpty() && pdf.totalPages != pages.size) {
                    val updated = pdf.copy(totalPages = pages.size, lastReadPage = pageToOpen)
                    repository.updatePdf(updated)
                    _currentPdf.value = updated
                }

                renderCurrentPage()
            }
        }
    }

    fun jumpToPage(pageIndex: Int) {
        stopAutoRead()
        lastKnownChunkIndex = 0
        val total = _virtualPages.value.size
        if (pageIndex in 0 until total) {
            _currentPage.value = pageIndex
            renderCurrentPage()
            saveProgress()
        }
    }

    fun addBookmark(title: String, highlightedText: String?, note: String?, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdf = _currentPdf.value ?: return@launch
            val pageIdx = _currentPage.value
            val bookmark = com.example.data.BookmarkEntity(
                pdfId = pdf.id,
                bookTitle = pdf.title,
                pageIndex = pageIdx,
                pageNumber = pageIdx + 1,
                title = title.ifBlank { "Pág ${pageIdx + 1}" },
                highlightedText = highlightedText,
                note = note,
                color = color
            )
            repository.insertBookmark(bookmark)
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmarkById(id)
        }
    }

    fun updateBookmark(bookmark: com.example.data.BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBookmark(bookmark)
        }
    }

    fun toggleSplitDoublePages() {
        viewModelScope.launch(Dispatchers.IO) {
            val pdf = _currentPdf.value ?: return@launch
            val r = renderer ?: return@launch
            val currentVIdx = _currentPage.value
            val currentVP = _virtualPages.value.getOrNull(currentVIdx)
            val currentPdfIdx = currentVP?.pdfIndex ?: 0

            val newSplit = !_isSplitDoublePages.value
            _isSplitDoublePages.value = newSplit

            val prefs = getApplication<Application>().getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("split_double_pages_${pdf.id}", newSplit).apply()

            val newPages = r.buildVirtualPages(newSplit)
            _virtualPages.value = newPages

            // Mapear al nuevo índice de la página correspondiente a la misma hoja
            val newIdx = newPages.indexOfFirst { it.pdfIndex == currentPdfIdx }.coerceAtLeast(0)
            _currentPage.value = newIdx

            val updated = pdf.copy(totalPages = newPages.size, lastReadPage = newIdx)
            repository.updatePdf(updated)
            _currentPdf.value = updated

            preloadedTexts.clear()
            renderCurrentPage()

            if (isAutoReading) {
                readPageAndPreloadNext(newIdx, 0)
            }
        }
    }

    fun toggleAutoRead(speed: Float, pitch: Float, engine: String, voiceName: String) {
        val context = getApplication<Application>()
        if (com.example.service.AudioReaderService.isServiceRunning.value) {
            stopAutoRead()
        } else {
            isAutoReading = true
            currentSpeed = speed
            currentPitch = pitch
            currentEngine = engine
            currentVoiceName = voiceName
            readPageAndPreloadNext(_currentPage.value, lastKnownChunkIndex)
        }
    }
    
    fun seekToChunk(chunkIndex: Int) {
        lastKnownChunkIndex = chunkIndex
        val prefs = getApplication<Application>().getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
        currentSpeed = prefs.getFloat("voice_speed", currentSpeed)
        currentPitch = prefs.getFloat("voice_pitch", currentPitch)
        currentEngine = prefs.getString("voice_engine", currentEngine) ?: currentEngine
        currentVoiceName = prefs.getString("voice_name", currentVoiceName) ?: currentVoiceName

        if (!isAutoReading && !com.example.service.AudioReaderService.isServiceRunning.value) {
            isAutoReading = true
        }
        readPageAndPreloadNext(_currentPage.value, chunkIndex)
    }

    private fun stopAutoRead() {
        isAutoReading = false
        val context = getApplication<Application>()
        val intent = Intent(context, com.example.service.AudioReaderService::class.java).apply {
            action = com.example.service.AudioReaderService.ACTION_STOP
        }
        context.startService(intent)
    }

    private suspend fun extractTextForPage(vIdx: Int): String {
        if (preloadedTexts.containsKey(vIdx)) {
            return preloadedTexts[vIdx]!!
        }
        val pages = _virtualPages.value
        if (vIdx !in pages.indices) return ""
        val vp = pages[vIdx]
        val bitmap = renderer?.renderVirtualPage(vp, 1200) ?: return ""
        val text = PdfTextExtractor.extractTextFromBitmap(bitmap)
        bitmap.recycle()
        preloadedTexts[vIdx] = text
        return text
    }

    private fun preloadNextPages(startIndex: Int, total: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var p = startIndex
            var validPagesFound = 0
            while (p < total && validPagesFound < 2) {
                if (!preloadedTexts.containsKey(p)) {
                    val text = extractTextForPage(p)
                    if (text.isNotBlank()) validPagesFound++
                } else {
                    if (preloadedTexts[p]!!.isNotBlank()) validPagesFound++
                }
                p++
            }
            // Limpiar caché antigua para no saturar memoria
            val keysToRemove = preloadedTexts.keys.filter { it < startIndex - 1 }
            keysToRemove.forEach { preloadedTexts.remove(it) }
        }
    }

    private fun readPageAndPreloadNext(startIndex: Int, startChunkIndex: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = _virtualPages.value.size
            if (startIndex >= total) {
                stopAutoRead()
                return@launch
            }

            var p = startIndex
            var text = extractTextForPage(p)

            // Saltar páginas en blanco
            while (text.isBlank() && p < total - 1) {
                p++
                text = extractTextForPage(p)
            }

            if (text.isBlank()) {
                stopAutoRead()
                return@launch
            }

            // Actualizar estado de UI
            if (p != _currentPage.value || _pageText.value != text) {
                _currentPage.value = p
                _pageText.value = text
                val vp = _virtualPages.value.getOrNull(p)
                if (vp != null) {
                    _pageBitmap.value = renderer?.renderVirtualPage(vp, 1200)
                }
                saveProgress()
            }

            val context = getApplication<Application>()
            val intent = Intent(context, com.example.service.AudioReaderService::class.java).apply {
                action = com.example.service.AudioReaderService.ACTION_START
                putExtra(com.example.service.AudioReaderService.EXTRA_TEXT, text)
                putExtra(com.example.service.AudioReaderService.EXTRA_SPEED, currentSpeed)
                putExtra(com.example.service.AudioReaderService.EXTRA_PITCH, currentPitch)
                putExtra(com.example.service.AudioReaderService.EXTRA_ENGINE, currentEngine)
                putExtra(com.example.service.AudioReaderService.EXTRA_VOICE_NAME, currentVoiceName)
                putExtra(com.example.service.AudioReaderService.EXTRA_START_INDEX, startChunkIndex)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)

            preloadNextPages(p + 1, total)
        }
    }

    private fun advanceAndReadNextPage() {
        if (!isAutoReading) return
        val total = _virtualPages.value.size
        if (_currentPage.value >= total - 1) {
            stopAutoRead()
            return
        }
        lastKnownChunkIndex = 0
        readPageAndPreloadNext(_currentPage.value + 1, 0)
    }

    fun nextPage() {
        stopAutoRead()
        lastKnownChunkIndex = 0
        val total = _virtualPages.value.size
        if (_currentPage.value < total - 1) {
            _currentPage.value += 1
            renderCurrentPage()
            saveProgress()
        }
    }

    fun previousPage() {
        stopAutoRead()
        lastKnownChunkIndex = 0
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
            renderCurrentPage()
            saveProgress()
        }
    }

    private fun renderCurrentPage() {
        viewModelScope.launch(Dispatchers.IO) {
            val p = _currentPage.value
            val pages = _virtualPages.value
            if (p !in pages.indices) {
                _pageBitmap.value = null
                _pageText.value = ""
                return@launch
            }
            val vp = pages[p]
            val bitmap = renderer?.renderVirtualPage(vp, 1200)
            _pageBitmap.value = bitmap
            if (bitmap != null) {
                val text = if (preloadedTexts.containsKey(p)) {
                    preloadedTexts[p]!!
                } else {
                    val extracted = PdfTextExtractor.extractTextFromBitmap(bitmap)
                    preloadedTexts[p] = extracted
                    extracted
                }
                _pageText.value = text
            } else {
                _pageText.value = ""
            }
        }
    }

    private fun saveProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentPdf.value?.let { pdf ->
                val updated = pdf.copy(lastReadPage = _currentPage.value)
                repository.updatePdf(updated)
                _currentPdf.value = updated
            }
        }
    }

    fun updatePdfEntity(summary: String, sentiment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentPdf.value?.let { pdf ->
                val updated = pdf.copy(summary = summary, sentiment = sentiment)
                repository.updatePdf(updated)
                _currentPdf.value = updated
            }
        }
    }

    fun generateSummaryAndSentiment() {
        viewModelScope.launch(Dispatchers.IO) {
            val text = _pageText.value
            if (text.isBlank()) return@launch
            
            _isGeneratingSummary.value = true
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val requestText = "Analiza el siguiente texto de un documento. Extrae un breve resumen de los puntos clave (máximo 3 oraciones) y determina la tonalidad/sentimiento predominante (ej. Positivo, Negativo, Neutral, Educativo, Formal, etc.). Formato: Resumen: [resumen] | Sentimiento: [sentimiento]. Texto: $text"
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = requestText)))
                    )
                )
                
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                if (responseText.contains("|")) {
                    val parts = responseText.split("|")
                    val summary = parts[0].replace("Resumen:", "").trim()
                    val sentiment = parts[1].replace("Sentimiento:", "").trim()
                    updatePdfEntity(summary, sentiment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }

    fun clearSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentPdf.value?.let { pdf ->
                val updated = pdf.copy(summary = null, sentiment = null)
                repository.updatePdf(updated)
                _currentPdf.value = updated
            }
        }
    }

    override fun onCleared() {
        stopAutoRead()
        super.onCleared()
        renderer?.close()
    }
}
