package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText etCodename, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private LinearLayout loginContainer;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI Init
        etCodename = findViewById(R.id.etCodename);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        loginContainer = findViewById(R.id.loginContainer);

        // Animation
        loginContainer.setTranslationY(300f);
        loginContainer.setAlpha(0f);
        loginContainer.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Retrofit Setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Actions
        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        btnLogin.setOnClickListener(v -> performServerLogin());
    }

    private void performServerLogin() {
        String username = etCodename.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Credentials Required", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthRequest request = new AuthRequest(username, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    if (auth.isSuccess()) {
                        // 1. Save Session & Dynamic Profile Data
                        SharedPreferences.Editor editor = getSharedPreferences("CyberPrefs", MODE_PRIVATE).edit();
                        editor.putString("username", auth.getUsername());
                        editor.putBoolean("isLoggedIn", true);

                        // 🔥 নতুন ডাটা সেভ করা হচ্ছে
                        editor.putString("role", auth.getRole() != null ? auth.getRole() : "Agent");
                        editor.putString("level", auth.getClearanceLevel() != null ? auth.getClearanceLevel() : "Level 1");

                        editor.apply();

                        // 2. Navigate to Dashboard
                        Toast.makeText(LoginActivity.this, "Identity Verified: " + auth.getRole(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Access Denied: " + auth.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}