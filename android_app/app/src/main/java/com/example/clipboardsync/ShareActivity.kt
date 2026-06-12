package com.example.clipboardsync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clipboardsync.network.ApiClient

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContentView, we want this to be transparent and finish quickly

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent)
            } else if (type.startsWith("image/")) {
                handleSendImage(intent)
            } else {
                Toast.makeText(this, "Unsupported share type", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (Intent.ACTION_PROCESS_TEXT == action) {
            handleProcessText(intent)
        } else {
            finish()
        }
    }

    private fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            sendTextToMac(sharedText)
        } else {
            finish()
        }
    }

    private fun handleProcessText(intent: Intent) {
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (sharedText != null) {
            sendTextToMac(sharedText)
        } else {
            finish()
        }
    }

    private fun sendTextToMac(text: String) {
        Toast.makeText(this, "Sending text to Mac...", Toast.LENGTH_SHORT).show()
        ApiClient.shareText(this, text) { success, error ->
            showResult(success, error)
        }
    }

    private fun handleSendImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        if (imageUri != null) {
            Toast.makeText(this, "Sending image to Mac...", Toast.LENGTH_SHORT).show()
            ApiClient.shareImage(this, imageUri) { success, error ->
                showResult(success, error)
            }
        } else {
            finish()
        }
    }

    private fun showResult(success: Boolean, error: String?) {
        Handler(Looper.getMainLooper()).post {
            if (success) {
                Toast.makeText(this, "Successfully sent to Mac!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to send: $error", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}
