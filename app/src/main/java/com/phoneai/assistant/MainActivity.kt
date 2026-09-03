package com.phoneai.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            Toast.makeText(this, "\"PhoneAI\"    ON ", Toast.LENGTH_LONG).show()
        }

        binding.sendCommandButton.setOnClickListener {
            val command = binding.commandInput.text.toString()
            if (TextUtils.isEmpty(command)) {
                Toast.makeText(this, "   ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val service = PhoneControlAccessibilityService.instance
            if (service == null) {
                Toast.makeText(this, " Accessibility Service ON  (   )", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            service.executeCommand(command)
            binding.commandInput.setText("")
        }

        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isOn = PhoneControlAccessibilityService.instance != null
        binding.statusText.text = if (isOn) ": AI   " else ": AI       ON "
    }

    override fun onDestroy() {
        super.onDestroy()
        CommandLog.removeListener(logListener)
    }
}
