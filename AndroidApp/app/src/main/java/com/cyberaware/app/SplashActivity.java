package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // ১. চেক করা ইউজার আগে লগইন ছিল কিনা
            SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

            Intent intent;
            if (isLoggedIn) {
                // ২. লগইন থাকলে সরাসরি ড্যাশবোর্ডে
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            } else {
                // ৩. না থাকলে অনবোর্ডিং-এ
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }

            startActivity(intent);
            finish();
        }, 3000); // 3 seconds delay
    }
}