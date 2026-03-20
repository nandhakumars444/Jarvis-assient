package com.jarvis.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.jarvis.assistant.util.JarvisPrefs

data class HealthData(
    val waterToday: Int,
    val waterGoal: Int,
    val stepsToday: Int,
    val stepsGoal: Int,
    val sleepGoal: Float,
    val score: Int
)

class HealthViewModel(app: Application) : AndroidViewModel(app) {

    val healthData = MutableLiveData<HealthData>()

    fun refresh() {
        val ctx = getApplication<Application>()
        val waterToday = JarvisPrefs.getWaterToday(ctx)
        val waterGoal  = JarvisPrefs.getWaterGoal(ctx)
        val stepsToday = JarvisPrefs.getStepsToday(ctx)
        val stepsGoal  = JarvisPrefs.getStepsGoal(ctx)
        val sleepGoal  = JarvisPrefs.getSleepGoal(ctx)

        val waterPct = (waterToday.toFloat() / waterGoal * 100).coerceIn(0f, 100f)
        val stepsPct = (stepsToday.toFloat() / stepsGoal * 100).coerceIn(0f, 100f)
        val score    = ((waterPct + stepsPct) / 2).toInt()

        healthData.postValue(HealthData(waterToday, waterGoal, stepsToday, stepsGoal, sleepGoal, score))
    }

    fun addWater(ml: Int) {
        JarvisPrefs.addWaterToday(getApplication(), ml)
        refresh()
    }
}
