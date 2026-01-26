package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

        // --- RETROFIT SETUP ---
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Register Link
        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Login Button
        btnLogin.setOnClickListener(v -> performServerLogin());
    }

    private void performServerLogin() {
        String username = etCodename.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Credentials Required", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- SERVER CALL ---
        AuthRequest request = new AuthRequest(username, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    if (auth.isSuccess()) {
                        // ১. সার্ভার কনফার্ম করলে সেশন সেভ করা
                        SharedPreferences.Editor editor = getSharedPreferences("CyberPrefs", MODE_PRIVATE).edit();
                        editor.putString("username", username);
                        editor.putBoolean("isLoggedIn", true);
                        editor.apply();

                        // ২. ড্যাশবোর্ডে যাওয়া
                        Toast.makeText(LoginActivity.this, "Access Granted", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Access Denied: " + auth.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}