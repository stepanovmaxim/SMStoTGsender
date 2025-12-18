package com.example.smstotgsender

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        val etToken = findViewById<EditText>(R.id.etBotToken)
        val etChatId = findViewById<EditText>(R.id.etChatId)
        val etFilter = findViewById<EditText>(R.id.etFilter)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)

        // Получаем доступ к хранилищу настроек
        val sharedPrefs = getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)

        // 1. ЗАГРУЗКА: Подставляем сохраненные значения в поля
        etToken.setText(sharedPrefs.getString("BOT_TOKEN", ""))
        etChatId.setText(sharedPrefs.getString("CHAT_ID", ""))
        etFilter.setText(sharedPrefs.getString("FILTER_LIST", ""))

        // 2. СОХРАНЕНИЕ: Обработка кнопки "Сохранить"
        btnSave.setOnClickListener {
            val token = etToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()
            val filter = etFilter.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                // Используем ресурсы для текста (мультиязычность)
                Toast.makeText(this, getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
            } else {
                sharedPrefs.edit().apply {
                    putString("BOT_TOKEN", token)
                    putString("CHAT_ID", chatId)
                    putString("FILTER_LIST", filter)
                    apply()
                }
                Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
            }
        }

        // 3. ТЕСТ: Обработка кнопки "Тест"
        btnTest.setOnClickListener {
            val token = etToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()

            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                sendTestMessage(token, chatId)
            } else {
                Toast.makeText(this, getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Проверка прав на чтение SMS
        checkPermissions()
    }

    private fun sendTestMessage(token: String, chatId: String) {
        val client = OkHttpClient()
        val url = "https://api.telegram.org/bot$token/sendMessage"

        // Берем текст приветствия из ресурсов
        val text = getString(R.string.msg_test_body)

        val formBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        // Запрос выполняется в фоновом потоке
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    // Возвращаемся в главный поток (UI Thread) чтобы показать результат
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this, getString(R.string.msg_test_sent), Toast.LENGTH_LONG).show()
                        } else {
                            val errorMsg = "Code: ${response.code}"
                            Toast.makeText(this, getString(R.string.msg_test_error, errorMsg), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.msg_test_error, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
    }
}