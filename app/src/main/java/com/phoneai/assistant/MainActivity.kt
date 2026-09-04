package com.phoneai.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phoneai.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val logBuilder = StringBuilder()

    private val logListener: (String) -> Unit = { message ->
        runOnUiThread {
            logBuilder.insert(0, "$message\n")
            binding.logText.text = logBuilder.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CommandLog.addListener(logListener)

        binding.enableServiceButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "\"PhoneAI\" ढूंढें और उसे ON करें", Toast.LENGTH_LONG).show()
        }

        binding.sendCommandButton.setOnClickListener {
            val command = binding.commandInput.text.toString()
            if (TextUtils.isEmpty(command)) {
                Toast.makeText(this, "पहले कोई कमांड लिखें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val service = PhoneControlAccessibilityService.instance
            if (service == null) {
                Toast.makeText(this, "पहले Accessibility Service ON करें", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            service.executeCommand(command)
            binding.commandInput.setText("")
        }

        binding.voiceToggleButton.setOnClickListener {
            if (VoiceListenerService.isRunning) {
                stopService(Intent(this, VoiceListenerService::class.java))
            } else {
                if (checkMicPermission()) {
                    startVoiceService()
                }
            }
            updateServiceStatus()
        }

        updateServiceStatus()
    }

    private fun checkMicPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return false
        }
        return true
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Myra सुनना शुरू — बोलें: Myra, WhatsApp खोलो", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceService()
            updateServiceStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isAccessOn = PhoneControlAccessibilityService.instance != null
        binding.statusText.text = if (isAccessOn) "स्टेटस: AI चालू है ✅" else "स्टेटस: AI बंद है — नीचे बटन दबाकर ON करें"

        binding.voiceToggleButton.text = if (VoiceListenerService.isRunning) {
            "🛑 Myra को सुनना बंद करें"
        } else {
            "🎤 Myra को जगाओ (हमेशा सुनेगी)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CommandLog.removeListener(logListener)
    }
}
