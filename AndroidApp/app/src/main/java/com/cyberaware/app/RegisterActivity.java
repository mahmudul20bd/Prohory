package com.cyberaware.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    private EditText etCodename, etPass, etConfirmPass;
    private MaterialButton btnRegister;
    private ImageView btnBack;
    private ApiService apiService; // Retrofit API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // UI Init
        etCodename = findViewById(R.id.etNewCodename);
        etPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        // --- RETROFIT SETUP ---
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL) // আপনার সার্ভার URL (Constants.java তে আছে)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        btnBack.setOnClickListener(v -> finish());

        // --- REGISTER ACTION ---
        btnRegister.setOnClickListener(v -> performServerRegistration());
    }

    private void performServerRegistration() {
        String username = etCodename.getText().toString().trim();
        String p1 = etPass.getText().toString().trim();
        String p2 = etConfirmPass.getText().toString().trim();

        // ভ্যালিডেশন
        if(username.isEmpty() || p1.isEmpty()) {
            Toast.makeText(this, "Protocol Error: All fields required", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!p1.equals(p2)) {
            Toast.makeText(this, "Security Alert: Keys do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- SERVER CALL ---
        AuthRequest request = new AuthRequest(username, p1);

        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    if (auth.isSuccess()) {
                        // ১. সার্ভারে সফল হলে লোকালে নাম সেভ করব (পরের বার ব্যবহারের জন্য)
                        SharedPreferences.Editor editor = getSharedPreferences("CyberPrefs", MODE_PRIVATE).edit();
                        editor.putString("username", username);
                        editor.putBoolean("isLoggedIn", true);
                        editor.apply();

                        // ২. সাকসেস স্ক্রিনে পাঠানো
                        Intent intent = new Intent(RegisterActivity.this, SuccessActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + auth.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("RegisterAPI", t.getMessage());
            }
        });
    }
}