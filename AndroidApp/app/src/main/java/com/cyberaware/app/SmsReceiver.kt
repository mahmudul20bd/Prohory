package com.cyberaware.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cyberaware.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            // GoAsync ব্যবহার করছি যাতে অ্যাপ ক্র্যাশ না করে এবং ব্যাকগ্রাউন্ডে কাজ হয়
            val pendingResult = goAsync()

            Thread {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (messages != null) {
                        for (sms in messages) {
                            val sender = sms.originatingAddress ?: "Unknown"
                            val messageBody = sms.messageBody ?: ""

                            // মেসেজ চেক করা এবং সার্ভারে পাঠানো
                            checkSMSAndReport(context, sender, messageBody)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Prohory_SmsReceiver", "Error receiving SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }

    private fun checkSMSAndReport(context: Context, sender: String, message: String) {
        // Simple Spam Detection Logic (লোকাল ডিটেকশন)
        val lowerMsg = message.lowercase()
        val isSuspicious = lowerMsg.contains("win") ||
                lowerMsg.contains("lottery") ||
                lowerMsg.contains("bank") ||
                lowerMsg.contains("verify") ||
                lowerMsg.contains("click here") ||
                lowerMsg.contains("urgent")

        if (isSuspicious) {
            Log.d("Prohory_SmsReceiver", "⚠️ LOCAL THREAT DETECTED from $sender")

            // ১. লোকাল নোটিফিকেশন দেখান
            showScamNotification(context, sender, message)
        } else {
            Log.d("Prohory_SmsReceiver", "Message locally safe. Forwarding to AI Core...")
        }

        // ২. সব মেসেজ ব্যাকএন্ডে (AI Core) পাঠানো হবে যাতে গ্রাফ রিয়েল-টাইমে আপডেট হয়
        sendReportToServer(context, sender, message)
    }

    private fun sendReportToServer(context: Context, sender: String, message: String) {
        // SharedPreferences থেকে লগইন করা এজেন্টের টোকেন এবং আইডি নেওয়া
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", "") ?: ""

        // 🔥 Null Safety ফিক্স করা হলো (?: ব্যবহার করে)
        val reporterId = sharedPref.getString("agentId", "System-Detector") ?: "System-Detector"
        // ইউজার লগইন না থাকলে ডেটা পাঠানো হবে না
        if (token.isEmpty()) {
            Log.e("Prohory_SmsReceiver", "❌ No Auth Token. Cannot send report.")
            return
        }

        // Retrofit এর Coroutine বেইজড API কল
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Spring Boot এর ReportRequest / Report Entity এর সাথে ম্যাচ করে ডাটা ম্যাপ করা
                val payload = mapOf(
                    "sender" to sender,
                    "message" to message,
                    "reporter" to reporterId
                )

                val response = RetrofitClient.apiService.submitSmsReport("Bearer $token", payload)

                if (response.isSuccessful) {
                    Log.d("Prohory_SmsReceiver", "✅ Auto-Reported to Server! Graph will spike if threat found.")
                } else {
                    Log.e("Prohory_SmsReceiver", "❌ Report Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Prohory_SmsReceiver", "⚠️ Network Error: ${e.message}")
            }
        }
    }

    private fun showScamNotification(context: Context, sender: String, message: String) {
        createNotificationChannel(context) // চ্যানেল তৈরি নিশ্চিত করা

        // 노টিফিকেশনে ক্লিক করলে সরাসরি DashboardActivity তে যাবে
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "SCAM_ALERT")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("🚨 Prohory: Threat Intercepted!")
            .setContentText("Sender: $sender")
            .setStyle(NotificationCompat.BigTextStyle().bigText("CyberAware AI detected a threat:\n$message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // পারমিশন চেক (Android 13+)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prohory Alerts"
            val descriptionText = "Notifications for detected malicious SMS"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SCAM_ALERT", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}