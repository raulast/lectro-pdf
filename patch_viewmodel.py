import re

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "r") as f:
    content = f.read()

# 1. Imports
if "android.content.Intent" not in content:
    content = content.replace("import android.net.Uri", "import android.content.Intent\nimport android.net.Uri")

# 2. State variables
state_vars = """    private var renderer: PdfRendererWrapper? = null

    private var isAutoReading = false
    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f
    private val preloadedTexts = mutableMapOf<Int, String>()
"""
content = content.replace("    private var renderer: PdfRendererWrapper? = null", state_vars)

# 3. Init block (collect events)
init_block = """    init {
        val pdfDao = AppDatabase.getDatabase(application).pdfDao()
        repository = PdfRepository(pdfDao)
        
        viewModelScope.launch(Dispatchers.Main) {
            com.example.service.AudioReaderService.pageFinishedEvent.collect {
                if (isAutoReading) {
                    advanceAndReadNextPage()
                }
            }
        }
    }"""
content = re.sub(r"    init \{.*?\}", init_block, content, flags=re.DOTALL)

# 4. New methods
new_methods = """
    fun toggleAutoRead(speed: Float, pitch: Float) {
        val context = getApplication<Application>()
        if (com.example.service.AudioReaderService.isServiceRunning.value) {
            stopAutoRead()
        } else {
            isAutoReading = true
            currentSpeed = speed
            currentPitch = pitch
            readPageAndPreloadNext(_currentPage.value)
        }
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

    private fun readPageAndPreloadNext(startIndex: Int) {
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
        readPageAndPreloadNext(_currentPage.value + 1)
    }
"""

content = content.replace("    fun nextPage() {", new_methods + "\n    fun nextPage() {")

# 5. Modify renderCurrentPage
render_block = """    private fun renderCurrentPage() {
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
    }"""
content = re.sub(r"    private fun renderCurrentPage\(\) \{.*?\}\s*\}", render_block, content, flags=re.DOTALL)

# 6. Stop auto read on manual navigation
content = content.replace("    fun nextPage() {\n        if", "    fun nextPage() {\n        stopAutoRead()\n        if")
content = content.replace("    fun previousPage() {\n        if", "    fun previousPage() {\n        stopAutoRead()\n        if")
content = content.replace("        super.onCleared()\n        renderer?.close()", "        stopAutoRead()\n        super.onCleared()\n        renderer?.close()")

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "w") as f:
    f.write(content)
