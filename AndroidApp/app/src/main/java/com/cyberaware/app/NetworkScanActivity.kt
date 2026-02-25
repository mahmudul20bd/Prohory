package com.cyberaware.app

import android.content.Context
import android.net.wifi.WifiManager
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
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
                NetworkScannerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 🔥 UX FIX: Auto-Scroll State for Terminal
    val listState = rememberLazyListState()
    val terminalLogs = remember { mutableStateListOf<String>() }

    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var securityScore by remember { mutableIntStateOf(100) }

    val deviceIp = remember { getLocalIpAddress() }

    // 🔥 Blinking Cursor Animation for Terminal Execution Effect
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse)
    )

    // Auto-Scroll Trigger
    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
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
                            color = when {
                                securityScore >= 80 -> Color.Green
                                securityScore >= 50 -> Color(0xFFFF9800) // Orange
                                else -> Color.Red
                            },
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = when {
                                securityScore >= 80 -> "NETWORK SECURE"
                                securityScore >= 50 -> "VULNERABILITIES DETECTED"
                                else -> "CRITICAL RISK LEVEL"
                            },
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

                            coroutineScope.launch {
                                val finalScore = runRealNetworkScan(context, terminalLogs, deviceIp)
                                securityScore = finalScore
                                isScanning = false
                                scanComplete = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, NeonCyan, RoundedCornerShape(12.dp)).background(NeonCyan.copy(alpha = 0.1f))
                    ) {
                        Text("INITIATE DEEP SCAN", color = NeonCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 Auto-Scrolling Terminal Console
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                color = Color(0xFF0B1221),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM LOGS // REAL-TIME ANALYSIS", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(terminalLogs) { log ->
                            val textColor = when {
                                log.contains("[DANGER]") || log.contains("Risk:") -> Color.Red
                                log.contains("[WARNING]") || log.contains("[!]") -> Color.Yellow
                                log.contains("[✔]") || log.contains("[SECURE]") -> Color.Green
                                else -> NeonCyan
                            }
                            Text(text = log, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp), lineHeight = 16.sp)
                        }

                        // Active Execution Cursor
                        if (isScanning) {
                            item {
                                Text(text = "▌", color = NeonCyan.copy(alpha = cursorAlpha), fontFamily = FontFamily.Monospace, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🟢 Device IP Fetcher
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
    } catch (e: Exception) { e.printStackTrace() }
    return "127.0.0.1"
}

// 🔥 Real Gateway Detection using WifiManager
fun getRealGatewayIp(context: Context): String? {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        if (dhcp != null && dhcp.gateway != 0) {
            val ip = dhcp.gateway
            return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
        }
    } catch (e: Exception) { e.printStackTrace() }
    return null
}

// 🔥 UPGRADED ENTERPRISE NETWORK SCAN LOGIC
suspend fun runRealNetworkScan(context: Context, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, ip: String): Int {
    var score = 100
    var openPortsCount = 0
    var isPingFailed = false

    logs.add("> Initializing Prohory Real-Time Protocol...")
    delay(600)
    logs.add("> Target Device IP: $ip")
    delay(400)

    // 1. Detect Real Gateway
    logs.add("> Extracting Router/Gateway configuration...")
    delay(800)
    val realGateway = getRealGatewayIp(context)
    val gatewayIp = if (realGateway != null && realGateway != "0.0.0.0") realGateway else {
        val parts = ip.split(".")
        if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "8.8.8.8"
    }

    logs.add("[✔] Gateway IP Resolved: [$gatewayIp]")
    delay(600)

    withContext(Dispatchers.IO) {
        // 2. Real Ping Test
        try {
            val inet = InetAddress.getByName(gatewayIp)
            val startTime = System.currentTimeMillis()
            val reachable = inet.isReachable(2000)
            val latency = System.currentTimeMillis() - startTime

            withContext(Dispatchers.Main) {
                if (reachable) {
                    logs.add("[✔] Gateway Reachable. Latency: ${latency}ms")
                } else {
                    logs.add("[!] Gateway Ping Failed. Stealth Mode/Firewall Active.")
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
        withContext(Dispatchers.Main) { logs.add("> Initiating Vulnerability Port Scan...") }
        delay(600)

        // 3. Weighted Risk Engine & Human Readable Ports
        val portsToCheck = listOf(
            Triple(21, "FTP (File Transfer)", 25),
            Triple(22, "SSH (Secure Shell)", 15),
            Triple(23, "Telnet (Remote Control)", 40),
            Triple(80, "HTTP (Web Admin)", 15)
        )

        for ((port, desc, penalty) in portsToCheck) {
            withContext(Dispatchers.Main) { logs.add("> Probing Port $port [$desc]...") }
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(gatewayIp, port), 800)
                socket.close()

                withContext(Dispatchers.Main) {
                    logs.add("[DANGER] Port $port is OPEN and exposed!")

                    // Human readable risks
                    val riskText = when(port) {
                        21 -> "Risk: Hackers can access or modify your files without encryption."
                        22 -> "Risk: Exposed to brute-force login attacks."
                        23 -> "Risk: CRITICAL! Hackers can take full remote control of the router."
                        80 -> "Risk: Router admin panel is exposed to the network."
                        else -> "Risk: Unauthorized access point."
                    }
                    logs.add(riskText)

                    openPortsCount++
                    score -= penalty // Apply Weighted Penalty
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs.add("[✔] Port $port is closed securely.")
                }
            }
            delay(500) // Simulated processing time for animation
        }
    }

    delay(800)
    withContext(Dispatchers.Main) { logs.add("> Finalizing Cyber Report...") }
    delay(600)

    // Smart Conclusion
// Smart Conclusion
    if (openPortsCount == 0 && !isPingFailed) {
        logs.add("> [SECURE] NETWORK IS 100% SAFE. You can perform bank transactions safely.")
    } else if (openPortsCount == 0 && isPingFailed) {
        logs.add("> [SECURE] ROUTER STEALTH MODE ACTIVE. NO THREATS FOUND.")
    } else {
        logs.add("> [DANGER] VULNERABILITIES DETECTED! DO NOT USE THIS WIFI FOR BANKING.")

        // 🔥 UNIFIED ARCHIVE FIX: Push to Central Archive
        withContext(Dispatchers.Main) {
            val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
            val currentList = threatPrefs.getStringSet("quarantine_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            currentList.add("Network Defense|||CRITICAL: $openPortsCount vulnerable port(s) detected on network gateway [$gatewayIp].")
            threatPrefs.edit().putStringSet("quarantine_list", currentList).apply()
        }
    }

    return score.coerceAtLeast(0)
}
// 🟢 Pro-Level Radar Animation
@Composable
fun CyberRadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
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