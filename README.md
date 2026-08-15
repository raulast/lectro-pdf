# Lector de Audiolibros AI (Android)

Una aplicación nativa de Android diseñada para transformar documentos PDF en audiolibros fluidos. Construida con **Kotlin** y **Jetpack Compose**, esta aplicación incluye integración de Text-To-Speech (TTS) nativa con soporte para motores personalizados (como Sherpa-ONNX o Google TTS), progreso de lectura interactivo sincronizado y resúmenes impulsados por la API de Gemini.

## 🚀 Características Principales

*   **Lector de PDF Local:** Explora y abre documentos PDF desde el almacenamiento de tu dispositivo.
*   **Motor de Audio Híbrido (TTS):** 
    *   Soporte para múltiples motores (Google, Sherpa-ONNX).
    *   Selección dinámica de "voces específicas" por motor.
    *   Lectura en 2º plano con controles multimedia en la barra de notificaciones.
    *   Algoritmo de *Goteo Inteligente* que inserta pausas naturales basadas en la puntuación (, . ; \n).
*   **Interfaz de Lectura Dinámica (Karaoke-style):**
    *   Modo lectura pura (sin imágenes) con control de tamaño de fuente.
    *   Sombreado dinámico de palabras: Gris (leído), Verde (leyendo), Amarillo (por leer).
    *   Navegación táctil: Toca cualquier frase para saltar la lectura a esa parte.
*   **Inteligencia Artificial (Gemini):**
    *   Botón de análisis de página: Genera un resumen (3 oraciones máximo) y detecta el sentimiento/tono del texto.

## 🛠 Entorno de Desarrollo y Requisitos

Para compilar y ejecutar este proyecto localmente, asegúrate de cumplir con los siguientes requisitos en tu máquina:

### 1. Requisitos del Sistema
*   **Java Development Kit (JDK):** Versión 17 (Requerido por AGP 8+).
*   **Android Studio:** Jellyfish | 2023.3.1 o superior.
*   **Android SDK:** 
    *   Min SDK: 24 (Android 7.0)
    *   Target SDK: 34 (Android 14)

### 2. Variables de Entorno / Credenciales (Gemini API)
La aplicación utiliza la IA de Gemini para los resúmenes. Para compilarla correctamente:

1. Crea un archivo llamado `local.properties` (o `.env` si estás en entornos automatizados/CI) en el directorio raíz del proyecto.
2. Obtén una API Key gratuita en [Google AI Studio](https://aistudio.google.com/).
3. Añade tu API Key al archivo con el siguiente formato:
   ```properties
   GEMINI_API_KEY="AIzaSyTuClaveSecretaAqui..."
   ```
*(Nota: El sistema de compilación inyectará esta clave de forma segura en `BuildConfig.GEMINI_API_KEY`).*

## 📦 Compilación y Ejecución (CLI)

Si prefieres usar la terminal en lugar de Android Studio, puedes compilar la aplicación usando Gradle Wrapper:

**1. Clonar el repositorio y dar permisos al wrapper:**
```bash
git clone <url-del-repositorio>
cd <nombre-del-proyecto>
chmod +x gradlew
```

**2. Compilar el APK de depuración (Debug):**
```bash
./gradlew assembleDebug
```
*El APK generado estará en: `app/build/outputs/apk/debug/app-debug.apk`*

**3. Instalar en un dispositivo conectado (o emulador):**
```bash
./gradlew installDebug
```

**4. Ejecutar pruebas unitarias locales:**
```bash
./gradlew testDebugUnitTest
```

## 🏗 Arquitectura y Librerías

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** y utiliza las siguientes librerías de Jetpack/AndroidX:

*   **UI:** Jetpack Compose (Material 3).
*   **Navegación:** Compose Navigation.
*   **Base de Datos Local:** Room Database (almacena el progreso de lectura, metadatos y resúmenes de los PDFs).
*   **Corrutinas y Flujos:** `kotlinx.coroutines` para manejo asíncrono y reactividad.
*   **PDF:** `android.graphics.pdf.PdfRenderer` para renderizado nativo.
*   **Red:** Retrofit2 y OkHttp3 para las llamadas a la API de Gemini.
*   **Procesamiento de Voz:** `android.speech.tts.TextToSpeech`.

## 📜 Licencia

Este proyecto es de uso personal/educativo. Las voces y modelos generados por motores TTS externos (como Piper o Sherpa) están sujetos a sus propias licencias de uso.
