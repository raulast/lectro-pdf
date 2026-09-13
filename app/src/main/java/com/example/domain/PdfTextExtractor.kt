package com.example.domain

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object PdfTextExtractor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            // Margen del 8% superior e inferior donde usualmente van headers (títulos) y footers (números de página)
            val topMargin = bitmapHeight * 0.08
            val bottomMargin = bitmapHeight * 0.92

            // Filtrar encabezados y pies de página antes de ordenar
            val validBlocks = result.textBlocks.filter { block ->
                val rect = block.boundingBox
                if (rect != null) {
                    val isHeader = rect.bottom < topMargin
                    val isFooter = rect.top > bottomMargin
                    !(isHeader || isFooter)
                } else {
                    true
                }
            }

            val isWideSpread = (bitmapWidth.toFloat() / bitmapHeight.toFloat()) > 1.1f
            val orderedBlocks = if (isWideSpread) {
                // Si la imagen es más ancha que alta (spread de 2 páginas por hoja),
                // leer primero toda la página izquierda (de arriba a abajo) y luego toda la derecha
                val midX = bitmapWidth / 2
                val leftBlocks = validBlocks.filter { (it.boundingBox?.centerX() ?: 0) < midX }
                    .sortedBy { it.boundingBox?.top ?: 0 }
                val rightBlocks = validBlocks.filter { (it.boundingBox?.centerX() ?: 0) >= midX }
                    .sortedBy { it.boundingBox?.top ?: 0 }
                leftBlocks + rightBlocks
            } else {
                // Verificar si la página individual tiene dos columnas de texto
                val midX = bitmapWidth / 2
                val gutter = bitmapWidth * 0.04f
                val leftCol = validBlocks.filter { (it.boundingBox?.right ?: 0) < (midX + gutter) }
                val rightCol = validBlocks.filter { (it.boundingBox?.left ?: 0) > (midX - gutter) }

                if (validBlocks.size >= 4 && (leftCol.size + rightCol.size) >= (validBlocks.size * 0.85)) {
                    val leftSorted = leftCol.sortedBy { it.boundingBox?.top ?: 0 }
                    val rightSorted = rightCol.sortedBy { it.boundingBox?.top ?: 0 }
                    val others = validBlocks.filter { it !in leftCol && it !in rightCol }
                        .sortedBy { it.boundingBox?.top ?: 0 }
                    leftSorted + rightSorted + others
                } else {
                    validBlocks.sortedBy { it.boundingBox?.top ?: 0 }
                }
            }
            
            val builder = StringBuilder()
            
            for (block in orderedBlocks) {
                
                // Procesar el texto dentro del bloque
                var blockText = block.text
                // 1. Eliminar palabras cortadas por guion al final de la línea
                blockText = blockText.replace(Regex("-\\n"), "")
                // 2. Reemplazar todos los demás saltos de línea internos por espacios, 
                // ya que dentro de un bloque de ML Kit todo pertenece al mismo párrafo continuo.
                blockText = blockText.replace(Regex("\\n"), " ")
                // 3. Reducir múltiples espacios a uno solo
                blockText = blockText.replace(Regex(" {2,}"), " ")
                
                builder.append(blockText.trim())
                builder.append("\n\n") // Separar físicamente los párrafos procesados
            }
            
            builder.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

}
