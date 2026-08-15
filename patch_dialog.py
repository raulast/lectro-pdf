import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

# Replace the DisposableEffect with a robust LaunchedEffect
old_effect = r"    DisposableEffect\(currentEngine\) \{.*?\n        \}\n    \}"

new_effect = """    LaunchedEffect(currentEngine) {
        var localTts: android.speech.tts.TextToSpeech? = null
        val latch = kotlinx.coroutines.CompletableDeferred<Boolean>()
        
        val initListener = android.speech.tts.TextToSpeech.OnInitListener { status ->
            latch.complete(status == android.speech.tts.TextToSpeech.SUCCESS)
        }
        
        localTts = if (currentEngine.isNotEmpty()) {
            android.speech.tts.TextToSpeech(context, initListener, currentEngine)
        } else {
            android.speech.tts.TextToSpeech(context, initListener)
        }
        
        try {
            val success = kotlinx.coroutines.withTimeoutOrNull(3000) { latch.await() } ?: false
            if (success) {
                // Poll for voices since some engines load them asynchronously
                for (i in 0..15) {
                    val voices = localTts?.voices?.toList()
                    if (!voices.isNullOrEmpty()) {
                        availableVoices = voices.sortedBy { it.name }
                        if (currentVoice.isNotEmpty() && availableVoices.none { it.name == currentVoice }) {
                            currentVoice = ""
                        }
                        break
                    }
                    kotlinx.coroutines.delay(200)
                }
            } else {
                availableVoices = emptyList()
            }
        } finally {
            try {
                localTts?.shutdown()
            } catch (e: Exception) {}
        }
    }"""

content = re.sub(old_effect, new_effect, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

