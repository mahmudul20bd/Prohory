package com.cyberaware.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ScanDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_detail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get Data from Intent
        String sender = getIntent().getStringExtra("sender");
        String message = getIntent().getStringExtra("message");
        String status = getIntent().getStringExtra("status");
        String time = getIntent().getStringExtra("time");

        // Init Views
        TextView tvSender = findViewById(R.id.tvDetailSender);
        TextView tvMessage = findViewById(R.id.tvDetailMessage);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvTime = findViewById(R.id.tvDetailTime);

        // Set Data
        tvSender.setText(sender);
        tvMessage.setText(message);
        tvStatus.setText(status);
        tvTime.setText(time != null ? time : "Just Now");

        // Color Logic
        if ("SCAM".equals(status) || "SUSPICIOUS".equals(status)) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.alert_crimson));
            tvStatus.setText("THREAT DETECTED");
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.hacker_green));
            tvStatus.setText("SYSTEM SECURE");
        }
    }
}