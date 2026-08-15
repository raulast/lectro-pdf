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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AudioReaderService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isPlaying = false
    private var isTtsReady = false
    private var pendingText: String? = null
    private var pendingStartIndex: Int = 0
    private var lastUtteranceId: String? = null
    private var currentSpeed: Float = 1.0f
    private var currentPitch: Float = 1.0f
    private var currentEngine: String? = null
    private var currentVoiceName: String? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TEXT = "EXTRA_TEXT"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_PITCH = "EXTRA_PITCH"
        const val EXTRA_ENGINE = "EXTRA_ENGINE"
        const val EXTRA_VOICE_NAME = "EXTRA_VOICE_NAME"
        const val EXTRA_START_INDEX = "EXTRA_START_INDEX"
        const val CHANNEL_ID = "AudioReaderChannel"
        const val NOTIFICATION_ID = 1

        
        private val _pageFinishedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
        val pageFinishedEvent = _pageFinishedEvent.asSharedFlow()
        
        private val _isServiceRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isServiceRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        val _currentChunkIndex = kotlinx.coroutines.flow.MutableStateFlow(-1)
        val currentChunkIndex: kotlinx.coroutines.flow.StateFlow<Int> = _currentChunkIndex.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                currentPitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f)
                val requestedEngine = intent.getStringExtra(EXTRA_ENGINE) ?: ""
                val requestedVoice = intent.getStringExtra(EXTRA_VOICE_NAME) ?: ""
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                
                currentVoiceName = requestedVoice

                startForegroundService(text)

                val engineChanged = (tts == null) || (currentEngine != requestedEngine)
                if (engineChanged) {
                    try {
                        tts?.stop()
                        tts?.shutdown()
                    } catch (e: Exception) {}
                    
                    tts = null
                    isTtsReady = false
                    currentEngine = requestedEngine
                    pendingText = text
                    pendingStartIndex = startIndex
                    
                    tts = if (requestedEngine.isNotEmpty()) {
                        TextToSpeech(this, this, requestedEngine)
                    } else {
                        TextToSpeech(this, this)
                    }
                } else {
                    applyVoiceAndSettings()
                    if (isTtsReady) {
                        speakText(text, startIndex)
                    } else {
                        pendingText = text
                        pendingStartIndex = startIndex
                    }
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
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                if (!currentVoiceName.isNullOrEmpty()) {
                    for (i in 0..15) { // Wait up to 3 seconds for voices to load
                        val voices = tts?.voices
                        if (!voices.isNullOrEmpty()) {
                            val selectedVoice = voices.find { it.name == currentVoiceName }
                            if (selectedVoice != null) {
                                tts?.voice = selectedVoice
                            }
                            break
                        }
                        kotlinx.coroutines.delay(200)
                    }
                }
                
                isTtsReady = true
                pendingText?.let {
                    speakText(it, pendingStartIndex)
                    pendingText = null
                }
            }
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlaying = true
                    if (utteranceId?.startsWith("CHUNK_") == true) {
                        val idx = utteranceId.substringAfter("CHUNK_").toIntOrNull()
                        if (idx != null) {
                            _currentChunkIndex.value = idx
                        }
                    }
                }
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                        _pageFinishedEvent.tryEmit(Unit)
                        _currentChunkIndex.value = -1
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == lastUtteranceId) {
                        isPlaying = false
                        _pageFinishedEvent.tryEmit(Unit)
                        _currentChunkIndex.value = -1
                    }
                }
            })
        }
    }

    private fun applyVoiceAndSettings() {
        tts?.let { engine ->
            engine.setSpeechRate(currentSpeed)
            engine.setPitch(currentPitch)
            if (!currentVoiceName.isNullOrEmpty()) {
                try {
                    val voices = engine.voices
                    val selectedVoice = voices?.find { it.name == currentVoiceName }
                    if (selectedVoice != null) {
                        engine.voice = selectedVoice
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun speakText(text: String, startIndex: Int) {
        if (tts != null && text.isNotEmpty()) {
            tts?.stop()
            applyVoiceAndSettings()
            _currentChunkIndex.value = startIndex

            val chunks = com.example.utils.TextChunker.parse(text)
            
            var lastId: String? = null
            var queuedCount = 0
            
            for (i in startIndex until chunks.size) {
                val chunk = chunks[i]
                if (chunk.isSpeakable) {
                    val queueMode = if (queuedCount == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    val currentId = "CHUNK_$i"
                    lastId = currentId
                    tts?.speak(chunk.text.trim(), queueMode, null, currentId)
                    
                    // Delay calculation based on punctuation
                    val t = chunk.text
                    val delay = when {
                        t.contains("\n") -> 800L
                        t.contains(".") || t.contains("!") || t.contains("?") -> 500L
                        t.contains(";") -> 300L
                        t.contains(",") -> 150L
                        else -> 0L
                    }
                    if (delay > 0) {
                        tts?.playSilentUtterance(delay, TextToSpeech.QUEUE_ADD, "SILENCE_$i")
                    }
                    queuedCount++
                }
            }
            
            lastUtteranceId = lastId

            if (queuedCount == 0) {
                 isPlaying = false
                 _pageFinishedEvent.tryEmit(Unit)
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
