import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

# Add EXTRA_START_INDEX and currentChunkIndex
content = content.replace('const val EXTRA_ENGINE = "EXTRA_ENGINE"', 'const val EXTRA_ENGINE = "EXTRA_ENGINE"\n        const val EXTRA_START_INDEX = "EXTRA_START_INDEX"')

content = content.replace('val isServiceRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceRunning.asStateFlow()', 'val isServiceRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceRunning.asStateFlow()\n\n        val _currentChunkIndex = kotlinx.coroutines.flow.MutableStateFlow(-1)\n        val currentChunkIndex: kotlinx.coroutines.flow.StateFlow<Int> = _currentChunkIndex.asStateFlow()')

content = content.replace("private val _pageFinishedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)", "private val _pageFinishedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)")

# ACTION_START update
start_block = """            ACTION_START -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                currentPitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f)
                val requestedEngine = intent.getStringExtra(EXTRA_ENGINE)
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

                startForegroundService(text)

                if (tts == null || currentEngine != requestedEngine) {
                    tts?.stop()
                    tts?.shutdown()
                    isTtsReady = false
                    currentEngine = requestedEngine
                    tts = if (!requestedEngine.isNullOrEmpty()) {
                        TextToSpeech(this, this, requestedEngine)
                    } else {
                        TextToSpeech(this, this)
                    }
                    pendingText = text
                    // No podemos saltar directamente al indice si el TTS no está listo, 
                    // así que asumimos que empezará de cero o lo guardamos si lo necesitamos
                } else {
                    if (isTtsReady) {
                        speakText(text, startIndex)
                    } else {
                        pendingText = text
                    }
                }
            }"""
content = re.sub(r"            ACTION_START -> \{.*?ACTION_STOP", start_block + "\n            ACTION_STOP", content, flags=re.DOTALL)

# Update onInit
content = content.replace("speakText(it)", "speakText(it, 0)")

# Update UtteranceProgressListener
listener_block = """            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlaying = true
                    if (utteranceId?.startsWith("CHUNK_") == true) {
                        val idx = utteranceId.substringAfter("CHUNK_").toIntOrNull()
                        if (idx != null) {
                            _currentChunkIndex.value = idx
                        }
                    }
                }
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                        _pageFinishedEvent.tryEmit(Unit)
                        _currentChunkIndex.value = -1
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                        _pageFinishedEvent.tryEmit(Unit)
                        _currentChunkIndex.value = -1
                    }
                }
            })"""
content = re.sub(r"            tts\?\.setOnUtteranceProgressListener.*?\}\)\s*\}", listener_block + "\n        }", content, flags=re.DOTALL)

# Update speakText
speak_block = """    private fun speakText(text: String, startIndex: Int) {
        if (tts != null && text.isNotEmpty()) {
            tts?.stop()
            tts?.setSpeechRate(currentSpeed)
            tts?.setPitch(currentPitch)
            _currentChunkIndex.value = startIndex

            val chunks = com.example.utils.TextChunker.parse(text)
            
            var lastId: String? = null
            var queuedCount = 0
            
            for (i in startIndex until chunks.size) {
                val chunk = chunks[i]
                if (chunk.isSpeakable) {
                    val queueMode = if (queuedCount == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    val currentId = "CHUNK_$i"
                    lastId = currentId
                    tts?.speak(chunk.text.trim(), queueMode, null, currentId)
                    
                    // Delay calculation based on punctuation
                    val t = chunk.text
                    val delay = when {
                        t.contains("\\n") -> 800L
                        t.contains(".") || t.contains("!") || t.contains("?") -> 500L
                        t.contains(";") -> 300L
                        t.contains(",") -> 150L
                        else -> 0L
                    }
                    if (delay > 0) {
                        tts?.playSilentUtterance(delay, TextToSpeech.QUEUE_ADD, "SILENCE_$i")
                    }
                    queuedCount++
                }
            }
            
            lastUtteranceId = lastId

            if (queuedCount == 0) {
                 isPlaying = false
                 _pageFinishedEvent.tryEmit(Unit)
            }
        }
    }"""
content = re.sub(r"    private fun speakText\(text: String\).*?\}\s*\}\s*private fun stopPlayback", speak_block + "\n\n    private fun stopPlayback", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)

