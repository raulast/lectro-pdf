package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val allPdfs: Flow<List<PdfDocumentEntity>> = pdfDao.getAllPdfs()

    suspend fun getPdfById(id: Int): PdfDocumentEntity? = pdfDao.getPdfById(id)

    suspend fun insertPdf(pdf: PdfDocumentEntity): Long = pdfDao.insertPdf(pdf)

    suspend fun updatePdf(pdf: PdfDocumentEntity) = pdfDao.updatePdf(pdf)

    suspend fun deletePdfById(id: Int) = pdfDao.deletePdfById(id)
}
