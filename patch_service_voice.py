import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

# 1. Add currentVoiceName
content = content.replace("private var currentEngine: String? = null", "private var currentEngine: String? = null\n    private var currentVoiceName: String? = null")

# 2. Update ACTION_START
start_block = """            ACTION_START -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                currentPitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f)
                val requestedEngine = intent.getStringExtra(EXTRA_ENGINE)
                val requestedVoice = intent.getStringExtra(EXTRA_VOICE_NAME)
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                
                currentVoiceName = requestedVoice

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
                } else {
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
                }
            }"""

content = re.sub(r"            ACTION_START -> \{.*?ACTION_STOP", start_block + "\n            ACTION_STOP", content, flags=re.DOTALL)


# 3. Update onInit
oninit_block = """    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            
            if (!currentVoiceName.isNullOrEmpty()) {
                try {
                    val voices = tts?.voices
                    voices?.find { it.name == currentVoiceName }?.let { voice ->
                        tts?.voice = voice
                    }
                } catch (e: Exception) {}
            }

            pendingText?.let {
                speakText(it, 0)
                pendingText = null
            }"""

content = re.sub(r"    override fun onInit\(status: Int\) \{.*?pendingText = null\n            \}", oninit_block, content, flags=re.DOTALL)


with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)

