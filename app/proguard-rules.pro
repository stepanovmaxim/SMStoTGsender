# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- ПРАВИЛА ДЛЯ OKHTTP (СЕТЬ) ---
# Без этих строк приложение упадет при отправке запроса в Telegram
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- ПРАВИЛА ДЛЯ ВАШЕГО ПРИЛОЖЕНИЯ ---
# Запрещаем переименовывать ваши классы (Activity, BroadcastReceiver),
# чтобы Android мог их найти в Manifest
-keep class com.example.smstotgsender.** { *; }

# --- ОТЛАДКА ---
# Оставляем номера строк в ошибках, чтобы понимать, где упало, если что-то пойдет не так
-keepattributes SourceFile,LineNumberTable