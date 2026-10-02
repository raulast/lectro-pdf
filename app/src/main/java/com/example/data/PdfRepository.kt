package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(
    private val pdfDao: PdfDao,
    private val bookmarkDao: BookmarkDao? = null
) {
    val allPdfs: Flow<List<PdfDocumentEntity>> = pdfDao.getAllPdfs()

    suspend fun getPdfById(id: Int): PdfDocumentEntity? = pdfDao.getPdfById(id)

    suspend fun insertPdf(pdf: PdfDocumentEntity): Long = pdfDao.insertPdf(pdf)

    suspend fun updatePdf(pdf: PdfDocumentEntity) = pdfDao.updatePdf(pdf)

    suspend fun deletePdfById(id: Int) {
        bookmarkDao?.deleteBookmarksForPdf(id)
        pdfDao.deletePdfById(id)
    }

    suspend fun updatePdfOrder(id: Int, order: Int) {
        pdfDao.updateDisplayOrder(id, order)
    }

    // Bookmark operations
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao?.getAllBookmarks() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getBookmarksForPdf(pdfId: Int): Flow<List<BookmarkEntity>> {
        return bookmarkDao?.getBookmarksForPdf(pdfId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun getBookmarkById(id: Int): BookmarkEntity? = bookmarkDao?.getBookmarkById(id)

    suspend fun insertBookmark(bookmark: BookmarkEntity): Long = bookmarkDao?.insertBookmark(bookmark) ?: -1L

    suspend fun updateBookmark(bookmark: BookmarkEntity) {
        bookmarkDao?.updateBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarkDao?.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(id: Int) {
        bookmarkDao?.deleteBookmarkById(id)
    }
}
