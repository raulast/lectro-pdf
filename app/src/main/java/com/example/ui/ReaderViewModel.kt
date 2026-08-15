package com.example.ui

import android.app.Application
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
import com.example.domain.PdfRendererWrapper
import com.example.domain.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PdfRepository
    
    private val _currentPdf = MutableStateFlow<PdfDocumentEntity?>(null)
    val currentPdf: StateFlow<PdfDocumentEntity?> = _currentPdf.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    private val _pageText = MutableStateFlow("")
    val pageText: StateFlow<String> = _pageText.asStateFlow()
    
    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private var renderer: PdfRendererWrapper? = null

    private var isAutoReading = false
    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f
    private var currentEngine = ""
    private var currentVoiceName = ""
    private val preloadedTexts = mutableMapOf<Int, String>()

    val currentChunkIndex = com.example.service.AudioReaderService.currentChunkIndex
    var lastKnownChunkIndex = 0


    init {
        val pdfDao = AppDatabase.getDatabase(application).pdfDao()
        repository = PdfRepository(pdfDao)
        
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

    fun loadPdf(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdf = repository.getPdfById(id)
            if (pdf != null) {
                _currentPdf.value = pdf
                _currentPage.value = pdf.lastReadPage
                renderer = PdfRendererWrapper(getApplication(), Uri.parse(pdf.uriString))
                renderCurrentPage()
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

    private suspend fun extractTextForPage(pageIndex: Int): String {
        if (preloadedTexts.containsKey(pageIndex)) {
            return preloadedTexts[pageIndex]!!
        }
        val bitmap = renderer?.renderPage(pageIndex, 1200) ?: return ""
        val text = PdfTextExtractor.extractTextFromBitmap(bitmap)
        preloadedTexts[pageIndex] = text
        return text
    }

    private fun preloadNextPages(startIndex: Int, total: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var p = startIndex
            var validPagesFound = 0
            while (p < total && validPagesFound < 2) {
                if (!preloadedTexts.containsKey(p)) {
                    val bitmap = renderer?.renderPage(p, 1200)
                    if (bitmap != null) {
                        val text = PdfTextExtractor.extractTextFromBitmap(bitmap)
                        preloadedTexts[p] = text
                        if (text.isNotBlank()) validPagesFound++
                    }
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
            val total = _currentPdf.value?.totalPages ?: 0
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
                _pageBitmap.value = renderer?.renderPage(p, 1200)
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
        val total = _currentPdf.value?.totalPages ?: 0
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
        if (_currentPdf.value != null && _currentPage.value < _currentPdf.value!!.totalPages - 1) {
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
            val bitmap = renderer?.renderPage(p, 1200)
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
