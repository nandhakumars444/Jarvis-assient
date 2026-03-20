package com.jarvis.assistant.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object JarvisTTS : TextToSpeech.OnInitListener {

    private const val TAG = "JarvisTTS"
    private var tts: TextToSpeech? = null
    private var ready = false
    private val queue = mutableListOf<String>()

    fun init(ctx: Context) {
        tts = TextToSpeech(ctx.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(0.9f)   // Slightly lower pitch for a more "synthetic" voice
            ready = true
            // Drain queued messages
            queue.forEach { speak(null, it) }
            queue.clear()
        } else {
            Log.e(TAG, "TTS init failed: $status")
        }
    }

    fun speak(ctx: Context?, text: String) {
        if (!ready) {
            if (ctx != null && tts == null) init(ctx)
            queue.add(text)
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_${System.currentTimeMillis()}")
    }

    fun stop() { tts?.stop() }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
