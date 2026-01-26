package com.cyberaware.app;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvHistory = findViewById(R.id.rvHistory);

        btnBack.setOnClickListener(v -> finish());

        // Setup Recycler View
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Mock Data (যেহেতু এখনো হিস্ট্রি API কানেক্ট করা হয়নি)
        // পরবর্তীতে এখানে API কল করে ডেটা আনা হবে
        List<String> mockData = new ArrayList<>();
        // Adapter সেটআপ করতে হবে (আমি শর্টকাটে ডামি লজিক দেখাচ্ছি)
        // HistoryAdapter adapter = new HistoryAdapter(mockData);
        // rvHistory.setAdapter(adapter);
    }
}