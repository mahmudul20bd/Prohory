package com.cyberaware.app;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // ১. আগের রিপোর্ট পাঠানোর মেথড (এটি যেমন ছিল তেমনই আছে)
    @POST("api/report")
    Call<String> sendReport(@Body ReportRequest request);

    // ২. নতুন: রেজিস্ট্রেশন (Spring Boot Backend এর সাথে কানেক্ট হবে)
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body AuthRequest request);

    // ৩. নতুন: লগইন (Spring Boot Backend এর সাথে কানেক্ট হবে)
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);
}