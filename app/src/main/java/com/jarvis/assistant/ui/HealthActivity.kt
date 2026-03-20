package com.jarvis.assistant.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityHealthBinding
import com.jarvis.assistant.util.JarvisPrefs
import com.jarvis.assistant.util.JarvisTTS

class HealthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Health Dashboard"
            setDisplayHomeAsUpEnabled(true)
        }

        refreshUI()
        setupButtons()
    }

    private fun refreshUI() {
        val waterToday  = JarvisPrefs.getWaterToday(this)
        val waterGoal   = JarvisPrefs.getWaterGoal(this)
        val stepsToday  = JarvisPrefs.getStepsToday(this)
        val stepsGoal   = JarvisPrefs.getStepsGoal(this)
        val sleepGoal   = JarvisPrefs.getSleepGoal(this)

        binding.tvWaterAmount.text  = "${waterToday} ml / ${waterGoal} ml"
        binding.pbWater.max         = waterGoal
        binding.pbWater.progress    = waterToday

        binding.tvSteps.text        = "$stepsToday / $stepsGoal steps"
        binding.pbSteps.max         = stepsGoal
        binding.pbSteps.progress    = stepsToday

        binding.tvSleepGoal.text    = "Sleep goal: ${sleepGoal}h"

        // Health score (simple composite)
        val waterPct = (waterToday.toFloat() / waterGoal * 100).coerceIn(0f, 100f)
        val stepsPct = (stepsToday.toFloat() / stepsGoal * 100).coerceIn(0f, 100f)
        val score    = ((waterPct + stepsPct) / 2).toInt()
        binding.tvHealthScore.text  = "Health Score: $score / 100"
        binding.pbHealth.progress   = score

        val tip = getHealthTip(waterPct.toInt(), stepsPct.toInt())
        binding.tvHealthTip.text = tip
    }

    private fun setupButtons() {
        // Log water intake buttons
        binding.btn250ml.setOnClickListener  { logWater(250) }
        binding.btn500ml.setOnClickListener  { logWater(500) }
        binding.btn750ml.setOnClickListener  { logWater(750) }
        binding.btnCustomMl.setOnClickListener {
            val ml = binding.etCustomMl.text.toString().toIntOrNull() ?: 0
            if (ml > 0) logWater(ml)
        }

        binding.btnHealthVoice.setOnClickListener {
            val water = JarvisPrefs.getWaterToday(this)
            val goal  = JarvisPrefs.getWaterGoal(this)
            val remaining = (goal - water).coerceAtLeast(0)
            val msg = if (remaining > 0)
                "You've had ${water} millilitres of water today. You need ${remaining} more to hit your goal, sir."
            else
                "Excellent hydration today, sir. Goal achieved!"
            JarvisTTS.speak(this, msg)
        }
    }

    private fun logWater(ml: Int) {
        JarvisPrefs.addWaterToday(this, ml)
        JarvisTTS.speak(this, "Logged ${ml} millilitres. Keep it up!")
        refreshUI()
    }

    private fun getHealthTip(waterPct: Int, stepsPct: Int): String = when {
        waterPct < 30  -> "💧 Tip: You're dehydrated. Drink a glass of water now."
        stepsPct < 30  -> "🚶 Tip: Take a short walk — even 10 minutes helps your heart."
        waterPct < 60  -> "💧 Tip: Halfway to your water goal. Keep sipping!"
        stepsPct < 60  -> "🚶 Tip: Great start on steps — keep moving!"
        else           -> "✅ You're doing great today. Keep up the healthy habits!"
    }
}
