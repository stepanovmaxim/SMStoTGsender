package com.example.smstotgsender

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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

        // --- Инициализация элементов ---
        val etToken = findViewById<EditText>(R.id.etBotToken)
        val etChatId = findViewById<EditText>(R.id.etChatId)
        val etFilter = findViewById<EditText>(R.id.etFilter)

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val btnBattery = findViewById<Button>(R.id.btnBattery) // Кнопка батареи

        // --- Работа с настройками ---
        val sharedPrefs = getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)

        // Загружаем сохраненные данные в поля
        etToken.setText(sharedPrefs.getString("BOT_TOKEN", ""))
        etChatId.setText(sharedPrefs.getString("CHAT_ID", ""))
        etFilter.setText(sharedPrefs.getString("FILTER_LIST", ""))

        // --- Кнопка СОХРАНИТЬ ---
        btnSave.setOnClickListener {
            val token = etToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()
            val filter = etFilter.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
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

        // --- Кнопка ТЕСТ ---
        btnTest.setOnClickListener {
            val token = etToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()

            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                sendTestMessage(token, chatId)
            } else {
                Toast.makeText(this, getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
            }
        }

        // --- Кнопка БАТАРЕЯ (Фон/Автозапуск) ---
        btnBattery.setOnClickListener {
            requestBatteryIgnore()  // 1. Просим Android не убивать процесс
            openAutoStartSettings() // 2. Пытаемся открыть автозапуск (для Xiaomi/Huawei)
        }

        // --- Проверки при запуске ---
        checkPermissions()
        checkBatteryOptimization() // Напоминаем, если экономия включена
    }

    // === ЛОГИКА ОТПРАВКИ ТЕСТА ===
    private fun sendTestMessage(token: String, chatId: String) {
        val client = OkHttpClient()
        val url = "https://api.telegram.org/bot$token/sendMessage"

        // Берем текст из ресурсов (для перевода)
        val text = getString(R.string.msg_test_body)

        val formBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
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

    // === ЛОГИКА РАЗРЕШЕНИЙ SMS ===
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
    }

    // === ЛОГИКА БАТАРЕИ И АВТОЗАПУСКА ===

    // Запрос на отключение Doze Mode (экономии батареи)
    private fun requestBatteryIgnore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, getString(R.string.msg_battery_ignored), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Проверка статуса при запуске (показываем тост, если не настроено)
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, getString(R.string.msg_battery_request), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Попытка открыть скрытые меню автозапуска для китайских оболочек
    private fun openAutoStartSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()
        try {
            when {
                "xiaomi" in manufacturer -> {
                    intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                "oppo" in manufacturer -> {
                    intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                "vivo" in manufacturer -> {
                    intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                "huawei" in manufacturer || "honor" in manufacturer -> {
                    intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                }
                else -> return // Для остальных брендов ничего не делаем
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace() // Если меню не найдено, просто игнорируем
        }
    }
}