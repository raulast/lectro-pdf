import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

old_button = """                    IconButton(onClick = {
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
                    })"""

new_button = """                    IconButton(onClick = {
                        if (!isPlaying && pageText.isBlank()) {
                            Toast.makeText(context, "No se pudo extraer texto, buscando siguiente...", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.toggleAutoRead(voiceSpeed, voicePitch)
                    })"""

content = content.replace(old_button, new_button)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)
