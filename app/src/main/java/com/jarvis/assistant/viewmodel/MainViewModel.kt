package com.jarvis.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.service.CommandRouter
import com.jarvis.assistant.util.JarvisPrefs
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,   // "user" | "jarvis" | "system"
    val text: String,
    val time: Long = System.currentTimeMillis()
)

enum class JarvisState {
    IDLE, WAKE_DETECTED, LISTENING, THINKING, SPEAKING, ERROR
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val messages   = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val jarvisState = MutableLiveData(JarvisState.IDLE)
    val wakeActive  = MutableLiveData(true)

    fun addMessage(role: String, text: String) {
        val list = messages.value ?: mutableListOf()
        list.add(ChatMessage(role, text))
        messages.postValue(list)
    }

    fun onWakeDetected() {
        jarvisState.postValue(JarvisState.WAKE_DETECTED)
        addMessage("system", "Wake word detected...")
    }

    fun onCommandReceived(command: String) {
        addMessage("user", command)
        jarvisState.postValue(JarvisState.THINKING)
    }

    fun onReplyReceived(command: String, reply: String) {
        addMessage("jarvis", reply)
        jarvisState.postValue(JarvisState.IDLE)
    }

    fun submitTextCommand(text: String) {
        if (text.isBlank()) return
        addMessage("user", text)
        jarvisState.postValue(JarvisState.THINKING)

        viewModelScope.launch {
            val reply = CommandRouter.route(getApplication(), text)
            addMessage("jarvis", reply)
            jarvisState.postValue(JarvisState.IDLE)
        }
    }
}
