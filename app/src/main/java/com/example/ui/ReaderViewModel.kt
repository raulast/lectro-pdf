package com.example.ui

import android.app.Application
import android.graphics.Bitmap
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

    init {
        val pdfDao = AppDatabase.getDatabase(application).pdfDao()
        repository = PdfRepository(pdfDao)
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

    fun nextPage() {
        if (_currentPdf.value != null && _currentPage.value < _currentPdf.value!!.totalPages - 1) {
            _currentPage.value += 1
            renderCurrentPage()
            saveProgress()
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
            renderCurrentPage()
            saveProgress()
        }
    }

    private fun renderCurrentPage() {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = renderer?.renderPage(_currentPage.value, 1200)
            _pageBitmap.value = bitmap
            if (bitmap != null) {
                // Extract text for TTS
                val text = PdfTextExtractor.extractTextFromBitmap(bitmap)
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
        super.onCleared()
        renderer?.close()
    }
}
