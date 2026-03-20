package com.jarvis.assistant.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.MainActivity
import com.jarvis.assistant.util.JarvisPrefs
import kotlinx.coroutines.*

class WakeWordService : Service() {

    companion object {
        const val TAG = "WakeWordService"
        const val ACTION_WAKE_DETECTED  = "com.jarvis.WAKE_DETECTED"
        const val ACTION_COMMAND_RESULT = "com.jarvis.COMMAND_RESULT"
        const val EXTRA_COMMAND         = "command_text"
        const val EXTRA_REPLY           = "reply_text"

        // Wake word variants (case-insensitive, checked as substrings)
        val WAKE_WORDS = listOf(
            "hey jarvis", "okay jarvis", "ok jarvis", "jarvis"
        )
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isListeningForWake = false
    private var isProcessingCommand = false
    private var restartDelay = 300L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WakeWordService started")
        startWakeLoop()
        return START_STICKY  // Restart automatically if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        speechRecognizer?.destroy()
    }

    // ── Foreground notification ────────────────────────────────────────────────

    private fun startForegroundNotification() {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, JarvisApp.CHANNEL_WAKE)
            .setContentTitle("JARVIS Active")
            .setContentText("Say \"Hey Jarvis\" to activate")
            .setSmallIcon(R.drawable.ic_jarvis_small)
            .setOngoing(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    // ── Wake-word recognition loop ─────────────────────────────────────────────

    private fun startWakeLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }
        isListeningForWake = true
        listenForWakeWord()
    }

    private fun listenForWakeWord() {
        if (!isListeningForWake || isProcessingCommand) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: android.os.Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return
                val text = matches.firstOrNull()?.lowercase() ?: return
                Log.d(TAG, "Wake scan heard: $text")

                val matchedWake = WAKE_WORDS.firstOrNull { text.contains(it) }
                if (matchedWake != null) {
                    val inline = text.substringAfter(matchedWake).trim(' ', ',', '.')
                    onWakeWordDetected(inline)
                } else {
                    // Not a wake word – keep scanning
                    scope.launch {
                        delay(restartDelay)
                        listenForWakeWord()
                    }
                }
            }

            override fun onError(error: Int) {
                // Errors 7 (no match) and 6 (timeout) are normal – just restart
                scope.launch {
                    delay(restartDelay)
                    listenForWakeWord()
                }
            }

            // Unused callbacks
            override fun onReadyForSpeech(p0: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: android.os.Bundle?) {}
            override fun onEvent(p0: Int, p0a: android.os.Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun onWakeWordDetected(inlineCommand: String) {
        Log.d(TAG, "Wake word detected! Inline: '$inlineCommand'")
        isProcessingCommand = true

        // Broadcast to UI
        sendBroadcast(Intent(ACTION_WAKE_DETECTED))

        if (inlineCommand.length > 2) {
            // Command was in the same breath as wake word
            processVoiceCommand(inlineCommand)
        } else {
            // Listen for a follow-up command
            listenForCommand()
        }
    }

    private fun listenForCommand() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: android.os.Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    processVoiceCommand(text)
                } else {
                    resetToWakeLoop()
                }
            }

            override fun onError(error: Int) {
                Log.w(TAG, "Command listen error: $error")
                resetToWakeLoop()
            }

            override fun onReadyForSpeech(p0: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: android.os.Bundle?) {}
            override fun onEvent(p0: Int, p0a: android.os.Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processVoiceCommand(command: String) {
        Log.d(TAG, "Processing command: $command")
        scope.launch {
            val reply = CommandRouter.route(applicationContext, command)
            broadcastResult(command, reply)
            resetToWakeLoop()
        }
    }

    private fun broadcastResult(command: String, reply: String) {
        sendBroadcast(Intent(ACTION_COMMAND_RESULT).apply {
            putExtra(EXTRA_COMMAND, command)
            putExtra(EXTRA_REPLY, reply)
        })
    }

    private fun resetToWakeLoop() {
        isProcessingCommand = false
        scope.launch {
            delay(500)
            listenForWakeWord()
        }
    }
}
