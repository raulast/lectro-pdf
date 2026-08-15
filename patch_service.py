import re

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "r") as f:
    content = f.read()

init_block = """        if (status == TextToSpeech.SUCCESS) {
            // Intentar usar la voz por defecto que el usuario ha configurado en su sistema
            val defaultVoice = tts?.defaultVoice
            if (defaultVoice != null) {
                tts?.voice = defaultVoice
            } else {
                // Fallback seguro a español o idioma por defecto si no hay voz establecida
                val locale = Locale("es")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
            
            isTtsReady = true"""

content = re.sub(r"        if \(status == TextToSpeech\.SUCCESS\) \{\s+val locale = Locale\(\"es\"\)\s+val result = tts\?\.setLanguage\(locale\)\s+if \(result == TextToSpeech\.LANG_MISSING_DATA \|\| result == TextToSpeech\.LANG_NOT_SUPPORTED\) \{\s+tts\?\.language = Locale\.getDefault\(\)\s+\}\s+isTtsReady = true", init_block, content)

with open("app/src/main/java/com/example/service/AudioReaderService.kt", "w") as f:
    f.write(content)
