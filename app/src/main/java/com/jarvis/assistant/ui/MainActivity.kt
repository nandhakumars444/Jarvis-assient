package com.jarvis.assistant.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityMainBinding
import com.jarvis.assistant.service.WakeWordService
import com.jarvis.assistant.util.JarvisPrefs
import com.jarvis.assistant.util.JarvisTTS
import com.jarvis.assistant.viewmodel.JarvisState
import com.jarvis.assistant.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WakeWordService.ACTION_WAKE_DETECTED -> vm.onWakeDetected()
                WakeWordService.ACTION_COMMAND_RESULT -> {
                    val cmd   = intent.getStringExtra(WakeWordService.EXTRA_COMMAND) ?: ""
                    val reply = intent.getStringExtra(WakeWordService.EXTRA_REPLY)   ?: ""
                    vm.onCommandReceived(cmd)
                    vm.onReplyReceived(cmd, reply)
                }
            }
        }
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            startWakeService()
        } else {
            vm.addMessage("system", "Microphone permission denied. Voice features disabled.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        JarvisTTS.init(this)
        setupUI()
        setupObservers()
        checkPermissionsAndStart()

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(WakeWordService.ACTION_WAKE_DETECTED)
            addAction(WakeWordService.ACTION_COMMAND_RESULT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wakeReceiver)
        JarvisTTS.shutdown()
    }

    private fun setupUI() {
        // Chat RecyclerView
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                it.stackFromEnd = true
            }
        }

        // Arc reactor tap → manual mic
        binding.arcReactor.setOnClickListener {
            if (vm.jarvisState.value == JarvisState.IDLE ||
                vm.jarvisState.value == JarvisState.ERROR) {
                triggerManualListen()
            }
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            vm.submitTextCommand(text)
            binding.etInput.setText("")
        }

        // Enter key on keyboard
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.btnSend.performClick()
                true
            } else false
        }

        // Toolbar actions
        binding.btnHealth.setOnClickListener {
            startActivity(Intent(this, HealthActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Wake toggle
        binding.toggleWake.setOnCheckedChangeListener { _, checked ->
            JarvisPrefs.setWakeEnabled(this, checked)
            vm.wakeActive.value = checked
            if (checked) startWakeService() else stopService(Intent(this, WakeWordService::class.java))
        }
    }

    private fun setupObservers() {
        vm.messages.observe(this) { msgs ->
            chatAdapter.submitList(msgs.toList())
            binding.rvChat.scrollToPosition(msgs.size - 1)
        }

        vm.jarvisState.observe(this) { state ->
            updateArcState(state)
        }

        vm.wakeActive.observe(this) { active ->
            binding.toggleWake.isChecked = active
            binding.tvWakeStatus.text = if (active) "Say \"Hey Jarvis\"" else "Wake word off"
        }
    }

    private fun updateArcState(state: JarvisState) {
        when (state) {
            JarvisState.IDLE -> {
                binding.arcReactor.state = ArcReactorView.State.IDLE
                binding.waveform.visibility = View.INVISIBLE
            }
            JarvisState.WAKE_DETECTED, JarvisState.LISTENING -> {
                binding.arcReactor.state = ArcReactorView.State.LISTENING
                binding.waveform.visibility = View.VISIBLE
            }
            JarvisState.THINKING, JarvisState.SPEAKING -> {
                binding.arcReactor.state = ArcReactorView.State.THINKING
                binding.waveform.visibility = View.INVISIBLE
            }
            JarvisState.ERROR -> {
                binding.arcReactor.state = ArcReactorView.State.IDLE
                binding.waveform.visibility = View.INVISIBLE
            }
        }
    }

    private fun triggerManualListen() {
        // Start a one-shot speech recogniser intent
        val intent = Intent(this, WakeWordService::class.java)
        intent.putExtra("manual_trigger", true)
        startService(intent)
    }

    private fun checkPermissionsAndStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startWakeService()
        } else {
            permLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startWakeService() {
        if (!JarvisPrefs.isWakeEnabled(this)) return
        val intent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, intent)
        vm.wakeActive.value = true
    }
}
