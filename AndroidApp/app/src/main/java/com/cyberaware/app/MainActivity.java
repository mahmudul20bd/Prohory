package com.cyberaware.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private EditText etSender, etMessage;
    private Button btnScan;
    private TextView tvResult;

    // AI Engine
    private SpamDetector spamDetector;

    // WebSocket Client
    private StompClient mStompClient;

    // Server URL (আপনার আইপি এবং পোর্ট ঠিক আছে তো?)
    private static final String BASE_URL = "http://192.168.0.104:8080/";

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. View Initialization
        etSender = findViewById(R.id.etSender);
        etMessage = findViewById(R.id.etMessage);
        btnScan = findViewById(R.id.btnScan);
        tvResult = findViewById(R.id.tvResult);

        // 2. Initialize AI Engine
        spamDetector = new SpamDetector(this);

        // 🔥 3. Notification Channel Create (Must for Android 8+)
        createNotificationChannel();

        // 🔥 4. Permission Logic (Updated for Android 13+)
        checkAndRequestPermissions();

        // 🔥 5. Initialize WebSocket (Real-time Alert Listener)
        connectToWebSocket();

        // 6. Setup Retrofit (Dual Converter)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 7. Button Logic
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sender = etSender.getText().toString().trim();
                String message = etMessage.getText().toString().trim();

                if (sender.isEmpty() || message.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // AI Check (Offline)
                String localResult = spamDetector.classify(message);
                tvResult.setText("🤖 AI Check (Offline): " + localResult + "\n\n☁️ Server: Syncing...");

                // Send to Server
                btnScan.setEnabled(false);
                ReportRequest request = new ReportRequest(sender, message);

                apiService.sendReport(request).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        btnScan.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            String serverReply = response.body();
                            tvResult.setText("🤖 AI Check: " + localResult + "\n☁️ Server Says: " + serverReply);
                        } else {
                            tvResult.setText("🤖 AI Check: " + localResult + "\n☁️ Server Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        btnScan.setEnabled(true);
                        tvResult.setText("🤖 AI Check: " + localResult + "\n☁️ Server: Offline/Failed (" + t.getMessage() + ")");
                        Log.e("CyberAware", "Network Error: " + t.getMessage());
                    }
                });
            }
        });
    }

    // ================= HELPER METHODS =================

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (SMS + Notification)
            if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.POST_NOTIFICATIONS
                }, 101);
            }
        } else {
            // Older Android (Only SMS)
            if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, 101);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔥 ID পরিবর্তন করেছি (V2) যাতে নতুন সেটিংস কাজ করে
            String channelId = "SCAM_ALERT_CHANNEL_V2";
            CharSequence name = "Scam Alerts";
            String description = "Notifications for detected scam messages";

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            // 🔥 ভাইব্রেশন সেটিংস (Force Vibration)
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500}); // থামবে, কাঁপবে, থামবে, কাঁপবে

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("CheckResult")
    private void connectToWebSocket() {
        // IP Address (Check cmd -> ipconfig)
        String wsUrl = "ws://192.168.0.104:8080/ws";
        Log.d("WebSocket", "Connecting to: " + wsUrl);

        if (mStompClient != null) {
            mStompClient.disconnect();
        }

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.d("WebSocket", "✅ Connection Opened");
                            showToast("🟢 Connected to Server");
                            break;
                        case ERROR:
                            Log.e("WebSocket", "❌ Connection Error", lifecycleEvent.getException());
                            showToast("🔴 Connection Failed");
                            break;
                        case CLOSED:
                            Log.d("WebSocket", "⚠️ Connection Closed");
                            break;
                    }
                });

        mStompClient.connect();

        mStompClient.topic("/topic/alerts")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d("WebSocket", "📩 Alert: " + topicMessage.getPayload());
                    showToast("⚠️ " + topicMessage.getPayload());
                }, throwable -> {
                    Log.e("WebSocket", "Subscription Error", throwable);
                });
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStompClient != null) {
            mStompClient.disconnect();
        }
    }
}