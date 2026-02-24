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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader // 🔥 NEW IMPORT
import kotlin.random.Random

class DashboardViewModel : ViewModel() {

    // 🔥 THE MAGIC FIX: replay = 1 ensures the logout signal is NEVER missed by the UI
    private val _sessionExpired = MutableSharedFlow<Boolean>(replay = 1)
    val sessionExpired: SharedFlow<Boolean> = _sessionExpired.asSharedFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats

    private val _liveThreatLevel = MutableStateFlow(0.1f)
    val liveThreatLevel: StateFlow<Float> = _liveThreatLevel

    private val _recentLogs = MutableStateFlow<List<SystemLog>>(emptyList())
    val recentLogs: StateFlow<List<SystemLog>> = _recentLogs

    private var stompClient: StompClient? = null

    init {
        startCoolDownEngine()
    }

    fun loadDashboardData(token: String, username: String, role: String) {
        viewModelScope.launch {
            try {
                // ১. Profile Check
                val profileResponse = RetrofitClient.apiService.getUserProfile("Bearer $token")
                if (profileResponse.code() == 401) {
                    _sessionExpired.emit(true) // 🔥 Token Expired! Trigger Logout
                    return@launch
                }
                if (profileResponse.isSuccessful) _userProfile.value = profileResponse.body()

                // ২. Stats Check
                val statsResponse = RetrofitClient.apiService.getDashboardStats("Bearer $token")
                if (statsResponse.code() == 401) {
                    _sessionExpired.emit(true) // 🔥 Token Expired! Trigger Logout
                    return@launch
                }
                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()
                    _dashboardStats.value = stats

                    val recentThreats = stats?.blockedScams ?: 0
                    if (recentThreats > 5) {
                        _liveThreatLevel.value = 0.6f
                    }
                }

                // ৩. Logs Check
                val logsResponse = RetrofitClient.apiService.getNeuralLogs("Bearer $token")
                if (logsResponse.code() == 401) {
                    _sessionExpired.emit(true) // 🔥 Token Expired! Trigger Logout
                    return@launch
                }
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

    @SuppressLint("CheckResult")
    fun connectWebSocket(token: String) { // 🔥 FIX: Receive token to authenticate WebSocket
        val wsUrl = "wss://cyber-aware.onrender.com/ws/websocket"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

        // Send Token in WebSocket Connection Header to stop backend rejection spam
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val headers = listOf(StompHeader("Authorization", authHeader))

        stompClient?.connect(headers)

        stompClient?.topic("/topic/alerts")?.subscribe({ topicMessage ->
            val payload = topicMessage.payload.uppercase()
            Log.d("WebSocket", "Real Threat Received: $payload")

            if (payload.contains("DANGER") || payload.contains("SCAM") || payload.contains("CRITICAL")) {
                _liveThreatLevel.value = 0.95f
            } else if (payload.contains("WARNING")) {
                _liveThreatLevel.value = 0.6f
            } else {
                _liveThreatLevel.value = 0.3f
            }
        }, { error ->
            Log.e("WebSocket", "Connection Error: ", error)
        })
    }

    private fun startCoolDownEngine() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _liveThreatLevel.value

                if (current > 0.15f) {
                    _liveThreatLevel.value = current - 0.08f
                } else {
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