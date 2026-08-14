package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioReaderService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isPlaying = false
    private var isTtsReady = false
    private var pendingText: String? = null
    private var lastUtteranceId: String? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TEXT = "EXTRA_TEXT"
        const val CHANNEL_ID = "AudioReaderChannel"
        const val NOTIFICATION_ID = 1

        private val _isServiceRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isServiceRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                startForegroundService(text)
                if (isTtsReady) {
                    speakText(text)
                } else {
                    pendingText = text
                }
            }
            ACTION_STOP -> {
                stopPlayback()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService(text: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Leyendo Documento")
            .setContentText("Reproduciendo audiolibro en segundo plano")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        _isServiceRunning.value = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audiolibro PDF",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("es", "ES")
            tts?.language = locale
            
            // Restablecemos el pitch y la velocidad a los valores por defecto
            // para que motores neuronales de terceros (como Piper) funcionen sin distorsiones
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            
            isTtsReady = true
            pendingText?.let {
                speakText(it)
                pendingText = null
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlaying = true
                }
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                        stopSelf()
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                    }
                }
            })
        }
    }

    private fun speakText(text: String) {
        if (tts != null && text.isNotEmpty()) {
            tts?.stop()
            
            // Dividir el texto en bloques lógicos por puntos, exclamaciones, interrogaciones o saltos de línea
            // Esto ayuda enormemente a los motores neuronales a procesar la prosodia natural
            val chunks = text.split(Regex("(?<=[.?!:])\\s+|\\n+"))
            
            var chunkIndex = 0
            for (chunk in chunks) {
                val cleanChunk = chunk.trim()
                if (cleanChunk.isNotEmpty()) {
                    val queueMode = if (chunkIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    val currentId = "TTS_ID_${System.currentTimeMillis()}_$chunkIndex"
                    lastUtteranceId = currentId
                    tts?.speak(cleanChunk, queueMode, null, currentId)
                    chunkIndex++
                }
            }
            
            if (chunkIndex == 0) {
                 isPlaying = false
                 stopSelf()
            }
        }
    }

    private fun stopPlayback() {
        tts?.stop()
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
