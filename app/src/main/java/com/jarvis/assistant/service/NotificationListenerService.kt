package com.jarvis.assistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class AppNotification(
    val app: String,
    val title: String,
    val text: String,
    val time: Long = System.currentTimeMillis()
)

class JarvisNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "JarvisNotifService"
        val recentNotifications = mutableListOf<AppNotification>()
        private const val MAX_STORED = 20
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: return
            val text  = extras.getCharSequence("android.text")?.toString() ?: ""
            val pm    = packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            } catch (e: Exception) { sbn.packageName }

            val notif = AppNotification(appLabel, title, text)
            recentNotifications.add(notif)
            if (recentNotifications.size > MAX_STORED) recentNotifications.removeAt(0)

            Log.d(TAG, "Notification from $appLabel: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) { }
}
