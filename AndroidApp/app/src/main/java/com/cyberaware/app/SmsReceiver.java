package com.cyberaware.app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class SmsReceiver extends BroadcastReceiver {

    // 🔥 আপনার সার্ভারের আইপি এখানে বসান (MainActivity র মতো সেইম হতে হবে)
    private static final String BASE_URL = "http://192.168.0.104:8080/";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = sms.getOriginatingAddress();
                        String messageBody = sms.getMessageBody();

                        // AI চেক করা এবং প্রয়োজনে সার্ভারে পাঠানো
                        checkSMSAndReport(context, sender, messageBody);
                    }
                }
            }
        }
    }

    private void checkSMSAndReport(Context context, String sender, String message) {
        SpamDetector detector = new SpamDetector(context);
        String result = detector.classify(message);

        // ১. যদি লোকাল AI বলে এটা স্ক্যাম
        if (result.contains("SCAM") || result.contains("SUSPICIOUS")) {
            Log.d("CyberAware", "⚠️ SCAM DETECTED! Reporting to User & Server...");

            // A. ইউজারকে নোটিফিকেশন দিন
            showScamNotification(context, sender, message);

            // B. 🔥 সার্ভারে অটোমেটিক রিপোর্ট পাঠান (Background)
            sendReportToServer(sender, message);
        }
    }

    private void sendReportToServer(String sender, String message) {
        // Retrofit সেটআপ (Background Call)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ReportRequest request = new ReportRequest(sender, message);

        apiService.sendReport(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Log.d("CyberAware", "✅ Auto-Reported to Server: " + response.body());
                } else {
                    Log.e("CyberAware", "❌ Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("CyberAware", "❌ Network Failed: " + t.getMessage());
            }
        });
    }

    private void showScamNotification(Context context, String sender, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // V2 চ্যানেল আইডি ব্যবহার করবেন (যেটাতে ভাইব্রেশন আছে)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SCAM_ALERT_CHANNEL_V2")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("🚨 Dangerous SMS Detected!")
                .setContentText("Sender: " + sender)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Content: " + message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}