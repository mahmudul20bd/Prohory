package com.cyberaware.app

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberaware.app.network.DashboardStats
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.network.SystemLog
import com.cyberaware.app.network.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import kotlin.random.Random

class DashboardViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats

    // Baseline is 0.1f (Safe). It will only spike when real data comes from DB or Server.
    private val _liveThreatLevel = MutableStateFlow(0.1f)
    val liveThreatLevel: StateFlow<Float> = _liveThreatLevel

    private val _recentLogs = MutableStateFlow<List<SystemLog>>(emptyList())
    val recentLogs: StateFlow<List<SystemLog>> = _recentLogs

    private var stompClient: StompClient? = null

    init {
        startCoolDownEngine() // Starts the reality-engine to normalize graph after a real threat
    }

    fun loadDashboardData(token: String, username: String, role: String) {
        viewModelScope.launch {
            try {
                val profileResponse = RetrofitClient.apiService.getUserProfile("Bearer $token")
                if (profileResponse.isSuccessful) _userProfile.value = profileResponse.body()

                val statsResponse = RetrofitClient.apiService.getDashboardStats("Bearer $token")
                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()
                    _dashboardStats.value = stats

                    // 🔥 DATABASE DRIVEN LOGIC:
                    // যদি ডেটাবেসে অনেকগুলো ব্লক করা স্ক্যাম থাকে, তবে প্রাথমিক থ্রেট লেভেল একটু হাই থাকবে
                    val recentThreats = stats?.blockedScams ?: 0
                    if (recentThreats > 5) {
                        _liveThreatLevel.value = 0.6f // High alert based on database history
                    }
                }

                val logsResponse = RetrofitClient.apiService.getNeuralLogs("Bearer $token")
                if (logsResponse.isSuccessful && logsResponse.body() != null) {
                    val allLogs = logsResponse.body()!!
                    val filteredLogs = if (role.uppercase() == "ADMIN" || role.uppercase() == "ROOT") {
                        allLogs.reversed()
                    } else {
                        allLogs.filter { it.username == username || it.username == null || it.username == "null" }.reversed()
                    }
                    _recentLogs.value = filteredLogs.take(3)
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "API Error: ${e.message}")
            }
        }
    }

    // 🔥 REAL-TIME SERVER DRIVEN LOGIC
    @SuppressLint("CheckResult")
    fun connectWebSocket() {
        // আপনার Render ব্যাকএন্ডের WebSocket URL (wss://)
        val wsUrl = "wss://cyber-aware.onrender.com/ws/websocket"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.connect()

        stompClient?.topic("/topic/alerts")?.subscribe({ topicMessage ->
            val payload = topicMessage.payload.uppercase()
            Log.d("WebSocket", "Real Threat Received: $payload")

            // ডেটাবেস/সার্ভার থেকে রিয়েল থ্রেট আসলে গ্রাফ স্পাইক করবে
            if (payload.contains("DANGER") || payload.contains("SCAM") || payload.contains("CRITICAL")) {
                _liveThreatLevel.value = 0.95f // Massive Red Spike
            } else if (payload.contains("WARNING")) {
                _liveThreatLevel.value = 0.6f  // Yellow Warning Spike
            } else {
                _liveThreatLevel.value = 0.3f  // Minor event
            }
        }, { error ->
            Log.e("WebSocket", "Connection Error: ", error)
        })
    }

    // 🔥 REALITY COOL-DOWN ENGINE
    // সার্ভার থেকে স্পাইক আসার পর এটি আস্তে আস্তে গ্রাফটিকে নরমাল লেভেলে নিয়ে আসবে।
    // ফলে কোনো ফেক স্পাইক হবে না।
    private fun startCoolDownEngine() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                val current = _liveThreatLevel.value

                if (current > 0.15f) {
                    // Gradually decrease the threat level (Cooldown)
                    _liveThreatLevel.value = current - 0.08f
                } else {
                    // Normal Baseline (Tiny random noise just to show the server is 'alive' and pinging)
                    _liveThreatLevel.value = 0.08f + (Random.nextFloat() * 0.04f)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompClient?.disconnect()
    }
}