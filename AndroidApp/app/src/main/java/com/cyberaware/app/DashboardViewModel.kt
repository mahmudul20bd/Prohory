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
import ua.naiksoftware.stomp.dto.StompHeader

class DashboardViewModel : ViewModel() {

    private val _sessionExpired = MutableSharedFlow<Boolean>(replay = 1)
    val sessionExpired: SharedFlow<Boolean> = _sessionExpired.asSharedFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats

    private val _liveThreatLevel = MutableStateFlow<Float?>(null)
    val liveThreatLevel: StateFlow<Float?> = _liveThreatLevel

    private val _recentLogs = MutableStateFlow<List<SystemLog>>(emptyList())
    val recentLogs: StateFlow<List<SystemLog>> = _recentLogs

    private var stompClient: StompClient? = null
    private var coolDownStarted = false

    private fun ensureCoolDownEngine() {
        if (coolDownStarted) return
        coolDownStarted = true
        startCoolDownEngine()
    }

    fun loadDashboardData(token: String, username: String, role: String) {
        viewModelScope.launch {
            try {
                // ১. Profile Check
                val profileResponse = RetrofitClient.apiService.getUserProfile("Bearer $token")
                // 🔥 FIX: 401 (Unauthorized) এবং 403 (Forbidden) দুটোই চেক করা হলো
                if (profileResponse.code() == 401 || profileResponse.code() == 403) {
                    _sessionExpired.emit(true)
                    return@launch
                }
                if (profileResponse.isSuccessful) _userProfile.value = profileResponse.body()

                // ২. Stats Check
                val statsResponse = RetrofitClient.apiService.getDashboardStats("Bearer $token")
                if (statsResponse.code() == 401 || statsResponse.code() == 403) {
                    _sessionExpired.emit(true)
                    return@launch
                }
                if (statsResponse.isSuccessful) {
                    _dashboardStats.value = statsResponse.body()
                    // Stats load hobar por engine ensure kora hobe kintu spike hobe na
                    ensureCoolDownEngine()
                }

                // ৩. Logs Check
                val logsResponse = RetrofitClient.apiService.getNeuralLogs("Bearer $token")
                if (logsResponse.code() == 401 || logsResponse.code() == 403) {
                    _sessionExpired.emit(true)
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

                // 🔥 FIX: Retrofit যদি সরাসরি Exception মারে (Token Expired এর কারণে), তাহলেও ইউজারকে লগআউট করবে
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                    _sessionExpired.emit(true)
                }
            }
        }
    }
    @SuppressLint("CheckResult")
    fun connectWebSocket(token: String) {
        val wsUrl = "wss://cyber-aware.onrender.com/ws/websocket"
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val headers = listOf(StompHeader("Authorization", authHeader))

        stompClient?.connect(headers)
        ensureCoolDownEngine()

        stompClient?.topic("/topic/alerts")?.subscribe({ topicMessage ->
            val payload = topicMessage.payload.uppercase()
            Log.d("WebSocket", "Real Threat Received: $payload")

            // 🔥 WEB SOCKET THEKE THREAT ASLEI SPIKE HOBE
            if (payload.contains("DANGER") || payload.contains("SCAM") || payload.contains("CRITICAL")) {
                _liveThreatLevel.value = 0.98f
            } else if (payload.contains("WARNING") || payload.contains("SPAM")) {
                _liveThreatLevel.value = 0.65f
            }
        }, { error ->
            Log.e("WebSocket", "Connection Error: ", error)
        })
    }

    private fun startCoolDownEngine() {
        viewModelScope.launch {
            while (true) {
                delay(1200)
                val current = _liveThreatLevel.value ?: 0.1f

                // যখনি স্পাইক শেষ হবে, এটি দ্রুত ০.১ এ ফিরে আসবে
                if (current > 0.15f) {
                    _liveThreatLevel.value = current - 0.3f // Fast drop for sharp spike
                } else {
                    _liveThreatLevel.value = 0.1f
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompClient?.disconnect()
    }
}