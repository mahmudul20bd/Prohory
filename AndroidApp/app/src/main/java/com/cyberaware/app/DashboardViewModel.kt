package com.cyberaware.app

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberaware.app.network.DashboardStats
import com.cyberaware.app.network.RetrofitClient
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

    // 🔥 লাইভ থ্রেট লেভেল (সকেট থেকে আসবে)
    private val _liveThreatLevel = MutableStateFlow(0.2f)
    val liveThreatLevel: StateFlow<Float> = _liveThreatLevel

    private var stompClient: StompClient? = null

    fun loadDashboardData(token: String) {
        viewModelScope.launch {
            try {
                val profileResponse = RetrofitClient.apiService.getUserProfile("Bearer $token")
                if (profileResponse.isSuccessful) _userProfile.value = profileResponse.body()

                val statsResponse = RetrofitClient.apiService.getDashboardStats("Bearer $token")
                if (statsResponse.isSuccessful) _dashboardStats.value = statsResponse.body()
            } catch (e: Exception) {
                Log.e("DashboardVM", "API Error: ${e.message}")
            }
        }
    }

    @SuppressLint("CheckResult")
    fun connectWebSocket() {
        // 🔥 আপনার সার্ভারের IP দিন। Render এ থাকলে: "wss://cyber-aware.onrender.com/ws/websocket"
        val wsUrl = "ws://10.0.2.2:9090/ws/websocket"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.connect()

        // ব্যাকএন্ডের /topic/alerts এ লিসেন করা
        stompClient?.topic("/topic/alerts")?.subscribe({ topicMessage ->
            Log.d("WebSocket", "New Alert Received: ${topicMessage.payload}")

            val payload = topicMessage.payload.uppercase()
            val isDanger = payload.contains("DANGER") || payload.contains("SCAM") || payload.contains("CRITICAL")

            // ডেঞ্জার হলে গ্রাফে বড় স্পাইক (0.8 - 1.0), সেফ হলে নরমাল (0.2 - 0.4)
            val nextThreat = if (isDanger) {
                0.8f + Random.nextFloat() * (1.0f - 0.8f)
            } else {
                0.2f + Random.nextFloat() * (0.4f - 0.2f)
            }

            _liveThreatLevel.value = nextThreat
        }, { error ->
            Log.e("WebSocket", "Connection Error: ", error)
        })
    }

    override fun onCleared() {
        super.onCleared()
        stompClient?.disconnect() // অ্যাপ বন্ধ হলে সকেট ডিসকানেক্ট হবে
    }
}