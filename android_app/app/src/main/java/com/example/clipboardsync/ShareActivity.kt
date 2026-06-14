package com.example.clipboardsync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clipboardsync.network.ApiClient
import java.io.File
import java.io.FileOutputStream

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout — this activity is transparent and finishes after sending.

        val action = intent.action
        val type = intent.type

        when {
            Intent.ACTION_SEND == action && type != null -> handleSend(intent, type)
            Intent.ACTION_PROCESS_TEXT == action -> handleProcessText(intent)
            else -> finish()
        }
    }

    // ── Entry points ─────────────────────────────────────────────────────────

    private fun handleSend(intent: Intent, type: String) {
        if (type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                pingThenShowDialog { chosenAction -> dispatchText(text, chosenAction) }
            } else {
                finish()
            }
        } else {
            // Any non-text type: treat as a file
            val fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            if (fileUri != null) {
                pingThenShowDialog { chosenAction -> dispatchFile(fileUri, chosenAction) }
            } else {
                showError("No file attached to the share intent.")
            }
        }
    }

    private fun handleProcessText(intent: Intent) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (text != null) {
            // PROCESS_TEXT always copies to clipboard — no dialog needed
            pingThenSendText(text)
        } else {
            finish()
        }
    }

    // ── Pre-flight ping ───────────────────────────────────────────────────────

    /**
     * Pings the server; on success shows the CP / File dialog and calls [onAction].
     * On failure shows a detailed AlertDialog explaining what went wrong.
     */
    private fun pingThenShowDialog(onAction: (String) -> Unit) {
        Toast.makeText(this, "Checking server...", Toast.LENGTH_SHORT).show()
        ApiClient.checkServerAlive(this) { alive, error ->
            runOnUiThread {
                if (alive) {
                    showCpFileDialog(onAction)
                } else {
                    showPingErrorDialog(error ?: "Unknown error")
                }
            }
        }
    }

    /** Pings and sends text directly (used by PROCESS_TEXT where CP is the only sensible action). */
    private fun pingThenSendText(text: String) {
        ApiClient.checkServerAlive(this) { alive, error ->
            runOnUiThread {
                if (alive) {
                    sendTextToMac(text)
                } else {
                    showPingErrorDialog(error ?: "Unknown error")
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showCpFileDialog(onAction: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Send to Mac")
            .setItems(arrayOf("CP  (copy to clipboard)", "File  (save to Documents)")) { _, which ->
                val action = if (which == 0) "clipboard" else "save"
                onAction(action)
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

    // ── Dispatch ──────────────────────────────────────────────────────────────

    /** Routes a text payload: CP → /share/text; File → convert to .txt then /share/file */
    private fun dispatchText(text: String, action: String) {
        if (action == "clipboard") {
            sendTextToMac(text)
        } else {
            // Convert text to a temporary .txt file and send as a file
            val txtFile = textToTempFile(text) ?: run {
                showError("Failed to create text file.")
                return
            }
            val fileUri = Uri.fromFile(txtFile)
            Toast.makeText(this, "Saving text as file...", Toast.LENGTH_SHORT).show()
            ApiClient.shareFile(this, fileUri, "save") { success, error ->
                txtFile.delete()
                showResult(success, error)
            }
        }
    }

    /** Routes a file URI: CP or File → /share/file with the corresponding action */
    private fun dispatchFile(uri: Uri, action: String) {
        val label = if (action == "clipboard") "Copying file to clipboard..." else "Saving file to Mac..."
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
        ApiClient.shareFile(this, uri, action) { success, error ->
            showResult(success, error)
        }
    }

    private fun sendTextToMac(text: String) {
        Toast.makeText(this, "Copying text to Mac clipboard...", Toast.LENGTH_SHORT).show()
        ApiClient.shareText(this, text) { success, error ->
            showResult(success, error)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Writes [text] to a temporary UTF-8 .txt file in the cache directory.
     * The caller is responsible for deleting the file after use.
     */
    private fun textToTempFile(text: String): File? {
        return try {
            val file = File(cacheDir, "shared_text_${System.currentTimeMillis()}.txt")
            FileOutputStream(file).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun showError(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
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
