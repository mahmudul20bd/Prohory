package com.cyberaware.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // নতুন লেআউট সেট করা হচ্ছে
        setContentView(R.layout.activity_success);

        // 3 সেকেন্ড (3000ms) পর অটোমেটিক লগইন পেজে নিয়ে যাবে
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SuccessActivity.this, LoginActivity.class);
            // ইউজার ব্যাক বাটনে চাপলে যাতে আবার এই সাকসেস পেজে ফিরে না আসে
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 3000);
    }
}