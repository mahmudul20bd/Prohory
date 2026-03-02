package com.cyberaware.app


data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null
)


data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val username: String?,
    val agentId: String?,
    val role: String? = "User"
)


data class User(
    val id: String,
    val name: String,
    val email: String
)


data class ReportRequest(
    val type: String,
    val description: String,
    val userId: String
)


data class Report(
    val id: String,
    val type: String,
    val date: String,
    val status: String,
    val description: String
)

