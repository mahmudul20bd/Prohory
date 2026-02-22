package com.cyberaware.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay

// Data Models
data class DivisionSector(
    val id: Int,
    var name: String,
    var centerLat: Double,
    var centerLon: Double,
    val polygons: List<List<GeoPoint>>,
    var threatLevel: Int = 0, // Used for coloring (Red/Yellow)
    var safeScans: Int = 0,   // Just for stats
    var targetedUsers: Int = 0
)

data class AdvancedAttackLog(val id: Int, val logMessage: String, val severity: String, val timestamp: Long)

@Composable
fun GlobalThreatMapScreen() {
    val context = LocalContext.current
    var bdDivisions by remember { mutableStateOf<List<DivisionSector>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Live Stats for UX
    var connectionStatus by remember { mutableStateOf("CONNECTING...") }
    var totalThreatsBlocked by remember { mutableIntStateOf(0) }
    val liveLogs = remember { mutableStateListOf<AdvancedAttackLog>() }
    var logCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        withContext(Dispatchers.IO) {
            val parsedDivisions = parseGeoJsonSmart(context)
            withContext(Dispatchers.Main) {
                bdDivisions = parsedDivisions
                isLoading = false
            }
        }
    }

    // 🔥 REAL BACKEND WEBSOCKET CONNECTION (STOMP Protocol over OkHttp)
    LaunchedEffect(bdDivisions) {
        if (bdDivisions.isEmpty()) return@LaunchedEffect

        val client = OkHttpClient()
        // Note: Spring Boot SockJS fallback raw endpoint is often /ws/websocket or /ws/websocket/websocket
        val request = Request.Builder().url("wss://cyber-aware.onrender.com/ws/websocket").build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PROHORY_WS", "Connected to Server")
                connectionStatus = "CONNECTED"

                // STOMP Connect Frame
                webSocket.send("CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\u0000")
                // STOMP Subscribe Frame
                webSocket.send("SUBSCRIBE\nid:sub-0\ndestination:/topic/alerts\n\n\u0000")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Parse STOMP Message Frame
                if (text.startsWith("MESSAGE")) {
                    try {
                        val jsonStartIndex = text.indexOf("{")
                        val jsonEndIndex = text.lastIndexOf("}") + 1
                        if (jsonStartIndex in 0 until jsonEndIndex) {
                            val jsonString = text.substring(jsonStartIndex, jsonEndIndex)
                            val json = JSONObject(jsonString)

                            val isSpam = json.optBoolean("isSpam", false)
                            val region = json.optString("region", "Dhaka") // Default to Dhaka if not provided
                            val type = json.optString("type", if (isSpam) "Phishing Attempt" else "Safe Scan")

                            // 🚀 CONCURRENCY & PRIORITY LOGIC
                            val targetDiv = bdDivisions.find { it.name.equals(region, ignoreCase = true) }
                            if (targetDiv != null) {
                                if (isSpam) {
                                    // SPAM: High priority, immediately spikes threat level
                                    targetDiv.threatLevel += 10
                                    targetDiv.targetedUsers += 1
                                    totalThreatsBlocked++

                                    val severity = if (targetDiv.threatLevel > 20) "CRITICAL" else "WARNING"
                                    val msg = "[LIVE] $type intercepted in ${targetDiv.name}."
                                    liveLogs.add(0, AdvancedAttackLog(logCounter++, msg, severity, System.currentTimeMillis()))
                                } else {
                                    // SAFE: Low priority, just increments safe scan count, doesn't spike map color
                                    targetDiv.safeScans += 1
                                    // Optionally add to log if you want, but better to keep logs clean for threats
                                }

                                // Keep logs max 20 to prevent memory leak
                                if (liveLogs.size > 20) liveLogs.removeAt(liveLogs.size - 1)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PROHORY_WS", "JSON Parse Error: ${e.message}")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionStatus = "DISCONNECTED (RETRYING)"
                Log.e("PROHORY_WS", "WebSocket Failed: ${t.message}")
            }
        })

        // Cool-down logic (Threat level decreases over time if no new spam comes)
        while (true) {
            delay(1000)
            bdDivisions.forEach { if (it.threatLevel > 0) it.threatLevel -= 2 }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A)).padding(16.dp)) {

        // 🌟 UX ENHANCEMENT: System Status HUD
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            color = Color(0xFF0B1221),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (connectionStatus == "CONNECTED") Color.Green else Color.Red
                    Icon(Icons.Default.CellTower, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("SERVER LINK", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(connectionStatus, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("THREATS BLOCKED", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("$totalThreatsBlocked", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // 🌟 REAL MAP
        Surface(
            modifier = Modifier.fillMaxWidth().height(360.dp)
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

        // 🌟 Live Threat Terminal
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp)),
            color = Color(0xFF131C2B),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYSTEM INTERCEPT LOGS", color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(liveLogs, key = { it.id }) { log ->
                        val textColor = if (log.severity == "CRITICAL") Color(0xFFFF5252) else Color(0xFFFFD740)
                        Text(
                            text = log.logMessage, color = textColor, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (log.severity == "CRITICAL") FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// 🔥 Smart GeoJSON Parser (No Color Mapping Needed Anymore)
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

            list.add(DivisionSector(i, detectedName, centerLat, centerLon, polygonsList, threatLevel = 0))
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
            delay(16) // 60 FPS Animation
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

                // 🔥 BORDERS ARE NOW STRICTLY WHITE (#E0E0E0)
                divisions.forEach { div ->
                    div.polygons.forEach { geoPoints ->
                        val polygon = Polygon().apply {
                            points = geoPoints
                            fillPaint.color = android.graphics.Color.TRANSPARENT
                            outlinePaint.color = android.graphics.Color.parseColor("#E0E0E0") // Professional White/Grey
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
            // Update polygon border colors ONLY if there is a threat
            map.overlays.filterIsInstance<Polygon>().forEach { poly ->
                val div = divisions.find { it.name == poly.id }
                if (div != null) {
                    if (div.threatLevel > 0) {
                        poly.outlinePaint.color = if (div.threatLevel > 20) android.graphics.Color.RED else android.graphics.Color.YELLOW
                        poly.outlinePaint.alpha = 255
                        poly.outlinePaint.strokeWidth = 3f
                    } else {
                        poly.outlinePaint.color = android.graphics.Color.parseColor("#E0E0E0") // Back to White
                        poly.outlinePaint.alpha = 150
                        poly.outlinePaint.strokeWidth = 2.5f
                    }
                }
            }
        }
    )
}

// 🔥 THE FIX: 100% PERFECT RADAR WAVE (No solid fill, proper spreading animation)
class AdvancedThreatOverlay(private val divisions: List<DivisionSector>) : Overlay() {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // STROKE is used so it looks like a ring/wave, NOT a filled puddle!
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

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
                div.threatLevel > 20 -> arrayOf(android.graphics.Color.parseColor("#FF1744"), 1200f, 150f) // RED (Critical)
                div.threatLevel > 5  -> arrayOf(android.graphics.Color.parseColor("#FFC400"), 1600f, 100f) // YELLOW (Warning)
                else                 -> arrayOf(android.graphics.Color.parseColor("#00E5FF"), 2500f, 60f)  // CYAN (Safe/Scanning)
            }

            val threatColor = color as Int
            val animSpeed = speed as Float
            val rippleMax = maxRadius as Float

            // 1. Spreading Light Wave (The fix for the solid puddle)
            val phase = (time % animSpeed) / animSpeed
            val waveRadius = rippleMax * phase

            // Starts highly visible, fades out to 0 as it gets bigger
            val waveAlpha = (255 * (1f - phase)).toInt().coerceIn(0, 255)

            // Thickness of the wave decreases as it spreads for extreme realism
            val waveThickness = 8f * (1f - phase) + 2f

            wavePaint.color = threatColor
            wavePaint.alpha = waveAlpha
            wavePaint.strokeWidth = waveThickness
            canvas.drawCircle(px, py, waveRadius, wavePaint)

            // 2. The Center Dot (Blinking dynamically)
            val isCritical = div.threatLevel > 5
            if (isCritical) {
                // Heartbeat pulse for danger
                val blinkAlpha = (100 + 155 * Math.abs(Math.sin(time / 200.0))).toInt()
                dotPaint.color = threatColor
                dotPaint.alpha = blinkAlpha
                canvas.drawCircle(px, py, 12f, dotPaint)
            } else {
                // Steady glow for safe
                dotPaint.color = threatColor
                dotPaint.alpha = 200
                canvas.drawCircle(px, py, 8f, dotPaint)
            }

            // Solid White Core
            dotPaint.color = android.graphics.Color.WHITE
            dotPaint.alpha = 255
            canvas.drawCircle(px, py, 4f, dotPaint)

            // 3. Division Text
            if (div.name != "Unknown") {
                canvas.drawText(div.name, px, py - 20f, textPaint)
            }
        }

        // Force continuous redraw
        osmv.postInvalidate()
    }
}