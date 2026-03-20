package com.jarvis.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jarvis.assistant.util.JarvisPrefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (JarvisPrefs.isWakeEnabled(ctx)) {
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, WakeWordService::class.java)
                )
            }
        }
    }
}
