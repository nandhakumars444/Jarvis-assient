package com.jarvis.assistant.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.util.JarvisPrefs

class HydrationReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val NOTIF_ID = 42
        private const val REQ_CODE = 1001

        /** Call once on app start or settings save to schedule 2-hourly reminders. */
        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = PendingIntent.getBroadcast(
                ctx, REQ_CODE,
                Intent(ctx, HydrationReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            try {
                am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, 2 * 60 * 60 * 1000L, intent)
            } catch (e: SecurityException) {
                // Exact alarms require permission on Android 12+; fallback to inexact
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, 2 * 60 * 60 * 1000L, intent)
            }
        }

        fun cancel(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = PendingIntent.getBroadcast(
                ctx, REQ_CODE,
                Intent(ctx, HydrationReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            am.cancel(intent)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent?) {
        val water = JarvisPrefs.getWaterToday(ctx)
        val goal  = JarvisPrefs.getWaterGoal(ctx)
        if (water >= goal) return   // Already hit goal — no need to nag

        val remaining = goal - water
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(ctx, JarvisApp.CHANNEL_NOTIF)
            .setSmallIcon(R.drawable.ic_jarvis_small)
            .setContentTitle("💧 Hydration Reminder")
            .setContentText("You need ${remaining}ml more to hit your goal today, sir.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }
}
