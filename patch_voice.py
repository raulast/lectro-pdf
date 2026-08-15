import re

# UPDATE AudioReaderService
with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    service_content = f.read()

service_content = service_content.replace('const val EXTRA_ENGINE = "EXTRA_ENGINE"', 'const val EXTRA_ENGINE = "EXTRA_ENGINE"\n        const val EXTRA_VOICE_NAME = "EXTRA_VOICE_NAME"')

# Update ACTION_START
start_block = """                val requestedEngine = intent.getStringExtra(EXTRA_ENGINE)
                val requestedVoice = intent.getStringExtra(EXTRA_VOICE_NAME)
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

                startForegroundService(text)

                if (tts == null || currentEngine != requestedEngine) {
                    tts?.stop()
                    tts?.shutdown()
                    isTtsReady = false
                    currentEngine = requestedEngine
                    
                    val initListener = TextToSpeech.OnInitListener { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            isTtsReady = true
                            // Assign voice if requested
                            if (!requestedVoice.isNullOrEmpty()) {
                                try {
                                    val voices = tts?.voices
                                    voices?.find { it.name == requestedVoice }?.let { voice ->
                                        tts?.voice = voice
                                    }
                                } catch (e: Exception) {}
                            }
                            if (pendingText != null) {
                                speakText(pendingText!!, 0)
                                pendingText = null
                            }
                        }
                    }

                    tts = if (!requestedEngine.isNullOrEmpty()) {
                        TextToSpeech(this, initListener, requestedEngine)
                    } else {
                        TextToSpeech(this, initListener)
                    }
                    pendingText = text
                } else {
                    // Update voice if changed
                    if (!requestedVoice.isNullOrEmpty()) {
                        try {
                            val voices = tts?.voices
                            voices?.find { it.name == requestedVoice }?.let { voice ->
                                tts?.voice = voice
                            }
                        } catch (e: Exception) {}
                    }
                    
                    if (isTtsReady) {
                        speakText(text, startIndex)
                    } else {
                        pendingText = text
                    }
                }"""

service_content = re.sub(r"                val requestedEngine = intent\.getStringExtra\(EXTRA_ENGINE\).*?pendingText = text\n                } else \{.*?pendingText = text\n                    \}\n                \}", start_block, service_content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(service_content)


# UPDATE ReaderViewModel
with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "r") as f:
    vm_content = f.read()

vm_content = vm_content.replace('private var currentEngine = ""', 'private var currentEngine = ""\n    private var currentVoiceName = ""')
vm_content = vm_content.replace('fun toggleAutoRead(speed: Float, pitch: Float, engine: String)', 'fun toggleAutoRead(speed: Float, pitch: Float, engine: String, voiceName: String)')
vm_content = vm_content.replace('currentEngine = engine', 'currentEngine = engine\n            currentVoiceName = voiceName')

vm_content = vm_content.replace('putExtra(com.example.service.AudioReaderService.EXTRA_ENGINE, currentEngine)', 'putExtra(com.example.service.AudioReaderService.EXTRA_ENGINE, currentEngine)\n                putExtra(com.example.service.AudioReaderService.EXTRA_VOICE_NAME, currentVoiceName)')

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "w") as f:
    f.write(vm_content)

