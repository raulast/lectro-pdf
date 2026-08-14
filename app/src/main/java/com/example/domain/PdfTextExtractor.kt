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
            
            val bitmapHeight = bitmap.height
            // Margen del 8% superior e inferior donde usualmente van headers (títulos) y footers (números de página)
            val topMargin = bitmapHeight * 0.08
            val bottomMargin = bitmapHeight * 0.92
            
            val builder = StringBuilder()
            
            // Ordenar los bloques de texto de arriba a abajo
            val sortedBlocks = result.textBlocks.sortedBy { it.boundingBox?.top ?: 0 }
            
            for (block in sortedBlocks) {
                val rect = block.boundingBox
                if (rect != null) {
                    val isHeader = rect.bottom < topMargin
                    val isFooter = rect.top > bottomMargin
                    if (isHeader || isFooter) {
                        continue // Ignorar este bloque porque es un encabezado o pie de página
                    }
                }
                
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
