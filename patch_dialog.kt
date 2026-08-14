@Composable
fun EditCoverDialog(
    pdf: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onSaveGeneric: (String, Int) -> Unit,
    onSaveFromPage: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customTitle by remember { mutableStateOf(pdf.title) }
    var pageInput by remember { mutableStateOf("1") }
    
    val colors = listOf(
        Color(0xFFE8DEF8), // default secondary container
        Color(0xFFFFD8E4), // pink
        Color(0xFFC3E8FF), // blue
        Color(0xFFC2F0C2), // green
        Color(0xFFFFE082), // yellow
        Color(0xFFE0E0E0)  // gray
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Portada") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Genérica") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Página PDF") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Título de la Portada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selecciona un color:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .androidx.compose.foundation.background(color)
                                    .clickable { selectedColor = color }
                                    .androidx.compose.foundation.border(
                                        width = if (selectedColor == color) 2.dp else 0.dp,
                                        color = if (selectedColor == color) Color.Black else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    Text("Escribe el número de página:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Página (1 - ${pdf.totalPages})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0) {
                        onSaveGeneric(customTitle, selectedColor.androidx.compose.ui.graphics.toArgb())
                    } else {
                        val page = pageInput.toIntOrNull() ?: 1
                        onSaveFromPage(page)
                    }
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
