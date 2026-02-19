package com.cyberaware.app

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/report")
    fun sendReport(@Body request: ReportRequest): Call<String>

    @POST("api/auth/register")
    fun register(@Body request: AuthRequest): Call<AuthResponse>

    @POST("api/auth/login")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @GET("api/reports/user")
    fun getUserHistory(@Query("username") username: String): Call<List<Report>>
}