package com.cyberaware.app;

import java.util.List; // 🔥 এই লাইনটি যোগ করা হয়েছে

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;   // 🔥 এই লাইনটি যোগ করা হয়েছে
import retrofit2.http.POST;
import retrofit2.http.Query; // 🔥 এই লাইনটি যোগ করা হয়েছে

public interface ApiService {

    // ১. রিপোর্ট পাঠানোর মেথড
    @POST("api/report")
    Call<String> sendReport(@Body ReportRequest request);

    // ২. রেজিস্ট্রেশন
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body AuthRequest request);

    // ৩. লগইন (আপনার কোডে এটি মিসিং ছিল, তাই আবার দিয়ে দিলাম)
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    // ৪. স্ক্যান হিস্ট্রি আনা (এখন আর এরর দেখাবে না)
    @GET("api/reports/user")
    Call<List<Report>> getUserHistory(@Query("username") String username);
}