package com.cyberaware.app

// ১. লগইন এবং রেজিস্ট্রেশনের জন্য রিকোয়েস্ট
data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null // রেজিস্ট্রেশনে নাম লাগবে, লগইনে লাগবে না
)

// ২. সার্ভার থেকে আসা রেসপন্স
data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val user: User?
)

// ৩. ইউজার ডাটা
data class User(
    val id: String,
    val name: String,
    val email: String
)

// ৪. রিপোর্ট পাঠানোর রিকোয়েস্ট
data class ReportRequest(
    val type: String,
    val description: String,
    val userId: String
)

// ৫. রিপোর্ট হিস্ট্রি ডাটা
data class Report(
    val id: String,
    val type: String,
    val date: String,
    val status: String,
    val description: String
)

