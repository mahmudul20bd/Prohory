package com.cyberaware.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.network.SystemLog
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.launch

class NeuralLogsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val isScreenshotProtected = sharedPref.getBoolean("screenshot_protect", false)
        if (isScreenshotProtected) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        setContent {
            CyberAwareTheme {
                NeuralLogsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralLogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logsList = remember { mutableStateListOf<SystemLog>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("Agent") }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", "") ?: ""
        val myUsername = sharedPref.getString("username", "") ?: ""
        userRole = sharedPref.getString("role", "Agent") ?: "Agent"

        if (token.isNotEmpty()) {
            try {
                val response = RetrofitClient.apiService.getNeuralLogs("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val allLogs = response.body()!!

                    val filteredLogs = if (userRole.uppercase() == "ADMIN") {
                        allLogs.reversed()
                    } else {
                        // 🔥 FIX: Check for nulls or matches
                        allLogs.filter { it.username == myUsername || it.username == null || it.username == "null" }.reversed()
                    }

                    logsList.addAll(filteredLogs)
                } else {
                    errorMessage = "Failed to synchronize with Central Core."
                }
            } catch (e: Exception) {
                errorMessage = "Network Interface Error: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Authentication Protocol Missing."
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        CyberGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = Color(0xFF0B1221).copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(0.05f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.History, contentDescription = "Logs", tint = NeonPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NEURAL LOGS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }

                    if (userRole.uppercase() == "ADMIN") {
                        Surface(color = Color.Red.copy(0.2f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color.Red)) {
                            Text("ADMIN VIEW", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                    } else {
                        Surface(color = NeonCyan.copy(0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NeonCyan)) {
                            Text("PRIVATE VIEW", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
            } else if (errorMessage.isNotEmpty() && logsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(errorMessage, color = Color.Red) }
            } else if (logsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("NO PERSONAL ACTIVITY FOUND.", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(logsList) { log -> LogCardItem(log) }
                }
            }
        }
    }
}

@Composable
fun LogCardItem(log: SystemLog) {
    val (icon, tintColor) = when (log.actionType.uppercase()) {
        "LOGIN" -> Pair(Icons.Default.Login, NeonCyan)
        "REGISTRATION" -> Pair(Icons.Default.PersonAdd, NeonPurple)
        "REPORT", "THREAT", "SCAM" -> Pair(Icons.Default.BugReport, Color.Red)
        else -> Pair(Icons.Default.Security, Color.Green)
    }

    val performer = if (log.username == null || log.username == "null") "System" else log.username
    val finalDetails = log.details.replace("Agent null", "Agent $performer")

    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(tintColor.copy(alpha = 0.1f), CircleShape).border(1.dp, tintColor.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.actionType.uppercase(), color = tintColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(log.timestamp?.take(10) ?: "Just now", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(finalDetails, color = Color.White.copy(0.9f), fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Target: $performer", color = NeonBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}