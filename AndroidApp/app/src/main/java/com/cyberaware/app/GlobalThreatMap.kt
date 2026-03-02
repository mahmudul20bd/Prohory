package com.cyberaware.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Models
data class DivisionSector(
    val id: Int,
    var name: String,
    var centerLat: Double,
    var centerLon: Double,
    val polygons: List<List<GeoPoint>>,
    var threatLevel: Int = 0,
    var safeScans: Int = 0,
    var targetedUsers: Int = 0,
    var confidence: Double = 0.95
)

data class AdvancedAttackLog(val id: Int, val logMessage: String, val severity: String, val timestamp: Long, val rawDetails: String = "")

@Composable
fun GlobalThreatMapScreen() {
    val context = LocalContext.current
    var bdDivisions by remember { mutableStateOf<List<DivisionSector>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Live Stats for UX
    var connectionStatus by remember { mutableStateOf("CONNECTING...") }
    var totalThreatsDetected by remember { mutableIntStateOf(0) }
    val liveLogs = remember { mutableStateListOf<AdvancedAttackLog>() }
    var logCounter by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

    var showRadarPrompt by remember { mutableStateOf(!sharedPref.getBoolean("has_seen_radar_prompt", false)) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        withContext(Dispatchers.IO) {
            val parsedDivisions = parseGeoJsonSmart(context)

            try {
                val statsResponse = com.cyberaware.app.network.RetrofitClient.apiService.getDashboardStats(authHeader)
                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()
                    if (stats != null) {
                        withContext(Dispatchers.Main) {
                            totalThreatsDetected = stats.blockedScams ?: 0
                        }
                    }
                }

                val logsResponse = com.cyberaware.app.network.RetrofitClient.apiService.getNeuralLogs(authHeader)
                if (logsResponse.isSuccessful) {
                    val logs = logsResponse.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        logs.filter { it.actionType != "LOGIN" && it.actionType != "REGISTRATION" }
                            .forEach { log ->
                                val typeUpper = log.actionType.uppercase()

                                // 🔥 FIX: Properly classifying map logs based on actual backend data
                                val severity = when {
                                    typeUpper.contains("DANGER") || typeUpper.contains("SCAM") || typeUpper.contains("PHISHING") || typeUpper.contains("THREAT") -> "DANGER"
                                    typeUpper.contains("SPAM") || typeUpper.contains("ALERT") || typeUpper.contains("WARNING") -> "SPAM"
                                    else -> "SAFE"
                                }

                                if (severity == "DANGER" || severity == "SPAM") {
                                    val timeInMillis = try {
                                        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                        // 🔥 FIX: সার্ভারের UTC টাইমকে লোকাল টাইমে কনভার্ট করা হচ্ছে
                                        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        format.parse(log.timestamp ?: "")?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) { System.currentTimeMillis() }
                                    liveLogs.add(AdvancedAttackLog(logCounter++, log.details, severity, timeInMillis, log.details))
                                }
                            }
                        if (liveLogs.size > 15) {
                            liveLogs.removeRange(15, liveLogs.size)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PROHORY_MAP", "Failed to fetch map history: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                bdDivisions = parsedDivisions
                isLoading = false
            }
        }
    }

    // REAL BACKEND WEBSOCKET CONNECTION
    LaunchedEffect(bdDivisions) {
        if (bdDivisions.isEmpty()) return@LaunchedEffect

        val client = OkHttpClient()
        val request = Request.Builder().url("wss://cyber-aware.onrender.com/ws/websocket").build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PROHORY_WS", "Connected to Server")
                connectionStatus = "CONNECTED"

                webSocket.send("CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\u0000")
                webSocket.send("SUBSCRIBE\nid:sub-0\ndestination:/topic/alerts\n\n\u0000")
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                if (text.startsWith("MESSAGE")) {
                    try {
                        val jsonStartIndex = text.indexOf("{")
                        val jsonEndIndex = text.lastIndexOf("}") + 1
                        if (jsonStartIndex in 0 until jsonEndIndex) {
                            val jsonString = text.substring(jsonStartIndex, jsonEndIndex)
                            val json = JSONObject(jsonString)

                            val isSpam = json.optBoolean("isSpam", false)
                            val region = json.optString("region", "UNKNOWN")
                            val type = json.optString("type", "Unknown").uppercase()
                            val confidence = json.optDouble("confidence", 0.0)
                            val rawHeatPoints = json.optInt("heatPoints", 1)

                            val severity = when {
                                type.contains("DANGER") || type.contains("SCAM") || type.contains("PHISHING") || rawHeatPoints >= 3 -> "DANGER"
                                type.contains("SPAM") || isSpam -> "SPAM"
                                else -> "SAFE"
                            }

                            coroutineScope.launch {
                                if (severity == "DANGER" || severity == "SPAM") {
                                    totalThreatsDetected++

                                    val displayRegion = if (region == "UNKNOWN") "an UNKNOWN Origin" else "$region Region"
                                    val msg = if (severity == "DANGER") "🚨 Alert! Malicious $type detected in $displayRegion."
                                    else "⚠️ Warning! Suspicious spam detected in $displayRegion."

                                    liveLogs.add(0, AdvancedAttackLog(logCounter++, msg, severity, System.currentTimeMillis(), msg))

                                    val targetDiv = bdDivisions.find {
                                        it.name.contains(region, ignoreCase = true) || region.contains(it.name, ignoreCase = true)
                                    }

                                    if (targetDiv != null) {
                                        when (severity) {
                                            "DANGER" -> targetDiv.threatLevel += (rawHeatPoints * 10).coerceAtLeast(25)
                                            "SPAM" -> targetDiv.threatLevel += (rawHeatPoints * 5).coerceAtLeast(10)
                                        }
                                        targetDiv.confidence = confidence
                                        targetDiv.targetedUsers += 1
                                    }

                                    if (liveLogs.size > 20) liveLogs.removeAt(liveLogs.size - 1)
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionStatus = "DISCONNECTED"
                Log.e("PROHORY_WS", "WebSocket Failed: ${t.message}")
            }
        })

        while (true) {
            delay(1000)
            bdDivisions.forEach { if (it.threatLevel > 0) it.threatLevel -= 2 }
        }
    }

    if (showRadarPrompt) {
        RegionalSetupModal(
            onDismiss = {
                sharedPref.edit().putBoolean("has_seen_radar_prompt", true).apply()
                showRadarPrompt = false
            },
            onSave = { selectedDivision ->
                sharedPref.edit()
                    .putString("user_division", selectedDivision)
                    .putBoolean("has_seen_radar_prompt", true)
                    .apply()
                showRadarPrompt = false
                Toast.makeText(context, "Radar Enhanced for $selectedDivision Region", Toast.LENGTH_LONG).show()
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition()
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A))
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(dotPulse)
                    .background(Color.Green, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Live Threat Map", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(
                    "Real-time scams and dangerous messages detected countrywide.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isConnected = connectionStatus == "CONNECTED"
            val linkColor = if (isConnected) Color(0xFF10B981) else Color.Red

            Surface(
                modifier = Modifier.weight(1f).height(65.dp),
                color = Color(0xFF131C2B),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, linkColor.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(linkColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if(isConnected) Icons.Default.CellTower else Icons.Default.WifiTethering, contentDescription = null, tint = linkColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text("SERVER LINK", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(connectionStatus, color = linkColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).height(65.dp),
                color = Color(0xFF131C2B),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFFF9800).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text("DETECTED", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("$totalThreatsDetected Scams", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1.2f)
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
            color = Color(0xFF0B1221)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                DivisionMapComposable(bdDivisions)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp)),
            color = Color(0xFF131C2B),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RECENT SECURITY ACTIVITY", color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(liveLogs, key = { it.id }) { log ->
                        AnimatedLogItem(log)
                    }
                }
            }
        }
    }
}

fun getMapTimeAgo(timeInMillis: Long): String {
    if (timeInMillis == 0L) return "Scanned just now"
    val diff = System.currentTimeMillis() - timeInMillis
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    val days = hours / 24
    return when {
        mins < 1 -> "Scanned just now"
        mins < 60 -> "Scanned $mins mins ago"
        hours < 24 -> "Scanned $hours hours ago"
        else -> "Scanned $days days ago"
    }
}

@Composable
fun AnimatedLogItem(log: AdvancedAttackLog) {
    val userFriendlyMessage = remember(log.rawDetails) {
        val details = log.rawDetails.lowercase()

        // 🔥 FIX: rawDetails থেকে Division/Region নামটা বের করার চেষ্টা করা
        val divisions = listOf("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh")
        val detectedDiv = divisions.find { details.contains(it.lowercase()) }
        val locationText = if (detectedDiv != null) " in $detectedDiv Region" else ""

        when {
            log.logMessage.startsWith("🚨") || log.logMessage.startsWith("⚠️") -> log.logMessage
            details.contains("ocr") || details.contains("image") -> "📸 Image scan found a threat$locationText."
            details.contains("phishing") || details.contains("link") -> "🎣 Dangerous website link was detected$locationText."
            log.severity == "DANGER" -> "🚨 DANGER: Malicious scam or phishing detected$locationText."
            log.severity == "SPAM" -> "⚠️ SPAM: Unsolicited message detected$locationText."
            else -> log.logMessage
        }
    }


    val statusColor = if (log.severity == "DANGER") Color(0xFFFF5252) else Color(0xFFFF9800)

    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0B1221).copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(pulse)
                    .background(statusColor, CircleShape)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = userFriendlyMessage,
                    color = if (log.severity == "DANGER") Color(0xFFFFCDD2) else Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )

                Text(
                    text = getMapTimeAgo(log.timestamp),
                    color = Color(0xFFB0BEC5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ... [RegionalSetupModal, parseGeoJsonSmart, DivisionMapComposable, AdvancedThreatOverlay remain entirely unchanged] ...
@Composable
fun RegionalSetupModal(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDiv by remember { mutableStateOf("Select Division") }
    val divisions = listOf("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B1221),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(60.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape).border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "Radar", tint = NeonCyan, modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("ENHANCE REGIONAL RADAR", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Help Prohory map local cyber threats accurately. Select your division to activate High-Confidence (0.95) threat detection in your area.",
                    color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(50.dp).clickable { expanded = true },
                        color = Color(0xFF131C2B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedDiv, color = if (selectedDiv == "Select Division") Color.Gray else Color.White, fontSize = 14.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF131C2B))
                    ) {
                        divisions.forEach { div ->
                            DropdownMenuItem(
                                text = { Text(div, color = Color.White) },
                                onClick = {
                                    selectedDiv = div
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("SKIP", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { if (selectedDiv != "Select Division") onSave(selectedDiv) },
                        enabled = selectedDiv != "Select Division",
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDiv != "Select Division") Color(0xFF10B981) else Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ACTIVATE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun parseGeoJsonSmart(context: Context): List<DivisionSector> {
    val list = mutableListOf<DivisionSector>()
    val knownDivisions = listOf("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh")

    try {
        val inputStream = context.resources.openRawResource(R.raw.bd_divisions)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObj = JSONObject(jsonString)
        val features = jsonObj.getJSONArray("features")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.getJSONObject("properties")

            var detectedName = "Unknown"
            for (key in props.keys()) {
                val value = props.optString(key, "")
                val match = knownDivisions.find { value.contains(it, ignoreCase = true) }
                if (match != null) {
                    detectedName = match.uppercase()
                    break
                }
            }

            val geometry = feature.getJSONObject("geometry")
            val type = geometry.getString("type")
            val coordinates = geometry.getJSONArray("coordinates")
            val polygonsList = mutableListOf<List<GeoPoint>>()

            if (type == "Polygon") {
                val poly = coordinates.getJSONArray(0)
                val points = mutableListOf<GeoPoint>()
                for (j in 0 until poly.length()) {
                    val pt = poly.getJSONArray(j)
                    points.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
                }
                polygonsList.add(points)
            } else if (type == "MultiPolygon") {
                for (j in 0 until coordinates.length()) {
                    val poly = coordinates.getJSONArray(j).getJSONArray(0)
                    val points = mutableListOf<GeoPoint>()
                    for (k in 0 until poly.length()) {
                        val pt = poly.getJSONArray(k)
                        points.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
                    }
                    polygonsList.add(points)
                }
            }

            var centerLat = 0.0
            var centerLon = 0.0
            if (polygonsList.isNotEmpty()) {
                val largestPolygon = polygonsList.maxByOrNull { it.size }!!
                centerLat = largestPolygon.map { it.latitude }.average()
                centerLon = largestPolygon.map { it.longitude }.average()
            }

            list.add(DivisionSector(i, detectedName, centerLat, centerLon, polygonsList, threatLevel = 0, safeScans = 0, targetedUsers = 0, confidence = 0.95))
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}

@Composable
fun DivisionMapComposable(divisions: List<DivisionSector>) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            mapView.postInvalidate()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(6.8)
                controller.setCenter(GeoPoint(23.6850, 90.3563))

                val inverseMatrix = ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f,  0f, 255f,
                    0f, -1f,  0f,  0f, 255f,
                    0f,  0f, -1f,  0f, 255f,
                    0f,  0f,  0f,  1f,   0f
                ))
                val blueTint = ColorMatrix(floatArrayOf(
                    0.7f, 0f, 0f, 0f, 0f,
                    0f, 0.8f, 0f, 0f, 0f,
                    0f, 0f, 1.1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                inverseMatrix.postConcat(blueTint)
                overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(inverseMatrix))

                divisions.forEach { div ->
                    div.polygons.forEach { geoPoints ->
                        val polygon = Polygon().apply {
                            points = geoPoints
                            fillPaint.color = android.graphics.Color.TRANSPARENT
                            outlinePaint.color = android.graphics.Color.parseColor("#E0E0E0")
                            outlinePaint.strokeWidth = 2.5f
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            outlinePaint.alpha = 150
                        }
                        polygon.id = div.name
                        overlays.add(polygon)
                    }
                }
                overlays.add(AdvancedThreatOverlay(divisions))
            }
        },
        update = { map ->
            map.overlays.filterIsInstance<Polygon>().forEach { poly ->
                val div = divisions.find { it.name == poly.id }
                if (div != null) {
                    when {
                        div.threatLevel > 20 -> {
                            poly.outlinePaint.color = android.graphics.Color.RED
                            poly.outlinePaint.alpha = 255
                            poly.outlinePaint.strokeWidth = 3.5f
                        }
                        div.threatLevel > 5 -> {
                            poly.outlinePaint.color = android.graphics.Color.parseColor("#FF9800")
                            poly.outlinePaint.alpha = 220
                            poly.outlinePaint.strokeWidth = 3f
                        }
                        div.threatLevel > 0 -> {
                            poly.outlinePaint.color = android.graphics.Color.parseColor("#3B82F6")
                            poly.outlinePaint.alpha = 180
                            poly.outlinePaint.strokeWidth = 2.5f
                        }
                        else -> {
                            poly.outlinePaint.color = android.graphics.Color.parseColor("#E0E0E0")
                            poly.outlinePaint.alpha = 150
                            poly.outlinePaint.strokeWidth = 2.5f
                        }
                    }
                }
            }
        }
    )
}

class AdvancedThreatOverlay(private val divisions: List<DivisionSector>) : Overlay() {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
    }
    private val point = Point()

    override fun draw(canvas: Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return

        val projection = osmv.projection
        val time = System.currentTimeMillis()

        divisions.forEach { div ->
            projection.toPixels(GeoPoint(div.centerLat, div.centerLon), point)
            val px = point.x.toFloat()
            val py = point.y.toFloat()

            val (color, speed, maxRadius) = when {
                div.threatLevel > 20 -> arrayOf(android.graphics.Color.parseColor("#FF1744"), 1200f, 150f)
                div.threatLevel > 5  -> arrayOf(android.graphics.Color.parseColor("#FF9800"), 1600f, 100f)
                div.threatLevel > 0  -> arrayOf(android.graphics.Color.parseColor("#3B82F6"), 2000f, 80f)
                else                 -> arrayOf(android.graphics.Color.parseColor("#00E5FF"), 2500f, 60f)
            }

            val isCritical = div.threatLevel > 5
            val threatColor = if (div.confidence < 0.8 && isCritical) android.graphics.Color.parseColor("#FF9800") else color as Int
            val animSpeed = speed as Float
            val rippleMax = maxRadius as Float

            val phase = (time % animSpeed) / animSpeed
            val waveRadius = rippleMax * phase
            val waveAlpha = (255 * (1f - phase)).toInt().coerceIn(0, 255)
            val waveThickness = 8f * (1f - phase) + 2f

            wavePaint.color = threatColor
            wavePaint.alpha = waveAlpha
            wavePaint.strokeWidth = waveThickness
            canvas.drawCircle(px, py, waveRadius, wavePaint)

            if (isCritical) {
                val blinkAlpha = (100 + 155 * Math.abs(Math.sin(time / 200.0))).toInt()
                dotPaint.color = threatColor
                dotPaint.alpha = blinkAlpha
                canvas.drawCircle(px, py, 12f, dotPaint)
            } else {
                dotPaint.color = threatColor
                dotPaint.alpha = 200
                canvas.drawCircle(px, py, 8f, dotPaint)
            }

            dotPaint.color = android.graphics.Color.WHITE
            dotPaint.alpha = 255
            canvas.drawCircle(px, py, 4f, dotPaint)

            if (div.name != "Unknown") {
                canvas.drawText(div.name, px, py - 20f, textPaint)
            }
        }
        osmv.postInvalidate()
    }
}