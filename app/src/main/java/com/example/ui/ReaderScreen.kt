package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import java.util.Locale
import androidx.core.content.ContextCompat
import com.example.service.AudioReaderService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    pdfId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentPdf by viewModel.currentPdf.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    val pageText by viewModel.pageText.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()

    var isTextMode by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var scale by remember { mutableStateOf(1f) }
    
    val isPlaying by AudioReaderService.isServiceRunning.collectAsState()

    
    val prefs = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("voice_speed", 1.0f)) }
    var voicePitch by remember { mutableFloatStateOf(prefs.getFloat("voice_pitch", 1.0f)) }
    var voiceEngine by remember { mutableStateOf(prefs.getString("voice_engine", "") ?: "") }
    var voiceName by remember { mutableStateOf(prefs.getString("voice_name", "") ?: "") }
    
    var ttsEngines by remember { mutableStateOf<List<android.speech.tts.TextToSpeech.EngineInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val tts = android.speech.tts.TextToSpeech(context) {}
            ttsEngines = tts.engines ?: emptyList()
            tts.shutdown()
        } catch (e: Exception) {}
    }
    var showVoiceSettings by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdfId) {
        viewModel.loadPdf(pdfId)
    }

    val backgroundColor = if (isNightMode) Color(0xFF121212) else Color.White
    val contentColor = if (isNightMode) Color.LightGray else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPdf?.title ?: "Leyendo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { isNightMode = !isNightMode }) {
                        Icon(
                            if (isNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Modo Noche"
                        )
                    }
                    IconButton(onClick = { isTextMode = !isTextMode }) {
                        Icon(
                            if (isTextMode) Icons.Filled.Image else Icons.Filled.TextFields,
                            contentDescription = "Cambiar Modo"
                        )
                    }
                    if (isTextMode) {
                        IconButton(onClick = { fontSize = (fontSize.value + 2).sp }) {
                            Icon(Icons.Filled.FormatSize, contentDescription = "Aumentar Fuente")
                        }
                        IconButton(onClick = { fontSize = (fontSize.value - 2).coerceAtLeast(10f).sp }) {
                            Icon(Icons.Filled.TextDecrease, contentDescription = "Disminuir Fuente")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { viewModel.previousPage() }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Anterior")
                    }
                    Text(
                        text = "${(currentPdf?.lastReadPage ?: 0) + 1} / ${currentPdf?.totalPages ?: 0}",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = { viewModel.nextPage() }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Siguiente")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
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
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = "Leer en voz alta"
                        )
                    }
                                        IconButton(onClick = { showVoiceSettings = true }) {
                        Icon(Icons.Filled.SettingsVoice, contentDescription = "Configurar Voz")
                    }
                    IconButton(onClick = { viewModel.generateSummaryAndSentiment() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "IA Analizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            if (isGeneratingSummary) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            currentPdf?.let { pdf ->
                if (pdf.summary != null || pdf.sentiment != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Análisis de IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                IconButton(onClick = { viewModel.clearSummary() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (pdf.sentiment != null) {
                                Text("Sentimiento: ${pdf.sentiment}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (pdf.summary != null) {
                                Text("Resumen: ${pdf.summary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (!isTextMode) {
                                scale = (scale * zoom).coerceIn(1f, 5f)
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                if (isTextMode) {
                    val scrollState = rememberScrollState()
                    val chunks = remember(pageText) { com.example.utils.TextChunker.parse(pageText) }
                    val currentChunkIndex by viewModel.currentChunkIndex.collectAsState(initial = -1)
                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    
                    val annotatedString = buildAnnotatedString {
                        if (chunks.isEmpty()) {
                            append(pageText.ifEmpty { "Extrayendo texto..." })
                        } else {
                            chunks.forEachIndexed { index, chunk ->
                                val isRead = isPlaying && index < currentChunkIndex
                                val isReading = isPlaying && index == currentChunkIndex
                                val isNext = isPlaying && index == currentChunkIndex + 1
                    
                                val textColor = if (isRead) Color.Gray else contentColor
                                val bgColor = when {
                                    isReading -> Color(0xFFC8E6C9) // Verde Claro
                                    isNext -> Color(0xFFFFF9C4) // Amarillo Claro
                                    else -> Color.Transparent
                                }
                    
                                withStyle(style = SpanStyle(color = textColor, background = bgColor)) {
                                    append(chunk.text)
                                }
                            }
                        }
                    }

                    Text(
                        text = annotatedString,
                        color = contentColor,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.5f).sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                            .pointerInput(chunks, isPlaying, currentChunkIndex) {
                                detectTapGestures { pos ->
                                    textLayoutResult?.let { layoutResult ->
                                        val offset = layoutResult.getOffsetForPosition(pos)
                                        val clickedChunkIndex = chunks.indexOfFirst { offset >= it.start && offset < it.end }
                                        if (clickedChunkIndex != -1) {
                                            viewModel.seekToChunk(clickedChunkIndex)
                                        }
                                    }
                                }
                            },
                        onTextLayout = { textLayoutResult = it }
                    )
                } else {
                    pageBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Página PDF",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                        )
                    }
                }
            }
        }
        
        if (showVoiceSettings) {
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
                        
                    // Si está leyendo, reiniciar con la nueva configuración
                    if (com.example.service.AudioReaderService.isServiceRunning.value) {
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice) // Stop
                        viewModel.toggleAutoRead(newSpeed, newPitch, newEngine, newVoice) // Start
                    }
                }
            )
        }
    }
}

@Composable
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
                    var selectedLanguage by remember { mutableStateOf("") }
                    val availableLanguages = remember(availableVoices) { 
                        availableVoices.map { it.locale.displayName }.distinct().sorted() 
                    }
                    val filteredVoices = remember(availableVoices, selectedLanguage) {
                        if (selectedLanguage.isEmpty()) availableVoices 
                        else availableVoices.filter { it.locale.displayName == selectedLanguage }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Filtrar por Idioma:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var langExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { langExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedLanguage.isEmpty()) "Todos los idiomas" else selectedLanguage)
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.fillMaxHeight(0.5f)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Todos los idiomas") },
                                onClick = { 
                                    selectedLanguage = ""
                                    langExpanded = false 
                                }
                            )
                            availableLanguages.forEach { lang ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = { 
                                        selectedLanguage = lang
                                        langExpanded = false 
                                    }
                                )
                            }
                        }
                    }

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
                            filteredVoices.forEach { v ->
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
}