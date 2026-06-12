package com.example.clipboardsync

import android.content.Context
import android.os.Bundle
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clipboardsync.network.ApiClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("ClipboardSyncPrefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("server_ip", "")
        val savedPort = prefs.getInt("server_port", 8766)

        val ipEditText = findViewById<EditText>(R.id.ipEditText)
        val portEditText = findViewById<EditText>(R.id.portEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        ipEditText.setText(savedIp)
        portEditText.setText(savedPort.toString())

        saveButton.setOnClickListener {
            val ip = ipEditText.text.toString().trim()
            val portStr = portEditText.text.toString().trim()
            
            if (ip.isEmpty()) {
                Toast.makeText(this, "Please enter an IP address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portStr.toIntOrNull() ?: 8766

            prefs.edit()
                .putString("server_ip", ip)
                .putInt("server_port", port)
                .apply()

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }

        val sendClipboardButton = findViewById<Button>(R.id.sendClipboardButton)
        val manualEditText = findViewById<EditText>(R.id.manualEditText)
        val sendTextButton = findViewById<Button>(R.id.sendTextButton)

        sendClipboardButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    sendToMac(text)
                } else {
                    Toast.makeText(this, "Clipboard does not contain text", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        sendTextButton.setOnClickListener {
            val text = manualEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendToMac(text)
            } else {
                Toast.makeText(this, "Enter some text first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendToMac(text: String) {
        Toast.makeText(this, "Sending to Mac...", Toast.LENGTH_SHORT).show()
        ApiClient.shareText(this, text) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (success) {
                    Toast.makeText(this, "Successfully sent to Mac!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
