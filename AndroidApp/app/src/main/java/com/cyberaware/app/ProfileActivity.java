package com.cyberaware.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // --- LOAD DATA ---
        SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);

        String username = prefs.getString("username", "Unknown");
        String role = prefs.getString("role", "Agent");
        String level = prefs.getString("level", "Level 1");

        // --- UPDATE UI ---
        // XML আপডেট করার পর এই লাইনগুলোর এরর চলে যাবে
        TextView tvUser = findViewById(R.id.tvUsername);
        TextView tvTitle = findViewById(R.id.tvProfileTitle);
        TextView tvClearance = findViewById(R.id.tvClearance);
        ImageView ivAvatar = findViewById(R.id.ivAvatar);

        // নাম সেট করা
        tvUser.setText(username.toUpperCase());

        // টাইটেল ডায়নামিক করা
        tvTitle.setText(role.toUpperCase() + " PROFILE");

        // লেভেল টেক্সট সেট করা
        tvClearance.setText("Clearance Status: " + level);

        // --- ROLE BASED STYLING ---
        if ("Admin".equalsIgnoreCase(role)) {
            tvClearance.setTextColor(ContextCompat.getColor(this, R.color.alert_crimson));
            ivAvatar.setColorFilter(ContextCompat.getColor(this, R.color.alert_crimson));
        } else {
            tvClearance.setTextColor(ContextCompat.getColor(this, R.color.hacker_green));
            ivAvatar.setColorFilter(ContextCompat.getColor(this, R.color.electric_cyan));
        }
    }
}