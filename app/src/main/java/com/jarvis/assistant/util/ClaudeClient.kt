package com.jarvis.assistant.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ClaudeClient {

    private const val TAG = "ClaudeClient"
    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL   = "claude-sonnet-4-20250514"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Rolling conversation history (max 20 turns)
    private val history = mutableListOf<Pair<String, String>>()   // role → content

    private val SYSTEM_PROMPT = """
You are JARVIS, the AI personal assistant from Iron Man — witty, precise, fiercely loyal.
You run on the user's Android phone.

CAPABILITIES YOU HAVE:
- Open any installed app
- Web search & browse
- Volume and media controls
- Set alarms and reminders
- Read notifications
- Health tracking (steps, heart rate, hydration, sleep)
- General knowledge and conversation

HEALTH ASSISTANT RULES:
- Track hydration reminders (every 2 hours)
- Encourage healthy habits gently
- If user mentions chest pain, severe symptoms → immediately say "Please call emergency services."
- Provide health tips when asked
- Never diagnose. Always recommend consulting a doctor for medical concerns.

RESPONSE STYLE:
- Keep spoken replies SHORT (1–3 sentences). You're speaking, not writing.
- Be clever and occasionally witty like JARVIS from the films.
- Address the user as "sir" or "ma'am" (use their preference from prefs if set).
- If you cannot do something, say so briefly and suggest an alternative.

When you understand a device action, START your reply with a JSON action block, then your spoken reply:
ACTION:{"type":"open_app","app":"spotify"}
Then say: "Opening Spotify, sir."

Action types: open_app, web_search, set_volume, media_control, set_alarm, open_url
""".trimIndent()

    suspend fun ask(ctx: Context, userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = JarvisPrefs.getApiKey(ctx)
        if (apiKey.isBlank()) {
            return@withContext "API key not configured. Please set your Anthropic key in Settings."
        }

        // Build messages array
        val messages = JSONArray()
        history.takeLast(10).forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userMessage))

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 300)
            put("system", SYSTEM_PROMPT)
            put("messages", messages)
        }

        try {
            val request = Request.Builder()
                .url(API_URL)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .build()

            val response = http.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "API error ${response.code}: $responseBody")
                return@withContext "I'm having trouble connecting to my intelligence core. Error ${response.code}."
            }

            val json = JSONObject(responseBody)
            val rawText = json
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Store in history
            history.add("user" to userMessage)
            history.add("assistant" to rawText)
            if (history.size > 20) {
                history.removeAt(0)
                history.removeAt(0)
            }

            // Extract spoken reply (strip ACTION: block if present)
            val spoken = if (rawText.startsWith("ACTION:")) {
                rawText.substringAfter("\n").trim().ifBlank { "Done." }
            } else rawText

            spoken

        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            "Network error. Please check your connection, sir."
        }
    }

    fun clearHistory() { history.clear() }
}
