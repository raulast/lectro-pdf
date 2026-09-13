package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

enum class PageSplit {
    FULL,
    LEFT,
    RIGHT
}

data class VirtualPage(
    val pdfIndex: Int,
    val split: PageSplit
)

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

    fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return null
        return try {
            val page = pdfRenderer!!.openPage(pageIndex)
            val w = page.width
            val h = page.height
            page.close()
            Pair(w, h)
        } catch (e: Exception) {
            null
        }
    }

    fun isPageWide(pageIndex: Int): Boolean {
        val dims = getPageDimensions(pageIndex) ?: return false
        return dims.first.toFloat() / dims.second.toFloat() > 1.15f
    }

    fun hasAnyWidePages(): Boolean {
        if (pdfRenderer == null) return false
        val count = pageCount
        val sampleSize = minOf(15, count)
        for (i in 0 until sampleSize) {
            if (isPageWide(i)) return true
        }
        return false
    }

    fun buildVirtualPages(splitDouble: Boolean): List<VirtualPage> {
        val list = mutableListOf<VirtualPage>()
        val count = pageCount
        for (i in 0 until count) {
            if (splitDouble && isPageWide(i)) {
                list.add(VirtualPage(pdfIndex = i, split = PageSplit.LEFT))
                list.add(VirtualPage(pdfIndex = i, split = PageSplit.RIGHT))
            } else {
                list.add(VirtualPage(pdfIndex = i, split = PageSplit.FULL))
            }
        }
        return list
    }

    fun renderVirtualPage(virtualPage: VirtualPage, targetWidth: Int = 1200): Bitmap? {
        if (pdfRenderer == null || virtualPage.pdfIndex < 0 || virtualPage.pdfIndex >= pageCount) return null

        return try {
            val page = pdfRenderer!!.openPage(virtualPage.pdfIndex)
            when (virtualPage.split) {
                PageSplit.FULL -> {
                    val height = (targetWidth.toFloat() / page.width * page.height).toInt()
                    val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap
                }
                PageSplit.LEFT, PageSplit.RIGHT -> {
                    val fullWidth = targetWidth * 2
                    val fullHeight = (fullWidth.toFloat() / page.width * page.height).toInt()
                    val fullBitmap = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888)
                    fullBitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(fullBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val halfWidth = fullWidth / 2
                    val croppedBitmap = if (virtualPage.split == PageSplit.LEFT) {
                        Bitmap.createBitmap(fullBitmap, 0, 0, halfWidth, fullHeight)
                    } else {
                        Bitmap.createBitmap(fullBitmap, halfWidth, 0, fullWidth - halfWidth, fullHeight)
                    }
                    fullBitmap.recycle()
                    croppedBitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renderPage(pageIndex: Int, width: Int = 1000): Bitmap? {
        return renderVirtualPage(VirtualPage(pageIndex, PageSplit.FULL), width)
    }
    
    fun saveCoverImage(pageIndex: Int = 0): String? {
        val isWide = isPageWide(pageIndex)
        val vp = if (isWide) VirtualPage(pageIndex, PageSplit.RIGHT) else VirtualPage(pageIndex, PageSplit.FULL)
        val bitmap = renderVirtualPage(vp, 600) ?: return null
        return try {
            val file = File(context.filesDir, "cover_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            bitmap.recycle()
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
