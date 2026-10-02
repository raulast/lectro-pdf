package com.example.utils

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val code: String, val displayNameRes: String) {
    SYSTEM("system", "Predeterminado del sistema / System Default"),
    SPANISH("es", "Español"),
    ENGLISH("en", "English")
}

object LanguageManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "app_language"

    private val _currentLanguage = MutableStateFlow("system")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        _currentLanguage.value = savedLang
    }

    fun setLanguage(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
        _currentLanguage.value = langCode
    }

    fun getEffectiveLanguageCode(): String {
        val selected = _currentLanguage.value
        return if (selected == "system") {
            val systemLocale = Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
            if (systemLocale.language.startsWith("es")) "es" else "en"
        } else {
            selected
        }
    }
}

val LocalAppLanguage = compositionLocalOf { "system" }

/**
 * Multi-language dictionary providing instantaneous language switching
 * across all screens, dialogs, and components in the application.
 */
object Strings {
    private val translations = mapOf(
        // App General
        "app_name" to mapOf("es" to "Lector PDF IA", "en" to "AI PDF Reader"),
        "library" to mapOf("es" to "Biblioteca", "en" to "Library"),
        "bookmarks" to mapOf("es" to "Marcadores", "en" to "Bookmarks"),
        "settings" to mapOf("es" to "Configuración", "en" to "Settings"),
        "save" to mapOf("es" to "Guardar", "en" to "Save"),
        "cancel" to mapOf("es" to "Cancelar", "en" to "Cancel"),
        "delete" to mapOf("es" to "Eliminar", "en" to "Delete"),
        "edit" to mapOf("es" to "Editar", "en" to "Edit"),
        "close" to mapOf("es" to "Cerrar", "en" to "Close"),
        "search" to mapOf("es" to "Buscar...", "en" to "Search..."),
        "search_bookmarks" to mapOf("es" to "Buscar marcadores o notas...", "en" to "Search bookmarks or notes..."),
        "no_results" to mapOf("es" to "No se encontraron resultados", "en" to "No results found"),
        
        // Home Screen
        "add_pdf" to mapOf("es" to "Añadir PDF", "en" to "Add PDF"),
        "empty_library" to mapOf("es" to "Añade tu primer PDF pulsando el botón '+'", "en" to "Add your first PDF by tapping the '+' button"),
        "view_cards" to mapOf("es" to "Vista Tarjetas", "en" to "Cards View"),
        "view_list" to mapOf("es" to "Vista Lista", "en" to "List View"),
        "rescan_cover" to mapOf("es" to "Re-escanear portada", "en" to "Rescan cover"),
        "edit_cover" to mapOf("es" to "Editar portada manual", "en" to "Edit cover manually"),
        "delete_pdf" to mapOf("es" to "Eliminar PDF", "en" to "Delete PDF"),
        "delete_confirm_title" to mapOf("es" to "Eliminar libro", "en" to "Delete book"),
        "delete_confirm_msg" to mapOf("es" to "¿Seguro que deseas eliminar este libro y sus marcadores?", "en" to "Are you sure you want to delete this book and its bookmarks?"),
        "order_books" to mapOf("es" to "Organizar libros", "en" to "Organize books"),
        "move_up" to mapOf("es" to "Subir", "en" to "Move Up"),
        "move_down" to mapOf("es" to "Bajar", "en" to "Move Down"),
        "page_indicator" to mapOf("es" to "Pág %1\$d / %2\$d", "en" to "Page %1\$d / %2\$d"),
        
        // Reader Screen
        "reading" to mapOf("es" to "Leyendo", "en" to "Reading"),
        "back" to mapOf("es" to "Volver", "en" to "Back"),
        "night_mode" to mapOf("es" to "Modo Noche", "en" to "Night Mode"),
        "switch_mode" to mapOf("es" to "Cambiar Modo", "en" to "Switch Mode"),
        "font_increase" to mapOf("es" to "Aumentar Fuente", "en" to "Increase Font"),
        "font_decrease" to mapOf("es" to "Disminuir Fuente", "en" to "Decrease Font"),
        "prev_page" to mapOf("es" to "Anterior", "en" to "Previous"),
        "next_page" to mapOf("es" to "Siguiente", "en" to "Next"),
        "read_aloud" to mapOf("es" to "Leer en voz alta", "en" to "Read aloud"),
        "stop_reading" to mapOf("es" to "Detener lectura", "en" to "Stop reading"),
        "voice_settings" to mapOf("es" to "Configurar Voz", "en" to "Voice Settings"),
        "ai_analysis" to mapOf("es" to "Análisis de IA", "en" to "AI Analysis"),
        "extracting_text" to mapOf("es" to "Extrayendo texto...", "en" to "Extracting text..."),
        "split_double_pages" to mapOf("es" to "2 páginas por hoja (dividido)", "en" to "2 pages per sheet (split)"),
        "full_sheet" to mapOf("es" to "Hoja completa", "en" to "Full sheet"),
        "left_page_indicator" to mapOf("es" to "Izq", "en" to "Left"),
        "right_page_indicator" to mapOf("es" to "Der", "en" to "Right"),
        
        // Jump to Page
        "jump_to_page" to mapOf("es" to "Saltar a página", "en" to "Jump to page"),
        "jump_prompt" to mapOf("es" to "Escribe el número de página al que deseas ir (1 a %1\$d):", "en" to "Enter the page number you want to jump to (1 to %1\$d):"),
        "page_number" to mapOf("es" to "Número de página", "en" to "Page number"),
        "invalid_page" to mapOf("es" to "Por favor introduce un número de página válido.", "en" to "Please enter a valid page number."),
        "jump" to mapOf("es" to "Ir a la página", "en" to "Go to page"),
        "first_page" to mapOf("es" to "Inicio (1)", "en" to "First (1)"),
        "last_page" to mapOf("es" to "Final (%1\$d)", "en" to "Last (%1\$d)"),
        
        // Bookmarks
        "add_bookmark" to mapOf("es" to "Añadir Marcador", "en" to "Add Bookmark"),
        "manage_bookmarks" to mapOf("es" to "Administrar Marcadores", "en" to "Manage Bookmarks"),
        "book_bookmarks" to mapOf("es" to "Marcadores de este libro", "en" to "Bookmarks of this book"),
        "bookmark_title" to mapOf("es" to "Título del marcador", "en" to "Bookmark title"),
        "bookmark_title_hint" to mapOf("es" to "Ej. Concepto clave, Capítulo...", "en" to "e.g. Key concept, Chapter..."),
        "highlighted_text" to mapOf("es" to "Texto marcado / Cita", "en" to "Highlighted text / Quote"),
        "highlighted_text_hint" to mapOf("es" to "Fragmento de texto relevante...", "en" to "Relevant text fragment..."),
        "written_note" to mapOf("es" to "Nota personal escrita", "en" to "Written personal note"),
        "written_note_hint" to mapOf("es" to "Escribe tus reflexiones o anotaciones...", "en" to "Write your thoughts or annotations..."),
        "tag_color" to mapOf("es" to "Color de etiqueta", "en" to "Tag color"),
        "no_bookmarks_book" to mapOf("es" to "Aún no tienes marcadores en este libro. Pulsa el icono de marcador para crear uno.", "en" to "No bookmarks in this book yet. Tap the bookmark icon to create one."),
        "no_bookmarks_all" to mapOf("es" to "No tienes marcadores guardados todavía.", "en" to "You have no saved bookmarks yet."),
        "delete_bookmark_confirm" to mapOf("es" to "¿Eliminar este marcador?", "en" to "Delete this bookmark?"),
        "bookmark_created" to mapOf("es" to "Marcador guardado con éxito", "en" to "Bookmark saved successfully"),
        "bookmark_deleted" to mapOf("es" to "Marcador eliminado", "en" to "Bookmark deleted"),
        "go_to_page" to mapOf("es" to "Ir a esta página", "en" to "Go to this page"),
        "filter_all_books" to mapOf("es" to "Todos los libros", "en" to "All books"),
        "notes" to mapOf("es" to "Notas", "en" to "Notes"),
        "quote" to mapOf("es" to "Cita", "en" to "Quote"),
        
        // Settings Screen
        "settings_title" to mapOf("es" to "Configuración", "en" to "Settings"),
        "general_section" to mapOf("es" to "General y Apariencia", "en" to "General & Appearance"),
        "language_option" to mapOf("es" to "Idioma de la aplicación", "en" to "Application Language"),
        "system_default" to mapOf("es" to "Predeterminado del sistema", "en" to "System Default"),
        "spanish" to mapOf("es" to "Español", "en" to "Spanish"),
        "english" to mapOf("es" to "English", "en" to "English"),
        "view_mode_option" to mapOf("es" to "Vista de biblioteca", "en" to "Library View Mode"),
        "reading_tts_section" to mapOf("es" to "Lectura y Voz (TTS)", "en" to "Reading & Voice (TTS)"),
        "tts_speed" to mapOf("es" to "Velocidad de voz", "en" to "Voice speed"),
        "tts_pitch" to mapOf("es" to "Tono de voz", "en" to "Voice pitch"),
        "tts_engine" to mapOf("es" to "Motor de voz TTS", "en" to "TTS Voice Engine"),
        "data_storage_section" to mapOf("es" to "Almacenamiento y Datos", "en" to "Storage & Data"),
        "total_books" to mapOf("es" to "Libros en biblioteca", "en" to "Books in library"),
        "total_bookmarks" to mapOf("es" to "Marcadores guardados", "en" to "Saved bookmarks"),
        "about_section" to mapOf("es" to "Acerca de la Aplicación", "en" to "About Application"),
        "app_description" to mapOf("es" to "Lector de PDF avanzado con soporte a hojas dobles, marcadores, síntesis de voz y resúmenes con IA.", "en" to "Advanced PDF Reader with dual-page support, bookmarks, speech synthesis, and AI summaries."),
        "version" to mapOf("es" to "Versión", "en" to "Version"),
        
        // Edit Cover Dialog
        "edit_cover_title" to mapOf("es" to "Editar Portada", "en" to "Edit Cover"),
        "tab_generic" to mapOf("es" to "Genérica", "en" to "Generic"),
        "tab_pdf_page" to mapOf("es" to "Página PDF", "en" to "PDF Page"),
        "cover_title_label" to mapOf("es" to "Título de la Portada", "en" to "Cover Title"),
        "select_color" to mapOf("es" to "Selecciona un color:", "en" to "Select a color:"),
        "enter_page_number" to mapOf("es" to "Escribe el número de página:", "en" to "Enter page number:"),
        "page_range" to mapOf("es" to "Página (1 - %1\$d)", "en" to "Page (1 - %1\$d)")
    )

    fun get(key: String, vararg args: Any): String {
        val lang = LanguageManager.getEffectiveLanguageCode()
        val entry = translations[key]
        val template = entry?.get(lang) ?: entry?.get("es") ?: key
        return if (args.isNotEmpty()) {
            try {
                String.format(template, *args)
            } catch (e: Exception) {
                template
            }
        } else {
            template
        }
    }
}

@Composable
fun t(key: String, vararg args: Any): String {
    // Observing currentLanguage allows automatic recomposition when language changes
    val lang = LanguageManager.currentLanguage
    return Strings.get(key, *args)
}
