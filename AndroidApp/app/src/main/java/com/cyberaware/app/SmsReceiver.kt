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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            // GoAsync ব্যবহার করছি যাতে অ্যাপ ক্র্যাশ না করে এবং ব্যাকগ্রাউন্ডে কাজ হয়
            val pendingResult = goAsync()

            Thread {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (messages != null) {
                        for (sms in messages) {
                            val sender = sms.originatingAddress ?: "Unknown"
                            val messageBody = sms.messageBody ?: ""

                            // মেসেজ চেক করা
                            checkSMSAndReport(context, sender, messageBody)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error receiving SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }

    private fun checkSMSAndReport(context: Context, sender: String, message: String) {
        // Simple Spam Detection Logic (SpamDetector ক্লাস না থাকলেও কাজ করবে)
        val lowerMsg = message.lowercase()
        val isSuspicious = lowerMsg.contains("win") ||
                lowerMsg.contains("lottery") ||
                lowerMsg.contains("bank") ||
                lowerMsg.contains("verify") ||
                lowerMsg.contains("click here")

        if (isSuspicious) {
            Log.d("CyberAware", "⚠️ THREAT DETECTED from $sender")

            // ১. নোটিফিকেশন দেখান
            showScamNotification(context, sender, message)

            // ২. সার্ভারে রিপোর্ট পাঠান
            sendReportToServer(context, sender, message)
        } else {
            Log.d("CyberAware", "Message is Safe.")
        }
    }

    private fun sendReportToServer(context: Context, sender: String, message: String) {
        // আমরা সরাসরি RetrofitClient ব্যবহার করছি
        val request = ReportRequest(
            type = "Auto-SMS-Scan",
            description = "Suspicious SMS from $sender: $message",
            userId = "System-Detector"
        )

        RetrofitClient.instance.sendReport(request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                Log.d("CyberAware", "✅ Auto-Reported to Server")
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("CyberAware", "❌ Report Failed: ${t.message}")
            }
        })
    }

    private fun showScamNotification(context: Context, sender: String, message: String) {
        createNotificationChannel(context) // চ্যানেল তৈরি নিশ্চিত করা

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "SCAM_ALERT")
            .setSmallIcon(android.R.drawable.stat_notify_error) // আইকন চেঞ্জ করতে পারেন
            .setContentTitle("🚨 Possible Scam Detected!")
            .setContentText("From: $sender")
            .setStyle(NotificationCompat.BigTextStyle().bigText("CyberAware detected a threat:\n$message"))
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
            val name = "Scam Alerts"
            val descriptionText = "Notifications for detected scams"
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