package com.cyberaware.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cyberaware.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            // 🔥 MOST IMPORTANT: goAsync() সিস্টেমকে বলে দেয় প্রসেস কিল না করতে, যতক্ষণ এপিআই রেসপন্স না আসে
            val pendingResult = goAsync()

            for (message in smsMessages) {
                val sender = message.displayOriginatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""

                Log.d("PROHORY_GUARD", "নতুন এসএমএস এসেছে: $sender")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        analyzeThreat(context, sender, body)
                    } catch (e: Exception) {
                        Log.e("PROHORY_GUARD", "SMS Process Error: ${e.message}")
                    } finally {
                        // কাজ শেষ হলে সিস্টেমকে রিলিজ করে দেওয়া
                        pendingResult.finish()
                    }
                }
            }
        }
    }
    private suspend fun analyzeThreat(context: Context, sender: String, body: String) {
        val modelFile = File(context.filesDir, "cyberaware_model.tflite")

        if (modelFile.exists()) {
            Log.d("PROHORY_GUARD", "অফলাইন মডেল পাওয়া গেছে। স্ক্যান হচ্ছে...")
            val detector = SpamDetector.getInstance(context)
            detector.classify(body) { labelIndex, _ ->
                if (labelIndex == 2 || labelIndex == 3) {
                    triggerThreatAlert(context, sender, body, "Offline AI")
                }
            }
        } else {
            Log.d("PROHORY_GUARD", "মডেল নেই! সরাসরি Backend API কল করা হচ্ছে...")
            try {
                // 🔥 SharedPreferences থেকে ইউজারের সেভ করা টোকেন নেওয়া হচ্ছে
                val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
                val token = sharedPref.getString("auth_token", "") ?: ""
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                val payload = mapOf("message" to body)

                // 🔥 টোকেনসহ এপিআই কল করা হচ্ছে
                val response = RetrofitClient.apiService.checkSpamOnline(authHeader, payload)

                if (response.isSuccessful && response.body()?.isSpam == true) {
                    triggerThreatAlert(context, sender, body, "Cloud Backend")
                } else if (!response.isSuccessful) {
                    Log.e("PROHORY_GUARD", "API Error Response: ${response.code()}")
                    fallbackKeywordCheck(context, sender, body)
                }
            } catch (e: Exception) {
                Log.e("PROHORY_GUARD", "ইন্টারনেট বা সার্ভার সমস্যা: ${e.message}")
                fallbackKeywordCheck(context, sender, body)
            }
        }
    }
    // ব্যাকএন্ড ফেইল করলে বা ইন্টারনেট না থাকলে এটি ইউজারকে বাঁচাবে
    private fun fallbackKeywordCheck(context: Context, sender: String, body: String) {
        val lowerBody = body.lowercase()
        if (lowerBody.contains("bit.ly") || lowerBody.contains("lottery") || lowerBody.contains("win")) {
            triggerThreatAlert(context, sender, body, "Emergency Keyword")
        }
    }

    private fun triggerThreatAlert(context: Context, sender: String, body: String, source: String) {
        Log.d("PROHORY_GUARD", "$source দ্বারা স্প্যাম ধরা পড়েছে! নোটিফিকেশন যাচ্ছে...")
        showThreatNotification(context, sender, body)
        saveToSmsGuard(context, sender, body)
    }

    private fun showThreatNotification(context: Context, sender: String, body: String) {
        val channelId = "PROHORY_THREAT_ALERTS"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Cyber Threats", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Phishing and Spam Alerts" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, SmsGuardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Malicious SMS Detected")
            .setContentText("Source: $sender")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(sender.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e("PROHORY_GUARD", "Notification Permission নেই!")
        }
    }

    private fun saveToSmsGuard(context: Context, sender: String, body: String) {
        val sharedPref = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
        val currentThreats = sharedPref.getStringSet("quarantine_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentThreats.add("$sender|||$body|||${System.currentTimeMillis()}")
        sharedPref.edit().putStringSet("quarantine_list", currentThreats).apply()
    }
}