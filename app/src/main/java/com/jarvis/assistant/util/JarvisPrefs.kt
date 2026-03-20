package com.jarvis.assistant.util

import android.content.Context
import androidx.core.content.edit

object JarvisPrefs {
    private const val PREF_FILE  = "jarvis_prefs"
    private const val KEY_API    = "api_key"
    private const val KEY_WAKE   = "wake_enabled"
    private const val KEY_NAME   = "user_name"
    private const val KEY_SALUTE = "salutation"   // sir / ma'am / name
    private const val KEY_HEALTH = "health_enabled"
    private const val KEY_WATER  = "water_goal_ml"
    private const val KEY_STEPS  = "steps_goal"
    private const val KEY_SLEEP  = "sleep_goal_hrs"
    private const val KEY_WATER_TODAY = "water_today_ml"
    private const val KEY_STEPS_TODAY = "steps_today"
    private const val KEY_WATER_DATE  = "water_date"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun getApiKey(ctx: Context)     = prefs(ctx).getString(KEY_API, "") ?: ""
    fun setApiKey(ctx: Context, k: String) = prefs(ctx).edit { putString(KEY_API, k) }

    fun isWakeEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_WAKE, true)
    fun setWakeEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_WAKE, v) }

    fun getUserName(ctx: Context)   = prefs(ctx).getString(KEY_NAME, "sir") ?: "sir"
    fun setUserName(ctx: Context, n: String) = prefs(ctx).edit { putString(KEY_NAME, n) }

    fun getSalutation(ctx: Context) = prefs(ctx).getString(KEY_SALUTE, "sir") ?: "sir"
    fun setSalutation(ctx: Context, s: String) = prefs(ctx).edit { putString(KEY_SALUTE, s) }

    fun isHealthEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_HEALTH, true)
    fun setHealthEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit { putBoolean(KEY_HEALTH, v) }

    fun getWaterGoal(ctx: Context)  = prefs(ctx).getInt(KEY_WATER, 2500)
    fun setWaterGoal(ctx: Context, ml: Int) = prefs(ctx).edit { putInt(KEY_WATER, ml) }

    fun getStepsGoal(ctx: Context)  = prefs(ctx).getInt(KEY_STEPS, 8000)
    fun setStepsGoal(ctx: Context, s: Int) = prefs(ctx).edit { putInt(KEY_STEPS, s) }

    fun getSleepGoal(ctx: Context)  = prefs(ctx).getFloat(KEY_SLEEP, 8f)
    fun setSleepGoal(ctx: Context, h: Float) = prefs(ctx).edit { putFloat(KEY_SLEEP, h) }

    // Daily water tracking (resets each day)
    fun getWaterToday(ctx: Context): Int {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val saved = prefs(ctx).getString(KEY_WATER_DATE, "") ?: ""
        return if (saved == today) prefs(ctx).getInt(KEY_WATER_TODAY, 0) else 0
    }
    fun addWaterToday(ctx: Context, ml: Int) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val current = getWaterToday(ctx)
        prefs(ctx).edit {
            putString(KEY_WATER_DATE, today)
            putInt(KEY_WATER_TODAY, current + ml)
        }
    }

    fun getStepsToday(ctx: Context) = prefs(ctx).getInt(KEY_STEPS_TODAY, 0)
    fun setStepsToday(ctx: Context, s: Int) = prefs(ctx).edit { putInt(KEY_STEPS_TODAY, s) }
}
