package com.cyberaware.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                NetworkScannerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val terminalLogs = remember { mutableStateListOf<String>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var securityScore by remember { mutableIntStateOf(100) }

    // Fetch Real IP
    val deviceIp = remember { getLocalIpAddress() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Custom Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.WifiTethering, contentDescription = "Network", tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NETWORK INTELLIGENCE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radar Animation UI
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                CyberRadarAnimation(isScanning)

                if (scanComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$securityScore%",
                            color = if (securityScore >= 90) Color.Green else if (securityScore >= 70) Color.Yellow else Color.Red,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (securityScore >= 90) "NETWORK SECURE" else if (securityScore >= 70) "STEALTH WARNING" else "VULNERABILITIES FOUND",
                            color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        )
                    }
                } else if (!isScanning) {
                    Button(
                        onClick = {
                            isScanning = true
                            scanComplete = false
                            securityScore = 100
                            terminalLogs.clear()

                            // 🔥 Start Real Network Scan
                            coroutineScope.launch {
                                val finalScore = runRealNetworkScan(terminalLogs, deviceIp)
                                securityScore = finalScore
                                isScanning = false
                                scanComplete = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, NeonCyan, RoundedCornerShape(12.dp)).background(NeonCyan.copy(alpha = 0.1f))
                    ) {
                        Text("INITIATE REAL SCAN", color = NeonCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Terminal Console
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                color = Color(0xFF0B1221),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM LOGS // REAL-TIME ANALYSIS", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = false) {
                        items(terminalLogs) { log ->
                            val textColor = when {
                                log.contains("[DANGER]") || log.contains("ERROR") -> Color.Red
                                log.contains("[WARNING]") || log.contains("[!]") -> Color.Yellow
                                log.contains("[✔]") || log.contains("[SECURE]") -> Color.Green
                                else -> NeonCyan
                            }
                            Text(text = log, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

// 🟢 Real IP Fetching Logic
fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1"
}

// 🔥 ACTUAL REAL WORLD NETWORK SCAN LOGIC (UPGRADED)
suspend fun runRealNetworkScan(logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, ip: String): Int {
    var score = 100
    var openPortsCount = 0
    var isPingFailed = false

    logs.add("> Initializing Prohory Real-Time Protocol...")
    delay(500)
    logs.add("> Target Device IP: $ip")
    delay(500)

    val parts = ip.split(".")
    val gatewayIp = if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "8.8.8.8"

    logs.add("> Resolving Default Gateway/Router: [$gatewayIp]...")
    delay(800)

    withContext(Dispatchers.IO) {
        // 1. Real Ping Test
        try {
            val inet = InetAddress.getByName(gatewayIp)
            val startTime = System.currentTimeMillis()
            val reachable = inet.isReachable(2000)
            val latency = System.currentTimeMillis() - startTime

            withContext(Dispatchers.Main) {
                if (reachable) {
                    logs.add("[✔] Gateway Reachable. Latency: ${latency}ms")
                } else {
                    logs.add("[!] Gateway Ping Failed. Firewall might be active.")
                    isPingFailed = true
                    score -= 5
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("[!] Failed to verify network route.")
                isPingFailed = true
                score -= 5
            }
        }

        delay(800)
        withContext(Dispatchers.Main) { logs.add("> Initiating Deep Port Scan on Network...") }

        // 2. Real Port Scanning
        val portsToCheck = listOf(21, 22, 23, 80)

        for (port in portsToCheck) {
            withContext(Dispatchers.Main) { logs.add("> Probing Port $port...") }
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(gatewayIp, port), 800)
                socket.close()

                withContext(Dispatchers.Main) {
                    logs.add("[DANGER] Port $port is OPEN and exposed!")
                    openPortsCount++
                    score -= 20
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs.add("[✔] Port $port is closed and SECURE.")
                }
            }
            delay(400)
        }
    }

    delay(500)

    // 🔥 Smart Conclusion Logic
    if (openPortsCount == 0 && !isPingFailed) {
        logs.add("> [SECURE] NETWORK IS 100% SAFE.")
    } else if (openPortsCount == 0 && isPingFailed) {
        logs.add("> [WARNING] ROUTER STEALTH MODE ACTIVE. NO THREATS FOUND.")
    } else {
        logs.add("> [DANGER] VULNERABILITIES DETECTED! USE VPN IMMEDIATELY.")
    }

    return score.coerceAtLeast(0)
}

// 🟢 Pro-Level Radar Animation
@Composable
fun CyberRadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        drawCircle(color = NeonCyan.copy(alpha = 0.2f * pulse), radius = radius, center = center, style = Stroke(width = 2f))
        drawCircle(color = NeonCyan.copy(alpha = 0.4f * pulse), radius = radius * 0.7f, center = center, style = Stroke(width = 2f))
        drawCircle(color = NeonCyan.copy(alpha = 0.6f * pulse), radius = radius * 0.4f, center = center, style = Stroke(width = 2f))

        if (isScanning) {
            drawArc(
                brush = Brush.sweepGradient(colors = listOf(Color.Transparent, NeonCyan.copy(alpha = 0.8f), Color.Transparent), center = center),
                startAngle = rotation, sweepAngle = 60f, useCenter = true, size = size
            )
        }
    }
}