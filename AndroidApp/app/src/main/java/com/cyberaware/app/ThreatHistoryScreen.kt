package com.cyberaware.app

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.*

// Data Model for the UI
data class ThreatArtifact(
    val id: String,
    val sender: String,
    val message: String,
    val type: String,
    val riskScore: Int,
    val aiConfidence: Float,
    val patternCluster: String,
    val rawString: String // To delete from SharedPreferences
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)

    // State to hold parsed threats
    var archiveList by remember { mutableStateOf<List<ThreatArtifact>>(emptyList()) }

    // 🔥 Hardware Back Button Handling
    BackHandler {
        onBack()
    }

    // Load and Parse Data from SharedPreferences
    fun loadThreats() {
        val savedThreats = threatPrefs.getStringSet("quarantine_list", emptySet()) ?: emptySet()
        val parsedList = savedThreats.mapIndexed { index, rawData ->
            val parts = rawData.split("|||")
            val sender = if (parts.isNotEmpty()) parts[0] else "Unknown Sender"
            val message = if (parts.size > 1) parts[1] else "No Payload Content"

            // Dynamic Intelligence Heuristics (AI Meta-data Simulation based on text)
            val lowerMsg = message.lowercase()
            val (type, score, confidence, cluster) = when {
                lowerMsg.contains("http") || lowerMsg.contains(".com") ->
                    listOf("Phishing", (90..98).random(), 0.95f, "PHISH_URL_WAVE")
                lowerMsg.contains("bkash") || lowerMsg.contains("nagad") || lowerMsg.contains("bank") ->
                    listOf("Financial Spoof", (85..95).random(), 0.92f, "FIN_SPOOF_CLUSTER")
                lowerMsg.contains("win") || lowerMsg.contains("offer") || lowerMsg.contains("free") ->
                    listOf("Scam Campaign", (75..85).random(), 0.88f, "REWARD_SCAM_A")
                else ->
                    listOf("General Spam", (60..75).random(), 0.75f, "GENERIC_SPAM_NET")
            }

            ThreatArtifact(
                id = index.toString(),
                sender = sender,
                message = message,
                type = type as String,
                riskScore = score as Int,
                aiConfidence = confidence as Float,
                patternCluster = cluster as String,
                rawString = rawData
            )
        }.sortedByDescending { it.riskScore } // Highest risk first
        archiveList = parsedList
    }

    LaunchedEffect(Unit) { loadThreats() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A)).padding(16.dp)) {

        // 1️⃣ Header with Back Button
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.Storage, contentDescription = "Archive", tint = NeonPurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("INTELLIGENCE ARCHIVE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Personal Threat Engine", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        if (archiveList.isNotEmpty()) {
            // 2️⃣ Threat Timeline Visualization (Weekly Heat Graph)
            ThreatTimelineChart(archiveList.size)
            Spacer(modifier = Modifier.height(16.dp))

            // 3️⃣ Smart Auto-Resolution Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SYSTEM DEGRADED", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${archiveList.size} unresolved artifacts are reducing your device health.", color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    Button(
                        onClick = {
                            threatPrefs.edit().remove("quarantine_list").apply()
                            loadThreats()
                            Toast.makeText(context, "System Purged. Health Restored to 100%!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("PURGE ALL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4️⃣ Threat Intelligence List
        if (archiveList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color.Green.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ARCHIVE CLEAN", color = Color.Green, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Device health is operating at 100%", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp) // 🔥 Reduced padding since there is no bottom nav here
            ) {
                item {
                    Text("QUARANTINED ARTIFACTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(archiveList) { threat ->
                    ThreatIntelligenceCard(
                        threat = threat,
                        onDelete = {
                            val currentList = threatPrefs.getStringSet("quarantine_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                            currentList.remove(threat.rawString)
                            threatPrefs.edit().putStringSet("quarantine_list", currentList).apply()
                            loadThreats()
                            Toast.makeText(context, "Artifact Destroyed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// 🔬 EXPANDABLE THREAT INTELLIGENCE CARD
// ==========================================
@Composable
fun ThreatIntelligenceCard(threat: ThreatArtifact, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    // UI Logic based on Risk Score
    val riskColor = when {
        threat.riskScore >= 90 -> Color(0xFFFF1744) // Critical Red
        threat.riskScore >= 75 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFFFC400) // Yellow
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (threat.riskScore >= 90) riskColor.copy(alpha = pulseBorder) else Color.DarkGray)
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(16.dp)) {

            // Minimal View
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(10.dp).background(riskColor, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(threat.type.uppercase(), color = riskColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(threat.sender, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Surface(color = riskColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))) {
                    Text("SCORE: ${threat.riskScore}", color = riskColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            // Expanded "Why Flagged" Intelligence View
            if (isExpanded) {
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 16.dp))

                // Payload
                Text("INTERCEPTED PAYLOAD:", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"${threat.message}\"", color = Color.White, fontSize = 12.sp, fontStyle = FontStyle.Italic, lineHeight = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // AI Explainability Layer
                Surface(color = Color(0xFF0B1221), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("AI Engine", color = Color.Gray, fontSize = 10.sp)
                            Text("Prohory Vision v2", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Pattern Cluster", color = Color.Gray, fontSize = 10.sp)
                            Text(threat.patternCluster, color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("AI Confidence (${(threat.aiConfidence * 100).toInt()}%)", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { threat.aiConfidence },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = riskColor, trackColor = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isExpanded = false }) {
                        Text("CLOSE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DESTROY ARTIFACT", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 📊 ATTACK FREQUENCY VISUALIZATION
// ==========================================
@Composable
fun ThreatTimelineChart(totalThreats: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-DAY THREAT ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Simulated Data for Visualization
            val data = listOf(0.1f, 0.3f, 0.2f, 0.8f, 0.4f, 0.1f, if (totalThreats > 0) 1.0f else 0.1f)
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")

            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width / (data.size * 2)
                val maxBarHeight = size.height - 30f

                data.forEachIndexed { index, value ->
                    val x = index * (size.width / data.size) + (barWidth / 2)
                    val barHeight = maxBarHeight * value
                    val y = maxBarHeight - barHeight

                    val barColor = if (value > 0.7f) Color.Red else NeonCyan

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Labels
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { Text(it, color = Color.DarkGray, fontSize = 9.sp) }
            }
        }
    }
}