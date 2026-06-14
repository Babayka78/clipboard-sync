package com.example.clipboardsync.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    // Main client for regular requests (longer timeouts for file uploads)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Dedicated client for the pre-flight ping with a strict 2-second timeout
    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun getServerUrl(context: Context, path: String): String? {
        val prefs = context.getSharedPreferences("ClipboardSyncPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", "")
        val port = prefs.getInt("server_port", 8766)
        if (ip.isNullOrEmpty()) return null
        return "http://$ip:$port$path"
    }

    fun getServerIp(context: Context): String {
        val prefs = context.getSharedPreferences("ClipboardSyncPrefs", Context.MODE_PRIVATE)
        return prefs.getString("server_ip", "") ?: ""
    }

    fun getServerPort(context: Context): Int {
        val prefs = context.getSharedPreferences("ClipboardSyncPrefs", Context.MODE_PRIVATE)
        return prefs.getInt("server_port", 8766)
    }

    /**
     * Performs a pre-flight HTTP GET to /ping with a 2-second timeout.
     * Calls back on the calling thread (background — caller must dispatch to main if needed).
     *
     * @param callback (isAlive: Boolean, errorMessage: String?)
     */
    fun checkServerAlive(context: Context, callback: (Boolean, String?) -> Unit) {
        val url = getServerUrl(context, "/ping")
        if (url == null) {
            callback(false, "Server IP not configured. Open the app and set the Mac IP address.")
            return
        }
        val request = Request.Builder().url(url).get().build()
        pingClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val ip = getServerIp(context)
                val port = getServerPort(context)
                callback(
                    false,
                    "Target Mac is unreachable. Ensure it is powered on and connected to the same network. " +
                    "Verify the IP address in settings is correct and the server script is running. " +
                    "You can test the script by running: 'curl http://$ip:$port/ping'"
                )
            }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful
                response.close()
                if (ok) {
                    callback(true, null)
                } else {
                    callback(false, "Server returned HTTP ${response.code}")
                }
            }
        })
    }

    /**
     * Sends plain text to the Mac server (copies to clipboard on Mac).
     */
    fun shareText(context: Context, text: String, callback: (Boolean, String?) -> Unit) {
        val url = getServerUrl(context, "/share/text")
        if (url == null) {
            callback(false, "Server IP not configured")
            return
        }
        val json = JSONObject().apply { put("text", text) }
        val body = json.toString().toRequestBody(JSON)
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false, e.message) }
            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val code = response.code
                response.close()
                callback(success, if (!success) "HTTP $code" else null)
            }
        })
    }

    /**
     * Sends any file (image, document, etc.) to the Mac server.
     *
     * @param action "clipboard" — place on Mac clipboard; "save" — save to ~/Documents/ClipboardSync
     */
    fun shareFile(
        context: Context,
        fileUri: Uri,
        action: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val url = getServerUrl(context, "/share/file")
        if (url == null) {
            callback(false, "Server IP not configured")
            return
        }
        try {
            // Resolve the original filename
            val filename = resolveFilename(context, fileUri) ?: "shared_file"
            val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

            // Copy URI contents into a temporary file
            val ext = filename.substringAfterLast('.', "")
            val prefix = filename.substringBeforeLast('.').take(16).ifEmpty { "file" }
            val tempFile = File.createTempFile(prefix, if (ext.isNotEmpty()) ".$ext" else "", context.cacheDir)
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }

            val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, tempFile.asRequestBody(mediaType))
                .addFormDataPart("action", action)
                .build()

            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    tempFile.delete()
                    callback(false, e.message)
                }
                override fun onResponse(call: Call, response: Response) {
                    tempFile.delete()
                    val success = response.isSuccessful
                    val code = response.code
                    response.close()
                    callback(success, if (!success) "HTTP $code" else null)
                }
            })
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }

    private fun resolveFilename(context: Context, uri: Uri): String? {
        // Try content resolver display name first
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        // Fall back to the last path segment
        return uri.lastPathSegment
    }
}
