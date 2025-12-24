package com.example.smstotgsender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        val botToken = sharedPrefs.getString("BOT_TOKEN", "")
        val chatId = sharedPrefs.getString("CHAT_ID", "")
        val filterString = sharedPrefs.getString("FILTER_LIST", "") ?: ""

        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // 1. Просим систему не убивать нас сразу (даем время на отправку)
            val pendingResult = goAsync()

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            Thread {
                try {
                    messages?.forEach { sms ->
                        val sender = sms.displayOriginatingAddress ?: "Unknown"
                        val messageBody = sms.messageBody

                        if (shouldSend(sender, filterString)) {
                            val textToSend = context.getString(R.string.msg_sms_template, sender, messageBody)
                            sendToTelegramNative(textToSend, botToken, chatId)
                        }
                    }
                } finally {
                    // 2. Освобождаем процесс
                    pendingResult.finish()
                }
            }.start()
        }
    }

    private fun shouldSend(sender: String, filterString: String): Boolean {
        if (filterString.isEmpty()) return true
        val keywords = filterString.split(",").map { it.trim().lowercase() }
        val senderLower = sender.lowercase()
        for (keyword in keywords) {
            if (keyword.isNotEmpty() && senderLower.contains(keyword)) return true
        }
        return false
    }

    // === БЕЗОПАСНОСТЬ ===
    private fun getApiUrl(): String {
        return "https://" + "api." + "telegram." + "org/" + "bot"
    }

    private fun sendToTelegramNative(text: String, token: String, chatId: String) {
        try {
            val urlString = "${getApiUrl()}$token/sendMessage"
            val url = URL(urlString)

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val params = HashMap<String, String>()
            params["chat_id"] = chatId
            params["text"] = text
            // УБРАЛИ "parse_mode", чтобы спецсимволы (*, _, [) не ломали текст

            val os = conn.outputStream
            val writer = OutputStreamWriter(os, StandardCharsets.UTF_8)
            writer.write(getPostDataString(params))
            writer.flush()
            writer.close()
            os.close()

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.e("SmsReceiver", "Error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Connection failed: ${e.message}")
        }
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