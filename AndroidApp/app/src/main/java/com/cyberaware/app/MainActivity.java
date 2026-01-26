package com.cyberaware.app;

import android.Manifest;
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

    // Result UI
    private CardView cardResult;
    private LinearLayout layoutResultBg;
    private TextView tvResultTitle, tvResultBody;
    private ImageView ivResultIcon;

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

        cardResult = findViewById(R.id.cardResult);
        layoutResultBg = findViewById(R.id.layoutResultBg);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultBody = findViewById(R.id.tvResultBody);
        ivResultIcon = findViewById(R.id.ivResultIcon);

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
            inputContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> inputContainer.setVisibility(View.INVISIBLE));
            lottieScanner.playAnimation();
            cardResult.setVisibility(View.GONE);

            // 2. Run AI Analysis (Simulated delay for effect)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                // AI Check
                String aiResult = SpamDetector.getInstance(this).classify(msg);

                // Show Result on UI
                showDetailedResult(aiResult, "Analysis Complete.");

                // Send to Server (Backend Reporting)
                api.sendReport(new ReportRequest(sender.isEmpty() ? "Unknown" : sender, msg))
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                // Server feedback can be appended if needed
                                Log.d("API", "Server Response: " + response.body());
                            }
                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                Log.e("API", "Failed: " + t.getMessage());
                            }
                        });

            }, 2000); // 2 second cool scanning effect
        });

        btnCloseResult.setOnClickListener(v -> resetUI());
    }

    private void showDetailedResult(String status, String subText) {
     //   lottieScanner.pauseAnimation();
        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.setScaleX(0.8f);
        cardResult.setScaleY(0.8f);

        // Pop-up animation for result card
        cardResult.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        if ("SCAM".equals(status) || "SUSPICIOUS".equals(status)) {
            // Danger UI
            layoutResultBg.setBackgroundColor(getColor(R.color.danger_red));
            tvResultTitle.setText("⚠️ THREAT DETECTED");
            tvResultBody.setText("This message contains suspicious patterns (Phishing/Spam).");
            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
        } else {
            // Safe UI
            layoutResultBg.setBackgroundColor(getColor(R.color.safe_green));
            tvResultTitle.setText("✅ SYSTEM SAFE");
            tvResultBody.setText("No malicious content detected by Neural Engine.");
            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
        }
    }

    private void resetUI() {
        cardResult.animate().alpha(0f).setDuration(300).withEndAction(() -> cardResult.setVisibility(View.GONE));
        inputContainer.setVisibility(View.VISIBLE);
        inputContainer.animate().alpha(1f).setDuration(300);

        etMessage.setText("");
        etSender.setText("");
        lottieScanner.setProgress(0f);
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