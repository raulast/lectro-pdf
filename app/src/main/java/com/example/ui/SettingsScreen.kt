package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.AppLanguage
import com.example.utils.LanguageManager
import com.example.utils.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    totalBooks: Int,
    totalBookmarks: Int,
    currentViewMode: String,
    onViewModeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLangCode by LanguageManager.currentLanguage.collectAsState()
    val scrollState = rememberScrollState()

    val prefs = remember { context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE) }
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("voice_speed", 1.0f)) }
    var voicePitch by remember { mutableFloatStateOf(prefs.getFloat("voice_pitch", 1.0f)) }
    var voiceEngine by remember { mutableStateOf(prefs.getString("voice_engine", "") ?: "") }
    var voiceName by remember { mutableStateOf(prefs.getString("voice_name", "") ?: "") }

    var ttsEngines by remember { mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList()) }
    var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    var isGoogleEngine by remember { mutableStateOf(false) }

    // Load TTS Engines and Voices
    LaunchedEffect(voiceEngine) {
        try {
            val tts = if (voiceEngine.isNotBlank()) {
                TextToSpeech(context, {}, voiceEngine)
            } else {
                TextToSpeech(context) {}
            }
            ttsEngines = tts.engines ?: emptyList()
            isGoogleEngine = voiceEngine.contains("google", ignoreCase = true) || 
                             (voiceEngine.isBlank() && (tts.defaultEngine?.contains("google", ignoreCase = true) == true))
            try {
                availableVoices = tts.voices?.toList() ?: emptyList()
            } catch (e: Exception) {
                availableVoices = emptyList()
            }
            tts.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.get("settings_title")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.get("back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: General & Appearance
            Text(
                text = Strings.get("general_section"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Language setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Strings.get("language_option"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                val currentLangDisplay = when (currentLangCode) {
                                    "es" -> Strings.get("spanish")
                                    "en" -> Strings.get("english")
                                    else -> Strings.get("system_default")
                                }
                                Text(currentLangDisplay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // View Mode setting
                    Text(
                        text = Strings.get("view_mode_option"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = currentViewMode == "grid",
                            onClick = { onViewModeChanged("grid") },
                            label = { Text(Strings.get("view_cards")) },
                            leadingIcon = { Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentViewMode == "list",
                            onClick = { onViewModeChanged("list") },
                            label = { Text(Strings.get("view_list")) },
                            leadingIcon = { Icon(Icons.Filled.ViewList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: Bookmarks Management
            Text(
                text = Strings.get("bookmarks"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToBookmarks)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.get("manage_bookmarks"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$totalBookmarks ${Strings.get("total_bookmarks").lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }

            // Section 3: Voice / TTS Settings
            Text(
                text = Strings.get("reading_tts_section"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Voice Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.get("tts_speed"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(String.format("%.2fx", voiceSpeed), style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = voiceSpeed,
                        onValueChange = {
                            voiceSpeed = it
                            prefs.edit().putFloat("voice_speed", it).apply()
                        },
                        valueRange = 0.5f..2.5f,
                        steps = 7
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.get("tts_pitch"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(String.format("%.2fx", voicePitch), style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = voicePitch,
                        onValueChange = {
                            voicePitch = it
                            prefs.edit().putFloat("voice_pitch", it).apply()
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )

                    if (ttsEngines.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(Strings.get("tts_engine"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        var engineMenuExpanded by remember { mutableStateOf(false) }
                        val currentEngineLabel = ttsEngines.find { it.name == voiceEngine }?.label ?: "Predeterminado"
                        OutlinedButton(
                            onClick = { engineMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentEngineLabel)
                        }
                        DropdownMenu(
                            expanded = engineMenuExpanded,
                            onDismissRequest = { engineMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Predeterminado") },
                                onClick = {
                                    voiceEngine = ""
                                    voiceName = ""
                                    prefs.edit().putString("voice_engine", "").putString("voice_name", "").apply()
                                    engineMenuExpanded = false
                                }
                            )
                            ttsEngines.forEach { eng ->
                                DropdownMenuItem(
                                    text = { Text(eng.label) },
                                    onClick = {
                                        voiceEngine = eng.name
                                        voiceName = ""
                                        prefs.edit().putString("voice_engine", eng.name).putString("voice_name", "").apply()
                                        engineMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Data & Statistics
            Text(
                text = Strings.get("data_storage_section"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.get("total_books"), style = MaterialTheme.typography.bodyMedium)
                        Text("$totalBooks", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.get("total_bookmarks"), style = MaterialTheme.typography.bodyMedium)
                        Text("$totalBookmarks", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 5: About App
            Text(
                text = Strings.get("about_section"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.get("app_name"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.get("app_description"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Strings.get("version")}: 1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(Strings.get("language_option")) },
            text = {
                Column {
                    listOf(
                        "system" to Strings.get("system_default"),
                        "es" to Strings.get("spanish"),
                        "en" to Strings.get("english")
                    ).forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguageManager.setLanguage(context, code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLangCode == code,
                                onClick = {
                                    LanguageManager.setLanguage(context, code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }
}
