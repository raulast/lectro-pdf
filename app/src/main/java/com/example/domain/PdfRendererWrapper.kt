package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

class PdfRendererWrapper(private val context: Context, private val uri: Uri) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    init {
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun renderPage(pageIndex: Int, width: Int = 1000): Bitmap? {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return null

        return try {
            val page = pdfRenderer!!.openPage(pageIndex)
            val height = (width.toFloat() / page.width * page.height).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Fill with white background
            bitmap.eraseColor(android.graphics.Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun saveCoverImage(pageIndex: Int = 0): String? {
        val bitmap = renderPage(pageIndex, 400) ?: return null
        return try {
            val file = File(context.filesDir, "cover_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
