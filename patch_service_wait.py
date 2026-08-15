import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

# Replace onInit
new_oninit = """    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Launch coroutine to wait for voices if needed
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                if (!currentVoiceName.isNullOrEmpty()) {
                    for (i in 0..15) { // Wait up to 3 seconds for voices to load
                        val voices = tts?.voices
                        if (!voices.isNullOrEmpty()) {
                            val selectedVoice = voices.find { it.name == currentVoiceName }
                            if (selectedVoice != null) {
                                tts?.language = selectedVoice.locale
                                tts?.voice = selectedVoice
                            }
                            break
                        }
                        kotlinx.coroutines.delay(200)
                    }
                }
                
                isTtsReady = true
                pendingText?.let {
                    speakText(it, 0)
                    pendingText = null
                }
            }
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {"""

# We need to add import kotlinx.coroutines.launch
if "import kotlinx.coroutines.launch" not in content:
    content = content.replace("import kotlinx.coroutines.flow.asSharedFlow", "import kotlinx.coroutines.flow.asSharedFlow\nimport kotlinx.coroutines.launch")

content = re.sub(r"    override fun onInit\(status: Int\) \{.*?tts\?\.setOnUtteranceProgressListener\(object : UtteranceProgressListener\(\) \{", new_oninit, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)

