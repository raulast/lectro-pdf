import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

# Replace speakText definition
new_speak = """    private fun speakText(text: String, startIndex: Int) {
        if (tts != null && text.isNotEmpty()) {
            tts?.stop()
            
            // Re-apply voice just before speaking to handle async TTS initialization
            if (!currentVoiceName.isNullOrEmpty()) {
                try {
                    val voices = tts?.voices
                    val selectedVoice = voices?.find { it.name == currentVoiceName }
                    if (selectedVoice != null) {
                        tts?.language = selectedVoice.locale
                        tts?.voice = selectedVoice
                    }
                } catch (e: Exception) {}
            }
            
            tts?.setSpeechRate(currentSpeed)
            tts?.setPitch(currentPitch)
            _currentChunkIndex.value = startIndex"""

content = re.sub(r"    private fun speakText\(text: String, startIndex: Int\) \{\n        if \(tts \!\= null && text\.isNotEmpty\(\)\) \{\n            tts\?\.stop\(\)\n            tts\?\.setSpeechRate\(currentSpeed\)\n            tts\?\.setPitch\(currentPitch\)\n            _currentChunkIndex\.value = startIndex", new_speak, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)

