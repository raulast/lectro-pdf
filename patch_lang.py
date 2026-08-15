import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

old_block = """                if (availableVoices.isNotEmpty()) {
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
                }"""

new_block = """                if (availableVoices.isNotEmpty()) {
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
                }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

