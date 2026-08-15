import re

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

imports = """import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextLayoutResult
"""
if "buildAnnotatedString" not in content:
    content = content.replace("import androidx.compose.ui.text.font.FontWeight\n", "import androidx.compose.ui.text.font.FontWeight\n" + imports)

# Find the Text displaying pageText
old_text_block = """                    Text(
                        text = pageText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp,
                        modifier = Modifier.fillMaxSize()
                    )"""

new_text_block = """                    val chunks = remember(pageText) { com.example.utils.TextChunker.parse(pageText) }
                    val currentChunkIndex by viewModel.currentChunkIndex.collectAsState()
                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    
                    val annotatedString = buildAnnotatedString {
                        chunks.forEachIndexed { index, chunk ->
                            val isRead = isPlaying && index < currentChunkIndex
                            val isReading = isPlaying && index == currentChunkIndex
                            val isNext = isPlaying && index == currentChunkIndex + 1
                
                            val textColor = if (isRead) Color.Gray else MaterialTheme.colorScheme.onBackground
                            val bgColor = when {
                                isReading -> Color(0xFFC8E6C9)
                                isNext -> Color(0xFFFFF9C4)
                                else -> Color.Transparent
                            }
                
                            withStyle(style = SpanStyle(color = textColor, background = bgColor)) {
                                append(chunk.text)
                            }
                        }
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxSize()
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

content = content.replace(old_text_block, new_text_block)

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)
