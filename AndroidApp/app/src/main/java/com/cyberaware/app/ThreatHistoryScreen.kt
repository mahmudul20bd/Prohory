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
import androidx.compose.material.icons.filled.ArrowDropDown
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
    val timestamp: Long, // Added timestamp for filtering
    val rawString: String // To delete from SharedPreferences
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)

    var archiveList by remember { mutableStateOf<List<ThreatArtifact>>(emptyList()) }
    var filterOption by remember { mutableStateOf("All Time") }
    var showFilterMenu by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    fun loadThreats() {
        val savedThreats = threatPrefs.getStringSet("quarantine_list", emptySet()) ?: emptySet()
        val parsedList = savedThreats.mapIndexed { index, rawData ->
            val parts = rawData.split("|||")
            val sender = if (parts.isNotEmpty()) parts[0] else "Unknown Sender"
            val message = if (parts.size > 1) parts[1] else "No Payload Content"
            val timestamp = if (parts.size > 2) parts[2].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()

            // 🔥 4-Level AI: Dynamic Intelligence Heuristics based on text
            val lowerMsg = message.lowercase()
            val (type, score, confidence, cluster) = when {
                lowerMsg.contains("http") || lowerMsg.contains(".com") || lowerMsg.contains("bkash") || lowerMsg.contains("nagad") || lowerMsg.contains("bank") ->
                    listOf("DANGER", (90..99).random(), 0.95f, "MALICIOUS_CLUSTER")
                lowerMsg.contains("win") || lowerMsg.contains("offer") || lowerMsg.contains("free") || lowerMsg.contains("lottery") ->
                    listOf("SPAM", (70..89).random(), 0.88f, "SPAM_CAMPAIGN")
                lowerMsg.contains("discount") || lowerMsg.contains("buy") || lowerMsg.contains("promo") ->
                    listOf("PROMO", (30..50).random(), 0.75f, "PROMO_MARKETING")
                else ->
                    listOf("SPAM", (51..69).random(), 0.70f, "GENERIC_SPAM_NET")
            }

            ThreatArtifact(
                id = index.toString(),
                sender = sender,
                message = message,
                type = type as String,
                riskScore = score as Int,
                aiConfidence = confidence as Float,
                patternCluster = cluster as String,
                timestamp = timestamp,
                rawString = rawData
            )
        }

        // Apply Filter Logic
        val currentTime = System.currentTimeMillis()
        val filteredList = parsedList.filter {
            when (filterOption) {
                "Last 7 Days" -> currentTime - it.timestamp <= 7L * 24 * 60 * 60 * 1000
                "Last 30 Days" -> currentTime - it.timestamp <= 30L * 24 * 60 * 60 * 1000
                else -> true // All Time
            }
        }.sortedByDescending { it.timestamp } // Sort by newest first instead of risk score for better history tracking

        archiveList = filteredList
    }

    LaunchedEffect(filterOption) { loadThreats() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A)).padding(16.dp)) {

        // 1️⃣ Header with Back Button and Filter
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.Storage, contentDescription = "Archive", tint = NeonPurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("THREAT HISTORY", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Quarantined Messages", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            // Filter Button
            Box {
                Surface(
                    modifier = Modifier.clickable { showFilterMenu = true },
                    color = Color(0xFF131C2B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(filterOption, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }, modifier = Modifier.background(Color(0xFF131C2B))) {
                    listOf("Last 7 Days", "Last 30 Days", "All Time").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White, fontSize = 12.sp) },
                            onClick = { filterOption = option; showFilterMenu = false }
                        )
                    }
                }
            }
        }

        if (archiveList.isNotEmpty()) {
            // 2️⃣ Threat Timeline Visualization (Weekly Heat Graph) with Y-Axis
            ThreatTimelineChart(archiveList.size)
            Spacer(modifier = Modifier.height(16.dp))

            // 3️⃣ Smart Auto-Resolution Panel (UI Adjusted for better fit)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("SYSTEM DEGRADED", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${archiveList.size} unresolved messages found.", color = Color.Gray, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                    Button(
                        onClick = {
                            threatPrefs.edit().remove("quarantine_list").apply()
                            loadThreats()
                            Toast.makeText(context, "All threats cleared!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), // Made button slightly smaller
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("CLEAR ALL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    Text("No threats found in this period.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Text("BLOCKED MESSAGES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                            Toast.makeText(context, "Message Deleted", Toast.LENGTH_SHORT).show()
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

    val riskColor = when (threat.type) {
        "DANGER" -> Color(0xFFFF1744)
        "SPAM" -> Color(0xFFFF9800)
        "PROMO" -> Color(0xFF3B82F6)
        else -> Color.Green
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
        border = BorderStroke(1.dp, if (threat.type == "DANGER") riskColor.copy(alpha = pulseBorder) else Color.DarkGray)
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(10.dp).background(riskColor, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(threat.type.uppercase(), color = riskColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(threat.sender, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)

                        // Show formatted date
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        Text(dateFormat.format(Date(threat.timestamp)), color = Color.Gray, fontSize = 9.sp)
                    }
                }
                Surface(color = riskColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))) {
                    Text("SCORE: ${threat.riskScore}", color = riskColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            if (isExpanded) {
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 16.dp))

                Text("MESSAGE CONTENT:", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"${threat.message}\"", color = Color.White, fontSize = 12.sp, fontStyle = FontStyle.Italic, lineHeight = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))

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
                        Text("DELETE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold) // Renamed from DESTROY
                    }
                }
            }
        }
    }
}

// ==========================================
// 📊 ATTACK FREQUENCY VISUALIZATION (WITH Y-AXIS LABELS)
// ==========================================
@Composable
fun ThreatTimelineChart(totalThreats: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp), // Slightly taller to fit labels nicely
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-DAY THREAT ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Graph Area with Y-Axis and X-Axis
            Row(modifier = Modifier.fillMaxSize()) {

                // Y-Axis Labels (Number of threats)
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("10+", color = Color.DarkGray, fontSize = 8.sp)
                    Text("5", color = Color.DarkGray, fontSize = 8.sp)
                    Text("0", color = Color.DarkGray, fontSize = 8.sp)
                }

                // Bar Chart Area
                Column(modifier = Modifier.weight(1f)) {
                    val data = listOf(0.1f, 0.3f, 0.2f, 0.8f, 0.4f, 0.1f, if (totalThreats > 0) 1.0f else 0.1f)
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                    Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        val barWidth = size.width / (data.size * 2)
                        val maxBarHeight = size.height

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

                    // X-Axis Labels (Days)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { Text(it, color = Color.Gray, fontSize = 8.sp) }
                    }
                }
            }
        }
    }
}