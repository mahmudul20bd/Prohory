package com.cyberaware.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhishingScannerActivity : ComponentActivity() {
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
                PhishingScannerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var payloadInput by remember { mutableStateOf("") }
    val scanLogs = remember { mutableStateListOf<String>() }

    // 🔥 scanState: 0=Idle, 1=Scanning, 2=Safe, 3=Promo, 4=Spam, 5=Danger
    var scanState by remember { mutableIntStateOf(0) }
    var downloadStatus by remember { mutableStateOf("Checking Offline Files...") }
    var isModelReady by remember { mutableStateOf(false) }

    var isNetOn by remember { mutableStateOf(isInternetAvailable(context)) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isNetOn = true }
            override fun onLost(network: Network) { isNetOn = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        onDispose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(scanLogs.size) {
        if (scanLogs.isNotEmpty()) {
            listState.animateScrollToItem(scanLogs.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        val modelFile = File(context.filesDir, "cyberaware_model.tflite")
        val vocabFile = File(context.filesDir, "vocab.txt")

        val isModelValid = modelFile.exists() && modelFile.length() > 0L
        val isVocabValid = vocabFile.exists() && vocabFile.length() > 0L

        if (isModelValid && isVocabValid) {
            downloadStatus = "Offline Scanner is Ready."
            isModelReady = true
        } else {
            if (modelFile.exists()) modelFile.delete()
            if (vocabFile.exists()) vocabFile.delete()

            if (isNetOn) {
                downloadStatus = "Downloading Offline Files..."
                val success = downloadOfflineResources(context)
                if (success) {
                    downloadStatus = "Offline Scanner is Ready."
                    isModelReady = true
                } else {
                    downloadStatus = "Download failed. Server error."
                }
            } else {
                downloadStatus = "Files missing! Connect to internet."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        CleanCyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ১. Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ScreenSearchDesktop, contentDescription = "Scanner", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN MESSAGE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            // ২. Status Indicators
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if(isModelReady) Icons.Default.CloudDone else Icons.Default.CloudDownload, contentDescription = null, tint = if(isModelReady) Color.Green else Color.Yellow, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(downloadStatus, color = if(isModelReady) Color.Green else Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                val engineColor = if (isNetOn) NeonCyan else Color(0xFFFF9800)
                val engineText = if (isNetOn) "USING ONLINE SCANNER (CLOUD)" else "USING OFFLINE SCANNER (LOCAL)"

                Surface(
                    color = engineColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, engineColor.copy(alpha = 0.5f))
                ) {
                    Text(text = engineText, color = engineColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            // ৩. Input Area
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = Color(0xFF131C2B).copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = payloadInput,
                        onValueChange = { payloadInput = it },
                        placeholder = { Text("Paste suspicious text or link...", color = Color.Gray, fontSize = 12.sp) },
                        trailingIcon = { IconButton(onClick = { payloadInput = "" }) { Icon(Icons.Default.ContentPaste, contentDescription = "Clear", tint = Color.Gray) } },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan, focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (payloadInput.trim().isEmpty()) {
                                Toast.makeText(context, "Please paste some text first!", Toast.LENGTH_SHORT).show()
                            } else {
                                scanState = 1
                                scanLogs.clear()
                                coroutineScope.launch {
                                    if (isNetOn) {
                                        runServerAIAnalysis(context, payloadInput, scanLogs) { scanState = it }
                                    } else {
                                        if (isModelReady) {
                                            runLocalTFLiteAnalysis(context, payloadInput, scanLogs) { scanState = it }
                                        } else {
                                            scanLogs.add("ERROR: Offline files are missing.")
                                            scanState = 0
                                        }
                                    }
                                }
                            }
                        },

                        enabled = scanState != 1,
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = if (scanState == 1) Color.DarkGray else NeonPurple)
                    ) {
                        Text(if (scanState == 1) "SCANNING..." else "START SCAN", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            // ৪. Animation Box (🔥 UPDATED FOR 4 LEVELS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (scanState) {
                    0 -> AdvancedIdleState()
                    1 -> AdvancedActiveScanningState()
                    2 -> AdvancedResultState(level = 2) // Safe
                    3 -> AdvancedResultState(level = 3) // Promo
                    4 -> AdvancedResultState(level = 4) // Spam
                    5 -> AdvancedResultState(level = 5) // Danger
                }
            }

            // ৫. Logs Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(horizontal = 20.dp)
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                color = Color(0xFF0B1221).copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SCAN PROGRESS", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 6.dp))

                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), reverseLayout = false) {
                        items(scanLogs) { log ->
                            // 🔥 Color Mapping for 4 Levels
                            val textColor = when {
                                log.contains("DANGER") || log.contains("❌") || log.contains("ERROR") -> Color.Red
                                log.contains("SPAM") || log.contains("WARNING") -> Color(0xFFFF9800)
                                log.contains("PROMO") || log.contains("INFO") -> Color(0xFF3B82F6)
                                log.contains("✔") || log.contains("SAFE") -> Color.Green
                                else -> NeonCyan
                            }
                            Text(text = log, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp), fontWeight = FontWeight.Medium)
                        }

                        if (scanState == 1) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning in progress...", color = NeonCyan, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. ONLINE CLOUD AI LOGIC
// ==========================================
suspend fun runServerAIAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("Checking internet connection...")
    delay(400)
    logs.add("Connecting to secure server...")
    delay(500)
    logs.add("Sending your text securely...")

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val reporterId = sharedPref.getString("username", "Agent") ?: "Agent"
    val userRegion = sharedPref.getString("user_division", "") ?: ""

    var deviceId = sharedPref.getString("anonymous_device_id", null)
    if (deviceId == null) {
        deviceId = java.util.UUID.randomUUID().toString()
        sharedPref.edit().putString("anonymous_device_id", deviceId).apply()
    }

    val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"

    if (token.isEmpty()) {
        logs.add("ERROR: You are not logged in!")
        onResult(0)
        return
    }

    withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "sender" to "Manual AI Scan",
                "message" to payloadData,
                "reporter" to reporterId,
                "region" to userRegion,
                "deviceId" to deviceId,
                "deviceModel" to deviceModel,
                "osVersion" to osVersion
            )
            val response = RetrofitClient.apiService.submitSmsReport("Bearer $token", payload)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    logs.add("Message received by server. ✔")
                    delay(800)
                    logs.add("Analyzing threat level from Cloud AI...")
                    delay(1500)

                    val resultBody = response.body() as? Map<*, *>
                    val aiResult = resultBody?.get("aiResult")?.toString()?.uppercase() ?: "SAFE"

                    // 🔥 Map Cloud Result to 4 States
                    when {
                        aiResult.contains("DANGER") || aiResult.contains("SCAM") || aiResult.contains("PHISHING") -> {
                            logs.add("DANGER: Malicious intent detected! ❌")
                            onResult(5)
                        }
                        aiResult.contains("SPAM") -> {
                            logs.add("WARNING: Spam content detected. ⚠️")
                            onResult(4)
                        }
                        aiResult.contains("PROMO") -> {
                            logs.add("INFO: Promotional content found. ℹ️")
                            onResult(3)
                        }
                        else -> {
                            logs.add("Scan finished. No threats found. ✔")
                            onResult(2)
                        }
                    }
                } else {
                    logs.add("ERROR: Server rejected the request.")
                    onResult(0)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("ERROR: Server is down or no internet.")
                onResult(0)
            }
        }
    }
}

// ==========================================
// 2. OFFLINE LOCAL TFLITE LOGIC (USES SpamDetector.kt)
// ==========================================
suspend fun runLocalTFLiteAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("No internet. Initializing Local AI...")
    delay(400)

    val detector = SpamDetector.getInstance(context)

    withContext(Dispatchers.Main) {
        logs.add("Loading Prohory Neural Engine...")
        delay(600)
        logs.add("Analyzing message with AI...")
    }

    val labelIndex = suspendCoroutine<Int> { continuation ->
        detector.classify(payloadData) { index, _ ->
            continuation.resume(index)
        }
    }

    withContext(Dispatchers.Main) {
        delay(600)
        // 🔥 Map TFLite Result (0=Safe, 1=Promo, 2=Spam, 3=Danger) to UI States (2=Safe, 3=Promo, 4=Spam, 5=Danger)
        when (labelIndex) {
            3 -> {
                logs.add("DANGER: Malicious pattern found! ❌")
                onResult(5)
            }
            2 -> {
                logs.add("WARNING: Spam content detected. ⚠️")
                onResult(4)
            }
            1 -> {
                logs.add("INFO: Promotional content found. ℹ️")
                onResult(3)
            }
            else -> {
                logs.add("Scan finished. Safe. ✔")
                onResult(2)
            }
        }
    }
}

// ==========================================
// 3. DOWNLOADER LOGIC
// ==========================================
suspend fun downloadOfflineResources(context: Context): Boolean {
    val modelUrl = "https://pub-43adda752ae546939758ef344e12b935.r2.dev/cyberaware_model.tflite"
    val vocabUrl = "https://pub-43adda752ae546939758ef344e12b935.r2.dev/vocab.txt"

    val modelFile = File(context.filesDir, "cyberaware_model.tflite")
    val vocabFile = File(context.filesDir, "vocab.txt")

    return withContext(Dispatchers.IO) {
        val modelSuccess = if (!modelFile.exists()) downloadSingleFile(modelUrl, modelFile) else true
        val vocabSuccess = if (!vocabFile.exists()) downloadSingleFile(vocabUrl, vocabFile) else true
        modelSuccess && vocabSuccess
    }
}

private fun downloadSingleFile(urlStr: String, destFile: File): Boolean {
    return try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val input = connection.inputStream
            val output = FileOutputStream(destFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
            output.close()
            input.close()
            return true
        } else {
            return false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

// ==========================================
// 🔥 HIGHLY ADVANCED ANIMATION COMPONENTS 🔥
// ==========================================

@Composable
fun AdvancedIdleState() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.size(100.dp).scale(pulse).background(Color(0xFF1E293B), CircleShape).border(2.dp, Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
    }
}

@Composable
fun AdvancedActiveScanningState() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart))
    val fastRotation by infiniteTransition.animateFloat(initialValue = 360f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart))
    val pulse by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp).scale(pulse).background(NeonCyan.copy(alpha = 0.1f), CircleShape))

        Canvas(modifier = Modifier.size(120.dp).graphicsLayer { rotationZ = rotation }) {
            drawArc(color = NeonCyan.copy(alpha = 0.5f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))))
            drawArc(color = NeonPurple, startAngle = 0f, sweepAngle = 90f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
        }

        Canvas(modifier = Modifier.size(90.dp).graphicsLayer { rotationZ = fastRotation }) {
            drawArc(color = NeonCyan, startAngle = 0f, sweepAngle = 120f, useCenter = false, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }

        Box(modifier = Modifier.size(70.dp).background(Color(0xFF131C2B), CircleShape).border(2.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ScreenSearchDesktop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(30.dp))
        }
    }
}

// 🔥 UPDATED: Dynamic UI for 4 Levels
@Composable
fun AdvancedResultState(level: Int) {
    val infiniteTransition = rememberInfiniteTransition()

    val (color, icon, text) = when (level) {
        2 -> Triple(Color.Green, Icons.Default.HealthAndSafety, "SAFE")
        3 -> Triple(Color(0xFF3B82F6), Icons.Default.LocalOffer, "PROMO") // Blue for Promo
        4 -> Triple(Color(0xFFFF9800), Icons.Default.ReportProblem, "SPAM") // Orange for Spam
        5 -> Triple(Color.Red, Icons.Default.GppBad, "DANGER") // Red for Danger
        else -> Triple(Color.DarkGray, Icons.Default.Info, "UNKNOWN")
    }

    val auraScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.4f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse))
    val auraAlpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(100.dp).scale(if(level >= 4) auraScale else 1f).background(color.copy(alpha = if(level >= 4) auraAlpha else 0.2f), CircleShape))
            Box(modifier = Modifier.size(100.dp).background(color.copy(alpha = 0.2f), CircleShape).border(3.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(50.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
    }
}

@Composable
fun CleanCyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 120f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()

        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.04f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.04f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}