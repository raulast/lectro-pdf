package com.example

import com.example.data.BookmarkEntity
import com.example.utils.LanguageManager
import com.example.utils.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BookmarkAndFeaturesTest {

    @Test
    fun testBookmarkCreation() {
        val bookmark = BookmarkEntity(
            id = 1,
            pdfId = 10,
            bookTitle = "Libro de Prueba",
            pageIndex = 4,
            pageNumber = 5,
            title = "Capítulo 1",
            highlightedText = "Este es un fragmento de prueba",
            note = "Nota importante para examen",
            color = 0xFFFFD54F.toInt()
        )

        assertEquals(1, bookmark.id)
        assertEquals(10, bookmark.pdfId)
        assertEquals(4, bookmark.pageIndex)
        assertEquals(5, bookmark.pageNumber)
        assertEquals("Capítulo 1", bookmark.title)
        assertEquals("Este es un fragmento de prueba", bookmark.highlightedText)
        assertEquals("Nota importante para examen", bookmark.note)
    }

    @Test
    fun testStringsDictionary() {
        val appNameEs = Strings.get("app_name")
        assertNotNull(appNameEs)

        val jumpPrompt = Strings.get("jump_prompt", 42)
        assertNotNull(jumpPrompt)
    }
}
