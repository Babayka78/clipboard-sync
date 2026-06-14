package com.example.clipboardsync

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clipboardsync.network.ApiClient

class TileActionActivity : AppCompatActivity() {

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout — this activity is transparent.
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

        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val text = clip.getItemAt(0).text?.toString()
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, "Clipboard does not contain text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Pre-flight ping
        Toast.makeText(this, "Checking server...", Toast.LENGTH_SHORT).show()
        ApiClient.checkServerAlive(this) { alive, error ->
            runOnUiThread {
                if (alive) {
                    showCpFileDialog(text)
                } else {
                    showPingErrorDialog(error ?: "Unknown error")
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showCpFileDialog(text: String) {
        AlertDialog.Builder(this)
            .setTitle("Send Clipboard to Mac")
            .setItems(arrayOf("CP  (copy to clipboard)", "File  (save to Documents)")) { _, which ->
                val action = if (which == 0) "clipboard" else "save"
                sendClipboardText(text, action)
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun showPingErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Mac Unreachable")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    private fun sendClipboardText(text: String, action: String) {
        if (action == "clipboard") {
            Toast.makeText(this, "Copying to Mac clipboard...", Toast.LENGTH_SHORT).show()
            ApiClient.shareText(this, text) { success, error ->
                showResult(success, error)
            }
        } else {
            // Save as a .txt file
            Toast.makeText(this, "Saving as file on Mac...", Toast.LENGTH_SHORT).show()
            val txtFile = try {
                val f = java.io.File(cacheDir, "clipboard_${System.currentTimeMillis()}.txt")
                java.io.FileOutputStream(f).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                f
            } catch (e: Exception) {
                showResult(false, "Failed to create text file: ${e.message}")
                return
            }
            val fileUri = android.net.Uri.fromFile(txtFile)
            ApiClient.shareFile(this, fileUri, "save") { success, error ->
                txtFile.delete()
                showResult(success, error)
            }
        }
    }

    private fun showResult(success: Boolean, error: String?) {
        Handler(Looper.getMainLooper()).post {
            if (success) {
                Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed: $error", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}
