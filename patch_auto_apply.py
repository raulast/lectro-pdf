import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

new_onvalues = """                onValuesChange = { newSpeed, newPitch, newEngine, newVoice ->
                    voiceSpeed = newSpeed
                    voicePitch = newPitch
                    voiceEngine = newEngine
                    voiceName = newVoice
                    prefs.edit()
                        .putFloat("voice_speed", newSpeed)
                        .putFloat("voice_pitch", newPitch)
                        .putString("voice_engine", newEngine)
                        .putString("voice_name", newVoice)
                        .apply()
                        
                    // Si está leyendo, reiniciar con la nueva configuración
                    if (com.example.service.AudioReaderService.isServiceRunning.value) {
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice) // Stop
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice) // Start
                    }
                }"""

content = re.sub(r"                onValuesChange = \{ newSpeed, newPitch, newEngine, newVoice ->.*?\.apply\(\)\n                \}", new_onvalues, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

