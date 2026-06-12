package com.example.clipboardsync.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun getServerUrl(context: Context, path: String): String? {
        val prefs = context.getSharedPreferences("ClipboardSyncPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", "")
        val port = prefs.getInt("server_port", 8766)
        
        if (ip.isNullOrEmpty()) {
            return null
        }
        return "http://$ip:$port$path"
    }

    fun shareText(context: Context, text: String, callback: (Boolean, String?) -> Unit) {
        val url = getServerUrl(context, "/share/text")
        if (url == null) {
            callback(false, "Server IP not configured")
            return
        }

        val json = JSONObject()
        json.put("text", text)
        val body = json.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val code = response.code
                response.close()
                callback(success, if (!success) "HTTP $code" else null)
            }
        })
    }

    fun shareImage(context: Context, imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        val url = getServerUrl(context, "/share/image")
        if (url == null) {
            callback(false, "Server IP not configured")
            return
        }

        try {
            // Write URI to a temporary file
            val tempFile = File.createTempFile("shared_image", ".png", context.cacheDir)
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val mediaType = context.contentResolver.getType(imageUri)?.toMediaType() ?: "image/png".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", tempFile.name, tempFile.asRequestBody(mediaType))
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

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
}
