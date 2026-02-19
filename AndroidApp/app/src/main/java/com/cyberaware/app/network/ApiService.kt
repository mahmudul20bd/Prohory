package com.cyberaware.app.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

// ড্যাশবোর্ডের ডেটা মডেল
data class DashboardStats(
    val totalScans: Int,
    val blockedScams: Int,
    val safeLinks: Int
)

data class UserProfile(
    val username: String,
    val role: String,
    val clearanceLevel: String
)

interface ApiService {

    // ইউজারের প্রোফাইল ফেচ করার জন্য
    @GET("/api/users/me")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    // ড্যাশবোর্ডের স্ট্যাটস ফেচ করার জন্য
    @GET("/api/reports/dashboard-stats")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): Response<DashboardStats>
}