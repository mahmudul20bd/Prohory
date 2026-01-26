package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Switches
        SwitchCompat switchProtection = findViewById(R.id.switchProtection);
        SwitchCompat switchNotif = findViewById(R.id.switchNotif);

        // Update Check
        findViewById(R.id.btnUpdate).setOnClickListener(v ->
                Toast.makeText(this, "System is up to date (Patch v1.0.0)", Toast.LENGTH_SHORT).show()
        );

        // Logout Logic (System পেজ থেকেও লগআউট করা যাবে)
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Switch Listeners (Save State)
        SharedPreferences prefs = getSharedPreferences("CyberSettings", MODE_PRIVATE);

        // Load saved state
        switchProtection.setChecked(prefs.getBoolean("protection", true));
        switchNotif.setChecked(prefs.getBoolean("notifications", true));

        // Save on change
        switchProtection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("protection", isChecked).apply();
            String msg = isChecked ? "Protection Modules Activated" : "WARNING: Protection Disabled";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        switchNotif.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("notifications", isChecked).apply()
        );
    }
}