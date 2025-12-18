package com.example.smstotgsender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.*
import java.io.IOException

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Загружаем настройки из памяти
        val sharedPrefs = context.getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        val botToken = sharedPrefs.getString("BOT_TOKEN", "")
        val chatId = sharedPrefs.getString("CHAT_ID", "")
        val filterString = sharedPrefs.getString("FILTER_LIST", "") ?: ""

        // Если токен не настроен — выходим
        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.e("SmsReceiver", "Token or ChatID not set")
            return
        }

        // Проверяем, что это именно SMS
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            messages?.forEach { sms ->
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.messageBody

                // === ЛОГИКА ФИЛЬТРАЦИИ ===
                if (shouldSend(sender, filterString)) {
                    // Формируем сообщение, используя шаблон из ресурсов
                    // (Context нужен для доступа к getString внутри Receiver)
                    val textToSend = context.getString(R.string.msg_sms_template, sender, messageBody)

                    sendToTelegram(textToSend, botToken, chatId)
                } else {
                    Log.i("SmsReceiver", "SMS skipped by filter: $sender")
                }
            }
        }
    }

    // Функция проверки фильтра
    private fun shouldSend(sender: String, filterString: String): Boolean {
        // Если фильтр пустой — разрешаем все
        if (filterString.isEmpty()) return true

        // Разбиваем строку "Sber, Tinkoff" -> ["sber", "tinkoff"]
        val keywords = filterString.split(",").map { it.trim().lowercase() }
        val senderLower = sender.lowercase()

        // Ищем совпадение
        for (keyword in keywords) {
            if (keyword.isNotEmpty() && senderLower.contains(keyword)) {
                return true
            }
        }
        return false
    }

    private fun sendToTelegram(text: String, token: String, chatId: String) {
        val client = OkHttpClient()
        val url = "https://api.telegram.org/bot$token/sendMessage"

        val formBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .add("parse_mode", "Markdown") // Жирный шрифт и курсив
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        // Запускаем в отдельном потоке
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("SmsReceiver", "Failed to send: ${response.code}")
                    } else {
                        Log.i("SmsReceiver", "SMS sent successfully")
                    }
                }
            } catch (e: IOException) {
                Log.e("SmsReceiver", "Network error: ${e.message}")
            }
        }.start()
    }
}