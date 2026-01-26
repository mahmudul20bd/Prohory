package com.cyberaware.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // UI Elements
        LottieAnimationView lottie = findViewById(R.id.lottieSplash);
        TextView tvBrand = findViewById(R.id.tvBrand);
        TextView tvTagline = findViewById(R.id.tvTagline);

        // Animation Logic
        tvBrand.animate().alpha(1f).translationY(0).setDuration(1000).setStartDelay(300).start();
        tvTagline.animate().alpha(1f).translationY(0).setDuration(1000).setStartDelay(500).start();

        // 3 সেকেন্ড পর অনবোর্ডিং পেজে নিয়ে যাবে
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Updated: Redirect to Onboarding instead of Main
            Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish(); // স্প্ল্যাশ স্ক্রিন বন্ধ করে দেওয়া যাতে ব্যাক করলে আর না আসে
        }, 3000);
    }
}