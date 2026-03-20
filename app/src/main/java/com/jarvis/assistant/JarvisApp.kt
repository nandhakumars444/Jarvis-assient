package com.jarvis.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class JarvisApp : Application() {

    companion object {
        const val CHANNEL_WAKE  = "jarvis_wake"
        const val CHANNEL_NOTIF = "jarvis_notify"
        lateinit var instance: JarvisApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_WAKE, "JARVIS Wake Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-on wake-word detection"
                setShowBadge(false)
            })

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_NOTIF, "JARVIS Responses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "JARVIS replies and alerts"
            })
        }
    }
}
