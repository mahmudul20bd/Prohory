package com.cyberaware.app.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

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

// 🔥 FIX 1: ডিফল্ট (= 0) সরিয়ে Int? দেওয়া হয়েছে। এখন ব্যাকএন্ডের আসল ডেটা ঠিকমতো ম্যাপ হবে।
data class DashboardStats(
    @SerializedName("totalScans") val totalScans: Int?,
    @SerializedName("blockedScams") val blockedScams: Int?,
    @SerializedName("safeLinks") val safeLinks: Int?
)

data class UserProfile(
    val username: String,
    val role: String,
    val clearanceLevel: String
)

data class SystemLog(
    val id: Long?,
    @SerializedName("action") val actionType: String,
    // 🔥 FIX 2: ডাটাবেসে null থাকলে যেন অ্যাপ ক্র্যাশ না করে তাই String? দেওয়া হলো
    @SerializedName("performedBy") val username: String?,
    val details: String,
    val timestamp: String?
)

data class SpamResponse(
    val isSpam: Boolean,
    val threatLevel: String? = null,
    val message: String? = null
)

data class ScanTextRequest(
    val message: String,
    val username: String,
    val region: String? = null  // 🔥 Backend e pathanor jonno region add kora holo
)

data class ScanResponse(
    val result: String,
    val extractedText: String? = null
)

data class ReportModel(
    val id: Long?,
    val sender: String?,
    val message: String?,
    val aiResult: String?,
    val createdAt: String?
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

    @POST("/api/scan/sms")
    suspend fun checkSpamOnline(
        @Header("Authorization") token: String,
        @Body payload: Map<String, String>
    ): Response<SpamResponse>

    @POST("/api/scan/text")
    suspend fun scanText(@Body request: ScanTextRequest): Response<ScanResponse>

    // 🔥 scanImage API তে region অ্যাড করা হলো
    @Multipart
    @POST("/api/scan/image")
    suspend fun scanImage(
        @Part file: MultipartBody.Part,
        @Part("username") username: RequestBody,
        @Part("localText") localText: RequestBody?,
        @Part("region") region: RequestBody? // নতুন প্যারামিটার
    ): Response<ScanResponse>

    @GET("/api/reports/user")
    suspend fun getUserReports(
        @Header("Authorization") token: String,
        @Query("username") username: String
    ): Response<List<ReportModel>>
}