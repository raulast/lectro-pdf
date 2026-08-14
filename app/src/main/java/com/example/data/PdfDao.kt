package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdfs ORDER BY id DESC")
    fun getAllPdfs(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdfs WHERE id = :id")
    suspend fun getPdfById(id: Int): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfDocumentEntity): Long

    @Update
    suspend fun updatePdf(pdf: PdfDocumentEntity)

    @Query("DELETE FROM pdfs WHERE id = :id")
    suspend fun deletePdfById(id: Int)
}
