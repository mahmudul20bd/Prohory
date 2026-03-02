package com.cyberaware.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (smsMessages == null || smsMessages.isEmpty()) return

            val pendingResult = goAsync()

            var fullBody = ""
            var sender = "Unknown"

            for (message in smsMessages) {
                if (message != null) {
                    sender = message.displayOriginatingAddress ?: "Unknown"
                    fullBody += message.messageBody ?: ""
                }
            }

            Log.d("PROHORY_GUARD", "New SMS Received: $sender")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    analyzeThreat(context, sender, fullBody)
                } catch (e: Exception) {
                    Log.e("PROHORY_GUARD", "SMS Process Error: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun analyzeThreat(context: Context, sender: String, body: String) {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", "") ?: ""
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        val userRegion = sharedPref.getString("user_division", "") ?: ""
        var deviceId = sharedPref.getString("anonymous_device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPref.edit().putString("anonymous_device_id", deviceId).apply()
        }

        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE}"

        val telemetryPayload = mapOf(
            "message" to body,
            "region" to userRegion,
            "deviceId" to deviceId,
            "deviceModel" to deviceModel,
            "osVersion" to osVersion
        )

        try {
            val response = RetrofitClient.apiService.checkSpamOnline(authHeader, telemetryPayload)

            // 🔥 Cloud API থেকে আসা 4-Level State হ্যান্ডেল করা
            if (response.isSuccessful && response.body() != null) {
                val threatLevel = response.body()?.threatLevel?.uppercase() ?: "SAFE"

                when (threatLevel) {
                    "DANGER" -> {
                        Log.d("PROHORY_GUARD", "Cloud AI marked it as DANGER.")
                        triggerThreatAlert(context, sender, body, "Cloud Scanner", "DANGER")
                    }
                    "SPAM" -> {
                        Log.d("PROHORY_GUARD", "Cloud AI marked it as SPAM.")
                        triggerThreatAlert(context, sender, body, "Cloud Scanner", "SPAM")
                    }
                    "PROMO" -> {
                        Log.d("PROHORY_GUARD", "Cloud AI marked it as PROMO. Ignored.")
                    }
                    "SAFE" -> {
                        Log.d("PROHORY_GUARD", "Cloud AI marked it as SAFE. Ignored.")
                    }
                    else -> {
                        // Fallback logic
                        if (response.body()?.isSpam == true) {
                            triggerThreatAlert(context, sender, body, "Cloud Scanner", "DANGER")
                        } else {
                            runOfflineFallback(context, sender, body)
                        }
                    }
                }
            } else {
                runOfflineFallback(context, sender, body)
            }
        } catch (e: Exception) {
            Log.e("PROHORY_GUARD", "Cloud AI failed: ${e.message}. Switching to Offline mode.")
            runOfflineFallback(context, sender, body)
        }
    }

    private suspend fun runOfflineFallback(context: Context, sender: String, body: String) {
        val modelFile = File(context.filesDir, "cyberaware_model.tflite")

        if (modelFile.exists()) {
            val detector = SpamDetector.getInstance(context)

            val labelIndex = suspendCoroutine<Int> { continuation ->
                detector.classify(body) { index, _ ->
                    continuation.resume(index)
                }
            }

            // 🔥 4-Level PURE AI DETECTION Logic (Local TFLite)
            when (labelIndex) {
                3 -> { // DANGER
                    Log.d("PROHORY_GUARD", "Offline AI marked it as DANGER.")
                    triggerThreatAlert(context, sender, body, "Offline Scanner", "DANGER")
                }
                2 -> { // SPAM
                    Log.d("PROHORY_GUARD", "Offline AI marked it as SPAM.")
                    triggerThreatAlert(context, sender, body, "Offline Scanner", "SPAM")
                }
                1 -> { // PROMO
                    Log.d("PROHORY_GUARD", "Offline AI marked it as PROMO. Ignored.")
                }
                else -> { // SAFE
                    Log.d("PROHORY_GUARD", "Offline AI marked it as SAFE. Ignored.")
                }
            }
        } else {
            Log.e("PROHORY_GUARD", "Offline Model is missing!")
        }
    }

    // 🔥 FIX: threatLevel পাস করা হলো
    private fun triggerThreatAlert(context: Context, sender: String, body: String, source: String, threatLevel: String) {
        showThreatNotification(context, sender, body, source, threatLevel)
        saveToSmsGuard(context, sender, body, threatLevel)
    }

    private fun showThreatNotification(context: Context, sender: String, body: String, source: String, threatLevel: String) {
        val channelId = "PROHORY_THREAT_ALERTS"

        // 🔥 Threat Level অনুযায়ী ডাইনামিক কালার এবং টেক্সট
        val isDanger = threatLevel == "DANGER"
        val notifColor = if (isDanger) Color.RED else android.graphics.Color.parseColor("#FF9800") // Red for Danger, Orange for Spam
        val titleText = if (isDanger) "🚨 Dangerous Scam Blocked!" else "⚠️ Spam Message Detected!"
        val bigTitle = if (isDanger) "🚨 Scam Alert!" else "⚠️ Spam Alert!"
        val summaryText = if (isDanger) "We blocked a dangerous message from $sender." else "A suspicious spam message was intercepted from $sender."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Scam Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts for dangerous and spam messages"
                enableVibration(true)
                enableLights(true)
                lightColor = notifColor
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, SmsGuardActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(notifColor)
            .setContentTitle(titleText)
            .setContentText("Sender: $sender")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(bigTitle)
                .setSummaryText("Protected by $source")
                .bigText("$summaryText\n\nMessage:\n$body"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try { NotificationManagerCompat.from(context).notify(sender.hashCode(), builder.build()) }
        catch (e: SecurityException) { Log.e("PROHORY_GUARD", "Notification Permission missing!") }
    }

    // 🔥 FIX: threatLevel প্যারামিটার যোগ করা হলো এবং স্ট্রিংয়ের শেষে সেভ করা হলো
    private fun saveToSmsGuard(context: Context, sender: String, body: String, threatLevel: String) {
        val sharedPref = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
        val currentThreats = sharedPref.getStringSet("quarantine_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // 4th প্যারামিটার হিসেবে threatLevel যোগ করে দেওয়া হলো
        currentThreats.add("$sender|||$body|||${System.currentTimeMillis()}|||$threatLevel")

        sharedPref.edit().putStringSet("quarantine_list", currentThreats).apply()
    }
}