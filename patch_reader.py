import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

# Add mutableFloatStateOf to imports if missing
if "import androidx.compose.runtime.mutableFloatStateOf" not in content:
    content = content.replace("import androidx.compose.runtime.mutableStateOf", "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.mutableFloatStateOf")

if "import android.content.Context" not in content:
    content = content.replace("import android.content.Intent", "import android.content.Context\nimport android.content.Intent")

if "import androidx.compose.material.icons.filled.SettingsVoice" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.PlayArrow", "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.SettingsVoice")

if "import java.util.Locale" not in content:
    content = content.replace("import android.widget.Toast", "import android.widget.Toast\nimport java.util.Locale")

# 1. State variables
state_vars = """
    val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("voice_speed", 1.0f)) }
    var voicePitch by remember { mutableFloatStateOf(prefs.getFloat("voice_pitch", 1.0f)) }
    var showVoiceSettings by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()"""

content = content.replace("val coroutineScope = rememberCoroutineScope()", state_vars)

# 2. Put extra
intent_block = """                        val intent = Intent(context, AudioReaderService::class.java).apply {
                            action = if (isPlaying) AudioReaderService.ACTION_STOP else AudioReaderService.ACTION_START
                            putExtra(AudioReaderService.EXTRA_TEXT, pageText)
                            putExtra(AudioReaderService.EXTRA_SPEED, voiceSpeed)
                            putExtra(AudioReaderService.EXTRA_PITCH, voicePitch)
                        }"""
content = re.sub(r"val intent = Intent\(context, AudioReaderService::class\.java\)\.apply \{\s+action = if \(isPlaying\) AudioReaderService\.ACTION_STOP else AudioReaderService\.ACTION_START\s+putExtra\(AudioReaderService\.EXTRA_TEXT, pageText\)\s+\}", intent_block, content)

# 3. Add Settings button
settings_button = """                    IconButton(onClick = { showVoiceSettings = true }) {
                        Icon(Icons.Filled.SettingsVoice, contentDescription = "Configurar Voz")
                    }
                    IconButton(onClick = { viewModel.generateSummaryAndSentiment() }) {"""
content = content.replace("IconButton(onClick = { viewModel.generateSummaryAndSentiment() }) {", settings_button)

# 4. Add Dialog composable call inside Scaffold content (at the end of Column)
dialog_call = """            }
        }
        
        if (showVoiceSettings) {
            VoiceSettingsDialog(
                speed = voiceSpeed,
                pitch = voicePitch,
                onDismiss = { showVoiceSettings = false },
                onValuesChange = { newSpeed, newPitch ->
                    voiceSpeed = newSpeed
                    voicePitch = newPitch
                    prefs.edit()
                        .putFloat("voice_speed", newSpeed)
                        .putFloat("voice_pitch", newPitch)
                        .apply()
                }
            )
        }
    }
}

@Composable
fun VoiceSettingsDialog(
    speed: Float,
    pitch: Float,
    onDismiss: () -> Unit,
    onValuesChange: (Float, Float) -> Unit
) {
    var currentSpeed by remember { mutableFloatStateOf(speed) }
    var currentPitch by remember { mutableFloatStateOf(pitch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración de Voz") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Velocidad de Lectura: ${String.format(Locale.US, "%.1f", currentSpeed)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentSpeed,
                    onValueChange = { currentSpeed = it },
                    valueRange = 0.5f..2.5f,
                    steps = 19
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tono de Voz (Pitch): ${String.format(Locale.US, "%.1f", currentPitch)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { currentPitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValuesChange(currentSpeed, currentPitch)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
"""
content = re.sub(r"            }\n        }\n    }\n}$", dialog_call, content)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

