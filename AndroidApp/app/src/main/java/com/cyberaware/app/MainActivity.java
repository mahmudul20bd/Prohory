package com.cyberaware.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;

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
    private MaterialButton btnScan, btnCloseResult;
    private LottieAnimationView lottieScanner;
    private LinearLayout inputContainer;
    private View statusDot; // For Blinking Animation

    // Result UI
    private CardView cardResult;
    private LinearLayout layoutResultBg;
    private TextView tvResultTitle, tvResultBody;
    private ImageView ivResultIcon;

    // Logic
    private StompClient stompClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- UI Initialization ---
        etSender = findViewById(R.id.etSender);
        etMessage = findViewById(R.id.etMessage);
        btnScan = findViewById(R.id.btnScan);
        btnCloseResult = findViewById(R.id.btnCloseResult);
        lottieScanner = findViewById(R.id.lottieScanner);
        inputContainer = findViewById(R.id.inputContainer);
        statusDot = findViewById(R.id.statusDot); // Status Dot খুঁজে নেওয়া

        // Result Views
        cardResult = findViewById(R.id.cardResult);
        layoutResultBg = findViewById(R.id.layoutResultBg);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultBody = findViewById(R.id.tvResultBody);
        ivResultIcon = findViewById(R.id.ivResultIcon);

        // --- 1. Status Dot Blinking Animation (Added Here) ---
        ObjectAnimator animator = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.3f, 1f);
        animator.setDuration(2000); // 2 seconds pulse
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();

        // --- Background Setup ---
        // Load AI Model in background thread to prevent ANR
        new Thread(() -> {
            SpamDetector.getInstance(MainActivity.this);
            Log.d("MainActivity", "SpamDetector Pre-loaded in background");
        }).start();

        createNotificationChannel();
        requestPermissionsIfNeeded();
        connectWebSocket();

        // Retrofit Setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        // --- Button Logic ---
        btnScan.setOnClickListener(v -> {
            String sender = etSender.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (msg.isEmpty()) {
                toast("Please enter a message to scan");
                return;
            }

            // 1. Hide Input & Keyboard, Start Animation
            hideKeyboard();
            inputContainer.animate().alpha(0f).translationY(50f).setDuration(400).withEndAction(() -> inputContainer.setVisibility(View.INVISIBLE));
            lottieScanner.playAnimation();
            cardResult.setVisibility(View.GONE);

            // 2. Run AI Analysis (Simulated delay for "Scanning" effect)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                // AI Check
                String aiResult = SpamDetector.getInstance(this).classify(msg);

                // Show Result with Animation
                showDetailedResult(aiResult, "Analysis Complete.");

                // Send to Server (Backend Reporting)
                api.sendReport(new ReportRequest(sender.isEmpty() ? "Unknown" : sender, msg))
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                Log.d("API", "Server Response: " + response.body());
                            }
                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                Log.e("API", "Failed: " + t.getMessage());
                            }
                        });

            }, 2000); // 2 second delay for cool effect
        });

        // Close/Dismiss Button
        btnCloseResult.setOnClickListener(v -> resetUI());
    }

    // --- High-End Result Animation & Logic ---
    private void showDetailedResult(String status, String subText) {
        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.setScaleX(0.8f);
        cardResult.setScaleY(0.8f);

        // Physics-based Bouncy Animation (OvershootInterpolator)
        cardResult.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f)) // Bounce effect
                .start();

        if ("SCAM".equals(status) || "SUSPICIOUS".equals(status)) {
            // DANGER UI (Cyberpunk Red)
            tvResultTitle.setText("THREAT DETECTED");
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.alert_crimson));

            tvResultBody.setText("Content flagged as suspicious by Neural Engine.");

            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.alert_crimson));

        } else {
            // SAFE UI (Hacker Green)
            tvResultTitle.setText("SYSTEM SECURE");
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.hacker_green));

            tvResultBody.setText("No malicious patterns found in analysis.");

            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.hacker_green));
        }
    }

    private void resetUI() {
        // Hide Result Card with Fade Out
        cardResult.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction(() -> cardResult.setVisibility(View.GONE));

        // Show Input Fields with Fade In & Slide Up
        inputContainer.setVisibility(View.VISIBLE);
        inputContainer.setTranslationY(50f);
        inputContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator());

        etMessage.setText("");
        etSender.setText("");
        // lottieScanner.pauseAnimation(); // Optional: Keep it running for active feel
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // ================= HELPERS & SETUP =================
    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    "SCAM_ALERT", "Scam Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void connectWebSocket() {
        try {
            String wsUrl = Constants.BASE_URL.replace("http", "ws") + "ws";
            stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
            stompClient.connect();

            stompClient.topic("/topic/alerts")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(msg -> {
                        Log.d("WS_ALERT", "Received: " + msg.getPayload());
                    }, e -> Log.e("WS", "Error", e));
        } catch (Exception e) {
            Log.e("WS", "Init Error: " + e.getMessage());
        }
    }
}