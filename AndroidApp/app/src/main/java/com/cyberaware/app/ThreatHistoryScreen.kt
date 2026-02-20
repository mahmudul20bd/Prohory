package com.cyberaware.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay

// Data Model for History
data class ThreatHistoryLog(
    val id: String,
    val type: String, // "PHISHING", "NETWORK", "OCR"
    val target: String,
    val timestamp: String,
    val isThreat: Boolean,
    val details: String,
    val confidence: Int
)

@Composable
fun ThreatHistoryScreen() {
    // Simulated Database of Past Scans
    val historyLogs = remember {
        listOf(
            ThreatHistoryLog("PRH-001", "PHISHING", "http://free-money-update.com", "Today, 14:30", true, "Matched with Global Blacklist. SSL Invalid. Suspicious keywords found: 'free', 'update'.", 98),
            ThreatHistoryLog("PRH-002", "NETWORK", "192.168.1.1 (Home WiFi)", "Today, 12:05", false, "Gateway secure. No open vulnerable ports. Ping latency: 12ms.", 100),
            ThreatHistoryLog("PRH-003", "OCR", "Screenshot_20260220.png", "Yesterday, 21:15", true, "Extracted text contains social engineering attempt: 'Your account is blocked. Click here to verify'.", 92),
            ThreatHistoryLog("PRH-004", "PHISHING", "https://google.com", "Yesterday, 18:40", false, "Domain verified. SSL TLS 1.3 Active. Safe to browse.", 100),
            ThreatHistoryLog("PRH-005", "NETWORK", "Public_Coffee_Shop_WiFi", "Feb 18, 10:20", true, "Warning: Rogue Access Point detected. Port 80, 23 open. DNS Spoofing likely.", 85),
            ThreatHistoryLog("PRH-006", "OCR", "Invoice_Document.jpg", "Feb 17, 09:00", false, "Text semantics normal. No hidden URLs found.", 100)
        )
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    // Filtering Logic
    val filteredLogs = when (selectedFilter) {
        "THREATS" -> historyLogs.filter { it.isThreat }
        "SAFE" -> historyLogs.filter { !it.isThreat }
        else -> historyLogs
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        HistoryGridBackground()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // Header with Rotating Icon
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RotatingHistoryIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("INTELLIGENCE ARCHIVE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Encrypted Scan & Audit Logs", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Filter Tabs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip("ALL", selectedFilter == "ALL") { selectedFilter = "ALL" }
                FilterChip("THREATS", selectedFilter == "THREATS") { selectedFilter = "THREATS" }
                FilterChip("SAFE", selectedFilter == "SAFE") { selectedFilter = "SAFE" }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), // padding for bottom nav
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    AnimatedHistoryCard(log)
                }
            }
        }
    }
}

@Composable
fun FilterChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF131C2B), label = "bg")
    val borderColor by animateColorAsState(if (isSelected) NeonCyan else Color.DarkGray, label = "border")
    val textColor by animateColorAsState(if (isSelected) NeonCyan else Color.Gray, label = "text")

    Surface(
        modifier = Modifier.height(35.dp).clip(RoundedCornerShape(20.dp)).clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(title, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun AnimatedHistoryCard(log: ThreatHistoryLog) {
    var isExpanded by remember { mutableStateOf(false) }

    // Pulsing Glow for Threats
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    val cardBorderColor = if (log.isThreat) Color.Red.copy(alpha = pulseAlpha) else Color.White.copy(alpha = 0.05f)
    val iconColor = if (log.isThreat) Color.Red else Color.Green
    val iconVector = when (log.type) {
        "PHISHING" -> Icons.Default.Link
        "NETWORK" -> Icons.Default.WifiTethering
        else -> Icons.Default.DocumentScanner
    }

    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow")

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = Color(0xFF0B1221),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.1f), CircleShape).border(1.dp, iconColor.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(iconVector, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${log.type} SCAN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(log.timestamp, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (log.isThreat) "DANGER" else "SAFE", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.rotate(arrowRotation))
                }
            }

            // Expanded Details Section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2B), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("TARGET: ${log.target}", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("CONFIDENCE: ${log.confidence}%", color = iconColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("SYSTEM REPORT:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("> ${log.details}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Log ID: ${log.id}", color = Color.DarkGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun RotatingHistoryIcon() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "rot"
    )

    Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp).rotate(rotation))
    }
}

@Composable
fun HistoryGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}