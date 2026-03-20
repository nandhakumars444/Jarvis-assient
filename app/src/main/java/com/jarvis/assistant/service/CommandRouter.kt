package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.jarvis.assistant.util.ClaudeClient
import com.jarvis.assistant.util.JarvisPrefs
import com.jarvis.assistant.util.JarvisTTS
import java.util.Calendar

/**
 * Central command dispatcher.
 * First checks for direct device commands (fast, offline).
 * Falls back to Claude AI for natural language / unknown requests.
 */
object CommandRouter {

    private const val TAG = "CommandRouter"

    suspend fun route(ctx: Context, rawCommand: String): String {
        val cmd = rawCommand.lowercase().trim()
        Log.d(TAG, "Routing: $cmd")

        // ── Direct device commands (no API needed) ───────────────────────────
        val directReply = handleDirect(ctx, cmd)
        if (directReply != null) {
            JarvisTTS.speak(ctx, directReply)
            return directReply
        }

        // ── Claude AI for everything else ────────────────────────────────────
        val aiReply = ClaudeClient.ask(ctx, rawCommand)
        JarvisTTS.speak(ctx, aiReply)
        return aiReply
    }

    private fun handleDirect(ctx: Context, cmd: String): String? {
        return when {
            // ── Volume ───────────────────────────────────────────────────────
            cmd.contains("volume") -> handleVolume(ctx, cmd)

            // ── Media ────────────────────────────────────────────────────────
            cmd.contains("play") || cmd.contains("pause") ||
            cmd.contains("next song") || cmd.contains("previous song") ||
            cmd.contains("skip") -> handleMedia(ctx, cmd)

            // ── Alarm / Reminder ─────────────────────────────────────────────
            cmd.contains("alarm") || cmd.contains("remind") ||
            cmd.contains("wake me") || cmd.contains("set timer") -> handleAlarm(ctx, cmd)

            // ── App launch ───────────────────────────────────────────────────
            cmd.startsWith("open ") || cmd.startsWith("launch ") ||
            cmd.startsWith("start ") -> handleOpenApp(ctx, cmd)

            // ── Web search ───────────────────────────────────────────────────
            cmd.startsWith("search") || cmd.startsWith("google") ||
            cmd.startsWith("look up") || cmd.startsWith("find") -> handleWebSearch(ctx, cmd)

            // ── Notifications ─────────────────────────────────────────────────
            cmd.contains("notification") || cmd.contains("read my messages") ||
            cmd.contains("what did i miss") -> handleNotifications(ctx)

            // ── Settings ─────────────────────────────────────────────────────
            cmd.contains("wifi") || cmd.contains("bluetooth") ||
            cmd.contains("airplane mode") || cmd.contains("brightness") -> handleSettings(ctx, cmd)

            // ── Time / Date ──────────────────────────────────────────────────
            cmd == "what time is it" || cmd.contains("what's the time") ||
            cmd.contains("current time") -> {
                val cal = Calendar.getInstance()
                "It's ${cal.get(Calendar.HOUR_OF_DAY)}:${"%02d".format(cal.get(Calendar.MINUTE))}, sir."
            }

            cmd.contains("what day") || cmd.contains("today's date") -> {
                val days = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
                val months = arrayOf("January","February","March","April","May","June",
                    "July","August","September","October","November","December")
                val cal = Calendar.getInstance()
                "${days[cal.get(Calendar.DAY_OF_WEEK)-1]}, ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
            }

            // ── Greetings ────────────────────────────────────────────────────
            cmd == "hello" || cmd == "hi" || cmd == "hey" ->
                "Hello! How may I assist you today, sir?"

            cmd.contains("how are you") || cmd.contains("are you okay") ->
                "All systems are operating at peak efficiency. Thank you for asking."

            cmd.contains("thank") ->
                "You're most welcome, sir. Always at your service."

            cmd.contains("good morning") -> "Good morning, sir. Systems are online and ready."
            cmd.contains("good night")   -> "Good night, sir. Rest well."

            else -> null  // Defer to Claude
        }
    }

    // ── Volume handler ─────────────────────────────────────────────────────────
    private fun handleVolume(ctx: Context, cmd: String): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when {
            cmd.contains("mute") || cmd.contains("silence") -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                "Muted, sir."
            }
            cmd.contains("unmute") || cmd.contains("unmute") -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                "Audio restored."
            }
            cmd.contains("max") || cmd.contains("full") -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
                "Volume set to maximum."
            }
            cmd.contains("low") || cmd.contains("quiet") -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, max / 4, AudioManager.FLAG_SHOW_UI)
                "Volume lowered."
            }
            cmd.contains("up") || cmd.contains("increase") || cmd.contains("louder") -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "Volume increased."
            }
            cmd.contains("down") || cmd.contains("decrease") || cmd.contains("quieter") -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "Volume decreased."
            }
            else -> {
                // Extract percentage like "set volume to 50"
                val pct = Regex("(\\d+)").find(cmd)?.value?.toIntOrNull()
                if (pct != null) {
                    val level = (pct.coerceIn(0, 100) * max / 100)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, level, AudioManager.FLAG_SHOW_UI)
                    "Volume set to $pct percent."
                } else {
                    "I can raise, lower, mute, or set volume to a specific percentage."
                }
            }
        }
    }

    // ── Media handler ──────────────────────────────────────────────────────────
    private fun handleMedia(ctx: Context, cmd: String): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when {
            cmd.contains("next") || cmd.contains("skip") -> {
                dispatchMediaKey(ctx, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                "Playing next track."
            }
            cmd.contains("previous") || cmd.contains("back") -> {
                dispatchMediaKey(ctx, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                "Going back."
            }
            cmd.contains("pause") || cmd.contains("stop") -> {
                dispatchMediaKey(ctx, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                "Paused."
            }
            else -> {
                dispatchMediaKey(ctx, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                "Playing."
            }
        }
    }

    private fun dispatchMediaKey(ctx: Context, keyCode: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
    }

    // ── Alarm handler ──────────────────────────────────────────────────────────
    private fun handleAlarm(ctx: Context, cmd: String): String {
        val hour   = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(cmd)
        val hourVal = hour?.groupValues?.get(1)?.toIntOrNull() ?: 7
        val minVal  = hour?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val ampm    = hour?.groupValues?.get(3)?.lowercase()
        val h24 = when {
            ampm == "pm" && hourVal < 12 -> hourVal + 12
            ampm == "am" && hourVal == 12 -> 0
            else -> hourVal
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h24)
            putExtra(AlarmClock.EXTRA_MINUTES, minVal)
            putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
        return "Alarm set for $hourVal:${"%02d".format(minVal)}${ampm?.let { " $it" } ?: ""}, sir."
    }

    // ── App launcher ───────────────────────────────────────────────────────────
    private fun handleOpenApp(ctx: Context, cmd: String): String {
        val appName = cmd
            .removePrefix("open ").removePrefix("launch ").removePrefix("start ")
            .trim()
        val pm = ctx.packageManager
        val intent = pm.getLaunchIntentForPackage(
            findPackage(ctx, appName) ?: return "I couldn't find $appName installed."
        ) ?: return "Unable to launch $appName."
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return "Opening $appName."
    }

    private fun findPackage(ctx: Context, appName: String): String? {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.firstOrNull {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
        }?.packageName
    }

    // ── Web search ─────────────────────────────────────────────────────────────
    private fun handleWebSearch(ctx: Context, cmd: String): String {
        val query = cmd
            .removePrefix("search for ").removePrefix("search ")
            .removePrefix("google ").removePrefix("look up ")
            .removePrefix("find ").trim()
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
        return "Searching for $query."
    }

    // ── Notifications ──────────────────────────────────────────────────────────
    private fun handleNotifications(ctx: Context): String {
        val recent = JarvisNotificationService.recentNotifications
        return if (recent.isEmpty()) {
            "No recent notifications, sir."
        } else {
            val summary = recent.takeLast(3).joinToString(". ") { "${it.app}: ${it.text}" }
            "Recent notifications: $summary"
        }
    }

    // ── Settings ───────────────────────────────────────────────────────────────
    private fun handleSettings(ctx: Context, cmd: String): String {
        val intent = when {
            cmd.contains("wifi")      -> Intent(Settings.ACTION_WIFI_SETTINGS)
            cmd.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            cmd.contains("brightness")-> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        ctx.startActivity(intent)
        return "Opening settings."
    }
}
