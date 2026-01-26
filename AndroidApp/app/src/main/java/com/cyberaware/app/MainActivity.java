package com.cyberaware.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

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

    // --- UI Components ---
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageView ivSettings; // Menu Trigger

    private EditText etSender, etMessage;
    private MaterialButton btnScan, btnCloseResult;
    private LottieAnimationView lottieScanner;
    private LinearLayout inputContainer;
    private View statusDot; // For Blinking Animation

    // Result UI
    private CardView cardResult;
    private TextView tvResultTitle, tvResultBody;
    private ImageView ivResultIcon;

    // Logic
    private StompClient stompClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- 1. Init Views ---
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        ivSettings = findViewById(R.id.ivSettings); // Settings icon acts as Menu button

        etSender = findViewById(R.id.etSender);
        etMessage = findViewById(R.id.etMessage);
        btnScan = findViewById(R.id.btnScan);
        btnCloseResult = findViewById(R.id.btnCloseResult);
        lottieScanner = findViewById(R.id.lottieScanner);
        inputContainer = findViewById(R.id.inputContainer);
        statusDot = findViewById(R.id.statusDot);

        cardResult = findViewById(R.id.cardResult);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultBody = findViewById(R.id.tvResultBody);
        ivResultIcon = findViewById(R.id.ivResultIcon);

        // --- 2. Dynamic Header Setup (Fixing Agent 007) ---
        // নেভিগেশন হেডারে ইউজারের আসল নাম লোড করা হচ্ছে
        View headerView = navView.getHeaderView(0);
        TextView tvNavUsername = headerView.findViewById(R.id.tvNavUsername);
        TextView tvNavLevel = headerView.findViewById(R.id.tvNavLevel);

        SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("username", "Unknown Agent");

        tvNavUsername.setText(savedUsername); // ডায়নামিক নাম সেট
        tvNavLevel.setText("Level 5 Clearance"); // এটা আপাতত স্ট্যাটিক

        // --- 3. Navigation Drawer Logic ---
        ivSettings.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                // Logout: সেশন ক্লিয়ার করে লগইন পেজে পাঠানো
                prefs.edit().clear().apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_history) {
                // Open History Page
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            } else if (id == R.id.nav_profile) {
                toast("Access Denied: Restricted Area");
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // --- 4. Status Dot Blinking Animation ---
        ObjectAnimator animator = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.3f, 1f);
        animator.setDuration(2000); // 2 seconds pulse
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();

        // --- 5. Background Setup ---
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

        // --- 6. Scan Button Logic ---
        btnScan.setOnClickListener(v -> {
            String sender = etSender.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (msg.isEmpty()) {
                toast("Input Error: No data to analyze");
                return;
            }

            // Hide Input & Keyboard, Start Animation
            hideKeyboard();
            inputContainer.animate().alpha(0f).translationY(50f).setDuration(400).withEndAction(() -> inputContainer.setVisibility(View.INVISIBLE));
            lottieScanner.playAnimation();
            cardResult.setVisibility(View.GONE);

            // Run AI Analysis (Simulated "Scanning" delay)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                // AI Check
                String aiResult = SpamDetector.getInstance(this).classify(msg);

                // Show Result
                showDetailedResult(aiResult);

                // Backend Reporting
                api.sendReport(new ReportRequest(sender.isEmpty() ? "Unknown" : sender, msg))
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                Log.d("API", "Report Logged: " + response.body());
                            }
                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                Log.e("API", "Upload Failed: " + t.getMessage());
                            }
                        });

            }, 2000); // 2 second delay for cool effect
        });

        // Close Result Logic
        btnCloseResult.setOnClickListener(v -> resetUI());
    }

    // --- Result Animation & Logic ---
    private void showDetailedResult(String status) {
        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.setScaleX(0.8f);
        cardResult.setScaleY(0.8f);

        // Bouncy Animation
        cardResult.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        if ("SCAM".equals(status) || "SUSPICIOUS".equals(status)) {
            // DANGER UI
            tvResultTitle.setText("THREAT DETECTED");
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.alert_crimson));

            tvResultBody.setText("Content flagged as suspicious by Neural Engine.");

            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.alert_crimson));

        } else {
            // SAFE UI
            tvResultTitle.setText("SYSTEM SECURE");
            tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.hacker_green));

            tvResultBody.setText("No malicious patterns found in analysis.");

            ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
            ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.hacker_green));
        }
    }

    private void resetUI() {
        cardResult.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction(() -> cardResult.setVisibility(View.GONE));

        inputContainer.setVisibility(View.VISIBLE);
        inputContainer.setTranslationY(50f);
        inputContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator());

        etMessage.setText("");
        etSender.setText("");
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // ================= HELPERS =================
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