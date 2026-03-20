package com.jarvis.assistant.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.databinding.ActivitySettingsBinding
import com.jarvis.assistant.util.ClaudeClient
import com.jarvis.assistant.util.JarvisPrefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "JARVIS Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        loadPrefs()
        setupSaveButton()
        setupNotifAccess()
    }

    private fun loadPrefs() {
        binding.etApiKey.setText(JarvisPrefs.getApiKey(this))
        binding.etUserName.setText(JarvisPrefs.getUserName(this))
        binding.etWaterGoal.setText(JarvisPrefs.getWaterGoal(this).toString())
        binding.etStepsGoal.setText(JarvisPrefs.getStepsGoal(this).toString())
        binding.switchWake.isChecked   = JarvisPrefs.isWakeEnabled(this)
        binding.switchHealth.isChecked = JarvisPrefs.isHealthEnabled(this)

        val salutation = JarvisPrefs.getSalutation(this)
        when (salutation) {
            "ma'am" -> binding.rgSalutation.check(binding.rbMaam.id)
            else    -> binding.rgSalutation.check(binding.rbSir.id)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            JarvisPrefs.setApiKey(this, apiKey)
            JarvisPrefs.setUserName(this, binding.etUserName.text.toString().trim())
            JarvisPrefs.setWakeEnabled(this, binding.switchWake.isChecked)
            JarvisPrefs.setHealthEnabled(this, binding.switchHealth.isChecked)

            val waterGoal = binding.etWaterGoal.text.toString().toIntOrNull() ?: 2500
            JarvisPrefs.setWaterGoal(this, waterGoal)

            val stepsGoal = binding.etStepsGoal.text.toString().toIntOrNull() ?: 8000
            JarvisPrefs.setStepsGoal(this, stepsGoal)

            val salutation = if (binding.rbMaam.isChecked) "ma'am" else "sir"
            JarvisPrefs.setSalutation(this, salutation)

            ClaudeClient.clearHistory()
            Toast.makeText(this, "Settings saved, ${salutation}!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupNotifAccess() {
        binding.btnNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
