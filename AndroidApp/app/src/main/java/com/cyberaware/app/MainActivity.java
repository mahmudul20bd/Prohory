package com.cyberaware.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

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

    private EditText etSender, etMessage;
    private MaterialButton btnScan;

    private CardView cardResult;
    private LinearLayout layoutResultBg;
    private TextView tvResultTitle, tvResultBody;
    private ImageView ivResultIcon;

    private SpamDetector spamDetector;
    private StompClient stompClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        etSender = findViewById(R.id.etSender);
        etMessage = findViewById(R.id.etMessage);
        btnScan = findViewById(R.id.btnScan);

        cardResult = findViewById(R.id.cardResult);
        layoutResultBg = findViewById(R.id.layoutResultBg);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultBody = findViewById(R.id.tvResultBody);
        ivResultIcon = findViewById(R.id.ivResultIcon);

        spamDetector = new SpamDetector(this);
        createNotificationChannel();
        requestPermissionsIfNeeded();
        connectWebSocket();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        btnScan.setOnClickListener(v -> {
            String sender = etSender.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();

            if (sender.isEmpty() || msg.isEmpty()) {
                toast("Fill all fields");
                return;
            }

            String aiResult = spamDetector.classify(msg);
            showResult(aiResult, "🤖 AI Result: " + aiResult);

            btnScan.setEnabled(false);
            btnScan.setText("Scanning...");

            api.sendReport(new ReportRequest(sender, msg))
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            btnScan.setEnabled(true);
                            btnScan.setText("CHECK FOR SCAM");

                            if ("SCAM".equals(aiResult) || "SUSPICIOUS".equals(aiResult)) {
                                showResult(aiResult, "⚠️ AI Warning\nServer: " + response.body());
                            } else {
                                showResult("SAFE", "✅ AI Verified Safe\nServer: " + response.body());
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            btnScan.setEnabled(true);
                            btnScan.setText("CHECK FOR SCAM");
                            showResult(aiResult, "Server Offline\nShowing AI Result");
                        }
                    });
        });
    }

    // ================= RESULT UI =================
    private void showResult(String status, String body) {
        cardResult.setVisibility(View.VISIBLE);

        if ("SCAM".equals(status) || "SUSPICIOUS".equals(status)) {
            layoutResultBg.setBackgroundColor(getColor(R.color.danger_red));
            tvResultTitle.setText("⚠️ SCAM / SUSPICIOUS");
        } else {
            layoutResultBg.setBackgroundColor(getColor(R.color.safe_green));
            tvResultTitle.setText("✅ SAFE MESSAGE");
        }
        tvResultBody.setText(body);
        ivResultIcon.setImageResource(R.drawable.baseline_shield_24);
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
        String wsUrl = Constants.BASE_URL.replace("http", "ws") + "ws";
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        stompClient.connect();

        stompClient.topic("/topic/alerts")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(msg -> toast("⚠️ " + msg.getPayload()),
                        e -> Log.e("WS", "Error", e));
    }
}
