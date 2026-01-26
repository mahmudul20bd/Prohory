package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --- UI Initialization ---
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        ImageView ivMenu = findViewById(R.id.ivMenu);
        TextView tvUsername = findViewById(R.id.tvUsername);

        // --- 1. Load User Info ---
        SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Agent");
        tvUsername.setText(username.toUpperCase());

        // ড্রয়ারের হেডারেও নাম সেট করা
        View headerView = navView.getHeaderView(0);
        TextView navUser = headerView.findViewById(R.id.tvNavUsername);
        if (navUser != null) {
            navUser.setText(username);
        }

        // --- 2. Drawer Navigation Logic ---
        // মেনু আইকনে চাপলে ড্রয়ার খুলবে
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // ড্রয়ারের আইটেম ক্লিক হ্যান্ডেলিং
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                // Logout Logic
                prefs.edit().clear().apply();
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // --- 3. Dashboard Grid Buttons ---

        // Button: Scanner
        findViewById(R.id.btnNewScan).setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, MainActivity.class)));

        // Button: Logs (History)
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class)));

        // Button: Profile (New Page)
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));

        // 4. SYSTEM BUTTON CLICK (Settings)
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class))
        );
    }
}