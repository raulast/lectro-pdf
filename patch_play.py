import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

old_play = """                    IconButton(onClick = {
                        if (!isPlaying && pageText.isBlank()) {
                            Toast.makeText(context, "No se pudo extraer texto de esta página o aún está cargando.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        
                        val intent = Intent(context, AudioReaderService::class.java).apply {
                            action = if (isPlaying) AudioReaderService.ACTION_STOP else AudioReaderService.ACTION_START
                            putExtra(AudioReaderService.EXTRA_TEXT, pageText)
                            putExtra(AudioReaderService.EXTRA_SPEED, voiceSpeed)
                            putExtra(AudioReaderService.EXTRA_PITCH, voicePitch)
                        }
                        ContextCompat.startForegroundService(context, intent)
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Leer en voz alta"
                        )
                    }"""

new_play = """                    IconButton(onClick = {
                        if (!isPlaying && pageText.isBlank()) {
                            Toast.makeText(context, "No se pudo extraer texto de esta página o aún está cargando.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        viewModel.toggleAutoRead(voiceSpeed, voicePitch, voiceEngine, voiceName)
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Leer en voz alta"
                        )
                    }"""

content = content.replace(old_play, new_play)

# Replace VoiceSettingsDialog LaunchedEffect with DisposableEffect
old_effect = """    LaunchedEffect(currentEngine) {
        try {
            var tempTts: android.speech.tts.TextToSpeech? = null
            val initListener = android.speech.tts.TextToSpeech.OnInitListener { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    try {
                        val voices = tempTts?.voices?.toList() ?: emptyList()
                        // Sort by locale and name
                        availableVoices = voices.sortedBy { it.name }
                        
                        // Select default if current voice not found
                        if (currentVoice.isNotEmpty() && voices.none { it.name == currentVoice }) {
                            currentVoice = ""
                        }
                    } catch (e: Exception) {}
                }
            }
            tempTts = if (currentEngine.isNotEmpty()) {
                android.speech.tts.TextToSpeech(context, initListener, currentEngine)
            } else {
                android.speech.tts.TextToSpeech(context, initListener)
            }
        } catch (e: Exception) {}
    }"""

new_effect = """    DisposableEffect(currentEngine) {
        var tempTts: android.speech.tts.TextToSpeech? = null
        val initListener = android.speech.tts.TextToSpeech.OnInitListener { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                try {
                    val voices = tempTts?.voices?.toList() ?: emptyList()
                    availableVoices = voices.sortedBy { it.name }
                    
                    if (currentVoice.isNotEmpty() && voices.none { it.name == currentVoice }) {
                        currentVoice = ""
                    }
                } catch (e: Exception) {}
            }
        }
        tempTts = if (currentEngine.isNotEmpty()) {
            android.speech.tts.TextToSpeech(context, initListener, currentEngine)
        } else {
            android.speech.tts.TextToSpeech(context, initListener)
        }
        
        onDispose {
            try {
                tempTts?.shutdown()
            } catch (e: Exception) {}
        }
    }"""

content = content.replace(old_effect, new_effect)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

