package com.example.smstotgsender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etToken = findViewById<EditText>(R.id.etBotToken)
        val etChatId = findViewById<EditText>(R.id.etChatId)
        val etFilter = findViewById<EditText>(R.id.etFilter)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)

        // Кнопку батареи можно оставить в UI, но логику сделаем безопасной (просто открытие настроек)
        // Или лучше вообще убрать агрессивные запросы
        val btnBattery = findViewById<Button>(R.id.btnBattery)

        val sharedPrefs = getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        etToken.setText(sharedPrefs.getString("BOT_TOKEN", ""))
        etChatId.setText(sharedPrefs.getString("CHAT_ID", ""))
        etFilter.setText(sharedPrefs.getString("FILTER_LIST", ""))

        btnSave.setOnClickListener {
            sharedPrefs.edit().apply {
                putString("BOT_TOKEN", etToken.text.toString().trim())
                putString("CHAT_ID", etChatId.text.toString().trim())
                putString("FILTER_LIST", etFilter.text.toString().trim())
                apply()
            }
            Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            val token = etToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()
            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                sendTestMessageNative(token, chatId)
            } else {
                Toast.makeText(this, getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
            }
        }

        // Упрощенная логика батареи: просто открываем настройки приложений
        btnBattery.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Please disable battery optimization manually", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { }
        }

        findViewById<Button>(R.id.btnGithub).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/stepanovmaxim/SMStoTGsender")))
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
    }

    // === НЕТ OKHTTP, ЧИСТАЯ JAVA ===
    private fun sendTestMessageNative(token: String, chatId: String) {
        val text = getString(R.string.msg_test_body)

        Thread {
            try {
                // Сборка URL
                val urlString = "https://" + "api.telegram.org/bot$token/sendMessage"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000

                // Параметры
                val params = HashMap<String, String>()
                params["chat_id"] = chatId
                params["text"] = text

                // Отправка
                val os = conn.outputStream
                val writer = OutputStreamWriter(os, StandardCharsets.UTF_8)
                writer.write(getPostDataString(params))
                writer.flush()
                writer.close()
                os.close()

                val code = conn.responseCode
                runOnUiThread {
                    if (code == 200) {
                        Toast.makeText(this, getString(R.string.msg_test_sent), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error code: $code", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun getPostDataString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
    }
}