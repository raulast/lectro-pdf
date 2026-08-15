import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

# ADD voiceName state
if "voiceName" not in content:
    content = content.replace('var voiceEngine by remember { mutableStateOf(prefs.getString("voice_engine", "") ?: "") }', 'var voiceEngine by remember { mutableStateOf(prefs.getString("voice_engine", "") ?: "") }\n    var voiceName by remember { mutableStateOf(prefs.getString("voice_name", "") ?: "") }')

# Update VoiceSettingsDialog Call
dialog_call = """        if (showVoiceSettings) {
            VoiceSettingsDialog(
                speed = voiceSpeed,
                pitch = voicePitch,
                engine = voiceEngine,
                voice = voiceName,
                enginesList = ttsEngines,
                onDismiss = { showVoiceSettings = false },
                onValuesChange = { newSpeed, newPitch, newEngine, newVoice ->
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
                }
            )
        }"""
content = re.sub(r"        if \(showVoiceSettings\) \{.*?VoiceSettingsDialog.*?onDismiss = \{ showVoiceSettings = false \}.*?\}\n            \)\n        \}", dialog_call, content, flags=re.DOTALL)


# Update toggleAutoRead call
content = content.replace("viewModel.toggleAutoRead(voiceSpeed, voicePitch, voiceEngine)", "viewModel.toggleAutoRead(voiceSpeed, voicePitch, voiceEngine, voiceName)")

# Rewrite VoiceSettingsDialog
new_dialog = """@Composable
fun VoiceSettingsDialog(
    speed: Float,
    pitch: Float,
    engine: String,
    voice: String,
    enginesList: List<android.speech.tts.TextToSpeech.EngineInfo>,
    onDismiss: () -> Unit,
    onValuesChange: (Float, Float, String, String) -> Unit
) {
    var currentSpeed by remember { mutableFloatStateOf(speed) }
    var currentPitch by remember { mutableFloatStateOf(pitch) }
    var currentEngine by remember { mutableStateOf(engine) }
    var currentVoice by remember { mutableStateOf(voice) }
    
    var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(currentEngine) {
        try {
            var tempTts: android.speech.tts.TextToSpeech? = null
            val initListener = android.speech.tts.TextToSpeech.OnInitListener { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    try {
                        val voices = tempTts?.voices?.toList() ?: emptyList()
                        // Sort by locale and name
                        availableVoices = voices.sortedBy { it.name }
                        
                        // Select default if current voice not found
                        if (currentVoice.isNotEmpty() && voices.none { it.name == currentVoice }) {
                            currentVoice = ""
                        }
                    } catch (e: Exception) {}
                }
            }
            tempTts = if (currentEngine.isNotEmpty()) {
                android.speech.tts.TextToSpeech(context, initListener, currentEngine)
            } else {
                android.speech.tts.TextToSpeech(context, initListener)
            }
        } catch (e: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración de Voz") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Motor de Voz:", style = MaterialTheme.typography.titleSmall)
                enginesList.forEach { eng ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentEngine = eng.name }
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = (currentEngine == eng.name) || (currentEngine.isEmpty() && eng.name.contains("google")),
                            onClick = { currentEngine = eng.name }
                        )
                        Text(eng.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                if (availableVoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Voz Específica:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (currentVoice.isEmpty()) "Por Defecto" else currentVoice)
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxHeight(0.5f)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Por Defecto") },
                                onClick = { 
                                    currentVoice = ""
                                    expanded = false 
                                }
                            )
                            availableVoices.forEach { v ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(v.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(v.locale.displayName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    },
                                    onClick = { 
                                        currentVoice = v.name
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Velocidad de Lectura: ${String.format(java.util.Locale.US, "%.1f", currentSpeed)}x",
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
                    text = "Tono de Voz (Pitch): ${String.format(java.util.Locale.US, "%.1f", currentPitch)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { currentPitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent("com.android.settings.TTS_SETTINGS")
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No se pudieron abrir los ajustes", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text("Abrir Ajustes del Motor")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValuesChange(currentSpeed, currentPitch, currentEngine, currentVoice)
                    onDismiss()
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
}"""

content = re.sub(r"@Composable\nfun VoiceSettingsDialog.*?\}\n    \)\n\}", new_dialog, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

