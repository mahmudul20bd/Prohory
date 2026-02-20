package com.cyberaware.app.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val agentId: String?
)

data class AuthResponse(
    val success: Boolean,
    val token: String,
    val username: String,
    val agentId: String?,
    val message: String,
    val role: String?,
    val clearanceLevel: String?
)

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

// 🔥 JSON Mapping ফিক্স করা হয়েছে
data class SystemLog(
    val id: Long?,
    @SerializedName("action") val actionType: String,
    @SerializedName("performedBy") val username: String,
    val details: String,
    val timestamp: String?
)

interface ApiService {
    @POST("/api/auth/register")
    suspend fun registerUser(@Body payload: Map<String, String>): Response<RegisterResponse>

    @POST("/api/auth/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<AuthResponse>

    @GET("/api/users/me")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>

    @GET("/api/reports/dashboard-stats")
    suspend fun getDashboardStats(@Header("Authorization") token: String): Response<DashboardStats>

    @POST("/api/report")
    suspend fun submitSmsReport(@Header("Authorization") token: String, @Body payload: Map<String, String>): Response<Any>

    @GET("/api/logs")
    suspend fun getNeuralLogs(@Header("Authorization") token: String): Response<List<SystemLog>>
}