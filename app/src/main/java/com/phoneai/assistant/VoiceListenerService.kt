package com.phoneai.assistant

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceListenerService : Service(), TextToSpeech.OnInitListener {

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "myra_voice_channel"
        private const val NOTIF_ID = 1
        private val WAKE_WORDS = listOf("myra", "मायरा", "माइरा", "मिरा")
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var awaitingCommand = false
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        tts = TextToSpeech(this, this)
        startForeground(NOTIF_ID, buildNotification("Myra सुन रही है..."))
        startListeningLoop()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.voices?.firstOrNull {
                it.name.contains("female", ignoreCase = true) && !it.isNetworkConnectionRequired
            }?.let { tts?.voice = it }
        }
    }

    private fun speak(text: String) {
        CommandLog.add("🗣️ Myra: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startListeningLoop() {
        if (isListening) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.lowercase() ?: ""

                handleHeardText(text)
                restartListening()
            }

            override fun onError(error: Int) {
                isListening = false
                restartListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        recognizer?.startListening(intent)
    }

    private fun restartListening() {
        recognizer?.destroy()
        recognizer = null
        android.os.Handler(mainLooper).postDelayed({
            if (isRunning) startListeningLoop()
        }, 400)
    }

    private fun handleHeardText(text: String) {
        if (text.isBlank()) return

        val wakeWordFound = WAKE_WORDS.any { text.contains(it) }

        if (!awaitingCommand) {
            if (wakeWordFound) {
                var remainder = text
                WAKE_WORDS.forEach { remainder = remainder.replace(it, "").trim() }
                if (remainder.isNotBlank()) {
                    speak("जी बताइए")
                    executeSpokenCommand(remainder)
                } else {
                    speak("जी बोलिए")
                    awaitingCommand = true
                }
            }
        } else {
            awaitingCommand = false
            executeSpokenCommand(text)
        }
    }

    private fun executeSpokenCommand(rawText: String) {
        val service = PhoneControlAccessibilityService.instance
        if (service == null) {
            speak("पहले Accessibility Service ऑन करें")
            return
        }

        val cmd = when {
            rawText.contains("खोलो") || rawText.contains("open") -> rawText
            rawText.contains("लिखो") || rawText.contains("type") -> rawText
            rawText.contains("क्लिक") || rawText.contains("click") -> rawText
            rawText.contains("होम") || rawText.contains("home") -> "होम"
            rawText.contains("बैक") || rawText.contains("back") -> "बैक"
            else -> "खोलो $rawText"
        }

        service.executeCommand(cmd)
        speak("हो गया")
    }

    private fun buildNotification(text: String): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Myra Voice", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Myra AI")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        CommandLog.add("🛑 Myra सुनना बंद हो गई")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
