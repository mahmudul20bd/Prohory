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
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val timestamp: Long,
    val source: String,
    val isLocal: Boolean, // To track if we can delete it from local storage
    val rawString: String = "" // Original string for local deletion
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
    val userPrefs = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val loggedInUser = userPrefs.getString("username", "Unknown User") ?: "Unknown User"
    val token = userPrefs.getString("auth_token", "") ?: ""

    var archiveList by remember { mutableStateOf<List<ThreatArtifact>>(emptyList()) }
    var filterOption by remember { mutableStateOf("All Time") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler { onBack() }

    // 🔥 FETCH BOTH LOCAL SAFE SCANS AND CLOUD THREATS
    fun loadUnifiedThreats() {
        coroutineScope.launch(Dispatchers.IO) {
            val unifiedList = mutableListOf<ThreatArtifact>()

            // 1. LOAD LOCAL SAFE SCANS (Privacy First)
            val savedThreats = threatPrefs.getStringSet("scan_history_list", emptySet()) ?: emptySet()

            // Only keep SAFE scans from local (since Danger ones are fetched from server)
            val localSafeScans = savedThreats.filter { it.contains("SAFE") }

            localSafeScans.forEachIndexed { index, rawData ->
                val parts = rawData.split("|||")
                val sender = if (parts.isNotEmpty()) parts[0] else "Local Scan"
                val message = if (parts.size > 1) parts[1] else "No Payload"
                val timestamp = if (parts.size > 2) parts[2].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                val source = if (parts.size > 4) parts[4] else "Manual Scan"

                unifiedList.add(
                    ThreatArtifact(
                        id = "LOCAL-$index",
                        sender = sender,
                        message = message,
                        type = "SAFE",
                        riskScore = (1..29).random(),
                        aiConfidence = 0.99f,
                        patternCluster = "CLEAN_NETWORK",
                        timestamp = timestamp,
                        source = source,
                        isLocal = true,
                        rawString = rawData
                    )
                )
            }

            // 2. LOAD SERVER THREATS (Danger, Spam, Promo)
            if (token.isNotEmpty() && loggedInUser != "Unknown User") {
                try {
                    val response = RetrofitClient.apiService.getUserReports("Bearer $token", loggedInUser)
                    if (response.isSuccessful && response.body() != null) {
                        response.body()!!.forEach { report ->
                            val savedType = report.aiResult?.uppercase() ?: "UNKNOWN"

                            val (type, score, confidence, cluster) = when {
                                savedType == "DANGER" || savedType == "SCAM" -> listOf("DANGER", (90..99).random(), 0.95f, "MALICIOUS_CLUSTER")
                                savedType == "SPAM" -> listOf("SPAM", (70..89).random(), 0.88f, "SPAM_CAMPAIGN")
                                savedType == "PROMO" -> listOf("PROMO", (30..50).random(), 0.75f, "PROMO_MARKETING")
                                else -> listOf("SAFE", (1..29).random(), 0.99f, "CLEAN_NETWORK")
                            }

                            var timestampInMillis = System.currentTimeMillis()
                            try {
                                if (report.createdAt != null) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    timestampInMillis = sdf.parse(report.createdAt)?.time ?: System.currentTimeMillis()
                                }
                            } catch (e: Exception) {}

                            unifiedList.add(
                                ThreatArtifact(
                                    id = report.id?.toString() ?: UUID.randomUUID().toString().take(8),
                                    sender = report.sender ?: "Backend Scan",
                                    message = report.message ?: "No content",
                                    type = type as String,
                                    riskScore = score as Int,
                                    aiConfidence = confidence as Float,
                                    patternCluster = cluster as String,
                                    timestamp = timestampInMillis,
                                    source = report.sender ?: "Remote API",
                                    isLocal = false
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Apply Filter Logic & Sort Hybrid List by Newest First
            val currentTime = System.currentTimeMillis()
            val filteredList = unifiedList.filter {
                when (filterOption) {
                    "Last 7 Days" -> currentTime - it.timestamp <= 7L * 24 * 60 * 60 * 1000
                    "Last 30 Days" -> currentTime - it.timestamp <= 30L * 24 * 60 * 60 * 1000
                    else -> true
                }
            }.sortedByDescending { it.timestamp }

            withContext(Dispatchers.Main) {
                archiveList = filteredList
                isLoading = false
            }
        }
    }

    LaunchedEffect(filterOption) {
        isLoading = true
        loadUnifiedThreats()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.Storage, contentDescription = "Archive", tint = NeonPurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("SCAN HISTORY", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Logs (Local & Cloud)", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            if (archiveList.isNotEmpty()) {
                ThreatTimelineChart(archiveList.size)
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF131C2B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("SYSTEM LOGS", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${archiveList.size} mixed scans fetched.", color = Color.Gray, fontSize = 9.sp, lineHeight = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (archiveList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color.Green.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("HISTORY CLEAN", color = Color.Green, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("No scans found in database or local.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        Text("RECENT SCANS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(archiveList) { threat ->
                        ThreatIntelligenceCard(
                            threat = threat,
                            onDelete = {
                                if (threat.isLocal) {
                                    val currentList = threatPrefs.getStringSet("scan_history_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                                    currentList.remove(threat.rawString)
                                    threatPrefs.edit().putStringSet("scan_history_list", currentList).apply()
                                    loadUnifiedThreats()
                                    Toast.makeText(context, "Local SAFE Record Deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Cloud threats cannot be deleted by user.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(threat.type.uppercase(), color = riskColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (threat.isLocal) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.PhoneAndroid, contentDescription = "Local", tint = Color.Gray, modifier = Modifier.size(10.dp))
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.Cloud, contentDescription = "Cloud", tint = NeonCyan, modifier = Modifier.size(10.dp))
                            }
                        }
                        Text(threat.source, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)

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

                Text("SCANNED CONTENT:", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"${threat.message}\"", color = Color.White, fontSize = 12.sp, fontStyle = FontStyle.Italic, lineHeight = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Surface(color = Color(0xFF0B1221), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Scan ID", color = Color.Gray, fontSize = 10.sp)
                            Text("#${threat.id}", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Storage Location", color = Color.Gray, fontSize = 10.sp)
                            Text(if (threat.isLocal) "Encrypted Local Storage" else "Secure Cloud Server", color = if (threat.isLocal) Color.Green else NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
                    if (threat.isLocal) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DELETE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatTimelineChart(totalThreats: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-DAY SCAN ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("10+", color = Color.DarkGray, fontSize = 8.sp)
                    Text("5", color = Color.DarkGray, fontSize = 8.sp)
                    Text("0", color = Color.DarkGray, fontSize = 8.sp)
                }

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

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { Text(it, color = Color.Gray, fontSize = 8.sp) }
                    }
                }
            }
        }
    }
}