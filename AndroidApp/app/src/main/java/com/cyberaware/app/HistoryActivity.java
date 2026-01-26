package com.cyberaware.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ImageView btnBack = findViewById(R.id.btnBack);
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        // ইউজারের নাম এবং রোল আনা
        SharedPreferences prefs = getSharedPreferences("CyberPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // এখানে ভবিষ্যতে আমরা চেক করব ইউজার Admin কিনা
        // আপাতত ইউজারের পার্সোনাল হিস্ট্রি লোড করছি
        if (!username.isEmpty()) {
            fetchHistory(username);
        } else {
            Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchHistory(String username) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        api.getUserHistory(username).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Report> reports = response.body();

                    // 🔥 FIX: এখানে Context (HistoryActivity.this) পাস করা হয়েছে
                    adapter = new HistoryAdapter(HistoryActivity.this, reports);
                    rvHistory.setAdapter(adapter);

                    if(reports.isEmpty()){
                        Toast.makeText(HistoryActivity.this, "No Scan Logs Found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Server Returned Empty", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                Toast.makeText(HistoryActivity.this, "Failed to load logs: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}