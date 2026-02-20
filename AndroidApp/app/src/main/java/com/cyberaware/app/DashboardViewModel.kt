package com.cyberaware.app

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberaware.app.network.DashboardStats
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.network.SystemLog
import com.cyberaware.app.network.UserProfile
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

    private val _liveThreatLevel = MutableStateFlow(0.2f)
    val liveThreatLevel: StateFlow<Float> = _liveThreatLevel

    // 🔥 ড্যাশবোর্ডের রিসেন্ট লগের জন্য State
    private val _recentLogs = MutableStateFlow<List<SystemLog>>(emptyList())
    val recentLogs: StateFlow<List<SystemLog>> = _recentLogs

    private var stompClient: StompClient? = null

    // 🔥 এখন এটি username এবং role ও রিসিভ করবে ফিল্টার করার জন্য
    fun loadDashboardData(token: String, username: String, role: String) {
        viewModelScope.launch {
            try {
                val profileResponse = RetrofitClient.apiService.getUserProfile("Bearer $token")
                if (profileResponse.isSuccessful) _userProfile.value = profileResponse.body()

                val statsResponse = RetrofitClient.apiService.getDashboardStats("Bearer $token")
                if (statsResponse.isSuccessful) _dashboardStats.value = statsResponse.body()

                // 🔥 Neural Logs থেকে ডাটা নিয়ে ড্যাশবোর্ডে পাঠানো
                val logsResponse = RetrofitClient.apiService.getNeuralLogs("Bearer $token")
                if (logsResponse.isSuccessful && logsResponse.body() != null) {
                    val allLogs = logsResponse.body()!!
                    val filteredLogs = if (role.uppercase() == "ADMIN") {
                        allLogs.reversed()
                    } else {
                        allLogs.filter { it.username == username }.reversed()
                    }
                    _recentLogs.value = filteredLogs.take(3) // শুধুমাত্র টপ ৩টি লগ ড্যাশবোর্ডে দেখাবে
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "API Error: ${e.message}")
            }
        }
    }

    @SuppressLint("CheckResult")
    fun connectWebSocket() {
        // আপনার লোকাল বা Render এর WebSocket URL
        val wsUrl = "ws://10.0.2.2:9090/ws/websocket"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.connect()

        stompClient?.topic("/topic/alerts")?.subscribe({ topicMessage ->
            val payload = topicMessage.payload.uppercase()
            val isDanger = payload.contains("DANGER") || payload.contains("SCAM") || payload.contains("CRITICAL")

            val nextThreat = if (isDanger) 0.8f + Random.nextFloat() * 0.2f else 0.2f + Random.nextFloat() * 0.2f
            _liveThreatLevel.value = nextThreat
        }, { error ->
            Log.e("WebSocket", "Connection Error: ", error)
        })
    }

    override fun onCleared() {
        super.onCleared()
        stompClient?.disconnect()
    }
}