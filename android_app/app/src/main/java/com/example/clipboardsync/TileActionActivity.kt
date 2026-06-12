package com.example.clipboardsync

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clipboardsync.network.ApiClient

class TileActionActivity : AppCompatActivity() {

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We have no UI layout because this activity is transparent.
        // We must wait for the window to gain focus before reading the clipboard.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !handled) {
            handled = true
            processClipboard()
        }
    }

    private fun processClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty()) {
                Toast.makeText(this, "Sending clipboard to Mac...", Toast.LENGTH_SHORT).show()
                ApiClient.shareText(this, text) { success, error ->
                    Handler(Looper.getMainLooper()).post {
                        if (success) {
                            Toast.makeText(this, "Successfully sent to Mac!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send: $error", Toast.LENGTH_LONG).show()
                        }
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Clipboard does not contain text", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
