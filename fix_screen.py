import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

old_block = """                    Text(
                        text = pageText.ifEmpty { "Extrayendo texto..." },
                        color = contentColor,
                        fontSize = fontSize,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    )"""

new_block = """                    val chunks = remember(pageText) { com.example.utils.TextChunker.parse(pageText) }
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
                    )"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)

