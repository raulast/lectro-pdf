import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

# Replace the voice setting block in onInit
oninit_block = """    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            
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

            pendingText?.let {
                speakText(it, 0)
                pendingText = null
            }"""

content = re.sub(r"    override fun onInit\(status: Int\) \{.*?pendingText = null\n            \}", oninit_block, content, flags=re.DOTALL)

# Replace the voice setting block in ACTION_START
action_start = """                } else {
                    if (!requestedVoice.isNullOrEmpty()) {
                        try {
                            val voices = tts?.voices
                            val selectedVoice = voices?.find { it.name == requestedVoice }
                            if (selectedVoice != null) {
                                tts?.language = selectedVoice.locale
                                tts?.voice = selectedVoice
                            }
                        } catch (e: Exception) {}
                    }
                    if (isTtsReady) {
                        speakText(text, startIndex)
                    } else {
                        pendingText = text
                    }
                }"""

content = re.sub(r"                \} else \{\n                    if \(\!requestedVoice\.isNullOrEmpty\(\)\).*?pendingText = text\n                    \}\n                \}", action_start, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)

