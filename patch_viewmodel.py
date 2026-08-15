import re

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "r") as f:
    content = f.read()

# Add currentChunkIndex state flow
content = content.replace("    private val preloadedTexts = mutableMapOf<Int, String>()", "    private val preloadedTexts = mutableMapOf<Int, String>()\n\n    val currentChunkIndex = com.example.service.AudioReaderService.currentChunkIndex\n    var lastKnownChunkIndex = 0")

init_block_replacement = """    init {
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
    }"""
content = re.sub(r"    init \{.*?\}", init_block_replacement, content, flags=re.DOTALL)

old_toggle = """    fun toggleAutoRead(speed: Float, pitch: Float, engine: String) {
        val context = getApplication<Application>()
        if (com.example.service.AudioReaderService.isServiceRunning.value) {
            stopAutoRead()
        } else {
            isAutoReading = true
            currentSpeed = speed
            currentPitch = pitch
            currentEngine = engine
            readPageAndPreloadNext(_currentPage.value)
        }
    }"""
new_toggle = """    fun toggleAutoRead(speed: Float, pitch: Float, engine: String) {
        val context = getApplication<Application>()
        if (com.example.service.AudioReaderService.isServiceRunning.value) {
            stopAutoRead()
        } else {
            isAutoReading = true
            currentSpeed = speed
            currentPitch = pitch
            currentEngine = engine
            readPageAndPreloadNext(_currentPage.value, lastKnownChunkIndex)
        }
    }
    
    fun seekToChunk(chunkIndex: Int) {
        lastKnownChunkIndex = chunkIndex
        if (!isAutoReading && !com.example.service.AudioReaderService.isServiceRunning.value) {
            isAutoReading = true
        }
        readPageAndPreloadNext(_currentPage.value, chunkIndex)
    }"""
content = content.replace(old_toggle, new_toggle)

# Update readPageAndPreloadNext
read_page = """    private fun readPageAndPreloadNext(startIndex: Int) {"""
new_read_page = """    private fun readPageAndPreloadNext(startIndex: Int, startChunkIndex: Int = 0) {"""
content = content.replace(read_page, new_read_page)

# Put startChunkIndex in Intent
intent_str = """                action = com.example.service.AudioReaderService.ACTION_START
                putExtra(com.example.service.AudioReaderService.EXTRA_TEXT, text)
                putExtra(com.example.service.AudioReaderService.EXTRA_SPEED, currentSpeed)
                putExtra(com.example.service.AudioReaderService.EXTRA_PITCH, currentPitch)
                putExtra(com.example.service.AudioReaderService.EXTRA_ENGINE, currentEngine)
            }"""
new_intent_str = """                action = com.example.service.AudioReaderService.ACTION_START
                putExtra(com.example.service.AudioReaderService.EXTRA_TEXT, text)
                putExtra(com.example.service.AudioReaderService.EXTRA_SPEED, currentSpeed)
                putExtra(com.example.service.AudioReaderService.EXTRA_PITCH, currentPitch)
                putExtra(com.example.service.AudioReaderService.EXTRA_ENGINE, currentEngine)
                putExtra(com.example.service.AudioReaderService.EXTRA_START_INDEX, startChunkIndex)
            }"""
content = content.replace(intent_str, new_intent_str)

# advanceAndReadNextPage should reset chunk
advance_page = """    private fun advanceAndReadNextPage() {
        if (!isAutoReading) return
        val total = _currentPdf.value?.totalPages ?: 0
        if (_currentPage.value >= total - 1) {
            stopAutoRead()
            return
        }
        readPageAndPreloadNext(_currentPage.value + 1)
    }"""
new_advance_page = """    private fun advanceAndReadNextPage() {
        if (!isAutoReading) return
        val total = _currentPdf.value?.totalPages ?: 0
        if (_currentPage.value >= total - 1) {
            stopAutoRead()
            return
        }
        lastKnownChunkIndex = 0
        readPageAndPreloadNext(_currentPage.value + 1, 0)
    }"""
content = content.replace(advance_page, new_advance_page)

# manual paging resets chunk
content = content.replace("    fun nextPage() {\n        stopAutoRead()", "    fun nextPage() {\n        stopAutoRead()\n        lastKnownChunkIndex = 0")
content = content.replace("    fun previousPage() {\n        stopAutoRead()", "    fun previousPage() {\n        stopAutoRead()\n        lastKnownChunkIndex = 0")

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "w") as f:
    f.write(content)
