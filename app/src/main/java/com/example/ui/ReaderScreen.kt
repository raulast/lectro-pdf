package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
                    Text(
                        text = pageText.ifEmpty { "Extrayendo texto..." },
                        color = contentColor,
                        fontSize = fontSize,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
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
                
                Spacer(modifier = Modifier.height(24.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
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
                    androidx.compose.material3.Text("Abrir Ajustes del Motor (Sherpa/Piper)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValuesChange(currentSpeed, currentPitch)
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

