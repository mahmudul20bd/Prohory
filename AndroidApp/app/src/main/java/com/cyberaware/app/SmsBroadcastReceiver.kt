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
import java.util.UUID

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val pendingResult = goAsync() // প্রসেস কিল হওয়া থেকে বাঁচাবে

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
                        pendingResult.finish()
                    }
                }
            }
        }
    }
    private suspend fun analyzeThreat(context: Context, sender: String, body: String) {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", "") ?: ""
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // ==========================================
        // 🚀 ENTERPRISE TELEMETRY DATA PREPARATION
        // ==========================================
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
            // 🚀 ১. সবার আগে Cloud AI কল করবে (কারণ এটি সবচেয়ে পাওয়ারফুল)
            val response = RetrofitClient.apiService.checkSpamOnline(authHeader, telemetryPayload)

            if (response.isSuccessful && response.body()?.isSpam == true) {
                triggerThreatAlert(context, sender, body, "Cloud Backend")
            } else if (!response.isSuccessful) {
                // টোকেন এক্সপায়ার হলে বা সার্ভার এরর দিলে Offline-এ চলে যাবে
                runOfflineFallback(context, sender, body)
            }
        } catch (e: Exception) {
            // ইন্টারনেট কানেকশন না থাকলে Offline-এ চলে যাবে
            Log.e("PROHORY_GUARD", "Cloud AI failed: ${e.message}. Using Offline AI.")
            runOfflineFallback(context, sender, body)
        }
    }

    // 🚀 ২. ইন্টারনেট বা সার্ভার ফেইল করলে এটি কাজ করবে (Purely Offline AI - No Hardcoded Strings)
    private fun runOfflineFallback(context: Context, sender: String, body: String) {
        val modelFile = File(context.filesDir, "cyberaware_model.tflite")
        if (modelFile.exists()) {
            val detector = SpamDetector.getInstance(context)
            detector.classify(body) { labelIndex, _ ->
                // Model er output jodi 2 (Spam) ba 3 (Smishing) hoy, tobei alert dibe
                if (labelIndex == 2 || labelIndex == 3) {
                    triggerThreatAlert(context, sender, body, "Offline AI")
                } else {
                    Log.d("PROHORY_GUARD", "Offline AI scanned and marked it as SAFE.")
                }
            }
        } else {
            // Model na thakle ekhon ar kono fariabaji (keyword check) hobe na
            Log.e("PROHORY_GUARD", "Offline Model is missing! Cannot scan threat without internet.")
        }
    }

    private fun triggerThreatAlert(context: Context, sender: String, body: String, source: String) {
        showThreatNotification(context, sender, body)
        saveToSmsGuard(context, sender, body)
    }



    private fun showThreatNotification(context: Context, sender: String, body: String) {
        val channelId = "PROHORY_THREAT_ALERTS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Cyber Threats", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(context, SmsGuardActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Malicious SMS Detected")
            .setContentText("Source: $sender")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try { NotificationManagerCompat.from(context).notify(sender.hashCode(), builder.build()) }
        catch (e: SecurityException) { Log.e("PROHORY_GUARD", "Notification Permission নেই!") }
    }

    private fun saveToSmsGuard(context: Context, sender: String, body: String) {
        val sharedPref = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
        val currentThreats = sharedPref.getStringSet("quarantine_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentThreats.add("$sender|||$body|||${System.currentTimeMillis()}")
        sharedPref.edit().putStringSet("quarantine_list", currentThreats).apply()
    }
}