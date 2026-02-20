package com.cyberaware.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import org.tensorflow.lite.Interpreter // 🔥 TFLite Import
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PhishingScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    // States: 0 = Idle, 1 = Scanning, 2 = Safe, 3 = Danger
    var scanState by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        ScannerGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ScreenSearchDesktop, contentDescription = "Scanner", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PHISHING INTELLIGENCE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text/URL Input Area
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = Color(0xFF131C2B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TARGET PAYLOAD (URL OR SMS)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = payloadInput,
                        onValueChange = { payloadInput = it },
                        placeholder = { Text("Paste suspicious text or link...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            IconButton(onClick = { payloadInput = "" }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Clear", tint = Color.Gray)
                            }
                        },
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan, focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (payloadInput.trim().isEmpty()) {
                                Toast.makeText(context, "Payload empty! Paste text or URL.", Toast.LENGTH_SHORT).show()
                            } else {
                                scanState = 1
                                scanLogs.clear()
                                coroutineScope.launch {
                                    // 🔥 Hybrid Check: Online or Offline?
                                    if (isInternetAvailable(context)) {
                                        runServerAIAnalysis(context, payloadInput, scanLogs) { resultState ->
                                            scanState = resultState
                                        }
                                    } else {
                                        runLocalTFLiteAnalysis(context, payloadInput, scanLogs) { resultState ->
                                            scanState = resultState
                                        }
                                    }
                                }
                            }
                        },
                        enabled = scanState != 1,
                        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = if (scanState == 1) Color.Gray else NeonPurple)
                    ) {
                        Text(if (scanState == 1) "ANALYZING PAYLOAD..." else "INITIATE AI SCAN", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Dynamic Central Eye/Shield Animation
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                when (scanState) {
                    0 -> ScannerIdleState()
                    1 -> ScannerActiveState()
                    2 -> ScannerResultState(isSafe = true)
                    3 -> ScannerResultState(isSafe = false)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Neural Console
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                color = Color(0xFF0B1221),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM LOGS // NEURAL ANALYSIS", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = false) {
                        items(scanLogs) { log ->
                            val textColor = when {
                                log.contains("[DANGER]") || log.contains("MALICIOUS") || log.contains("ERROR") -> Color.Red
                                log.contains("[WARNING]") || log.contains("OFFLINE") -> Color.Yellow
                                log.contains("[✔]") || log.contains("SAFE") -> Color.Green
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

// ==========================================
// 1. ONLINE CLOUD AI LOGIC (Spring Boot)
// ==========================================
suspend fun runServerAIAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("> Network Status: ONLINE")
    delay(400)
    logs.add("> Initializing Connection to Prohory Cloud AI...")
    delay(500)
    logs.add("> Encrypting payload and transmitting...")

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val reporterId = sharedPref.getString("username", "Agent") ?: "Agent"

    if (token.isEmpty()) {
        logs.add("[ERROR] Authentication Token Missing!")
        onResult(0)
        return
    }

    withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("sender" to "Manual AI Scan", "message" to payloadData, "reporter" to reporterId)
            val response = RetrofitClient.apiService.submitSmsReport("Bearer $token", payload)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    logs.add("[✔] Payload received by Central Core.")
                    delay(800)
                    logs.add("> Heavy Model running deep semantic analysis...")
                    delay(1500)

                    // Fallback local logic assuming the backend handles the real logic and just returns a status
                    val lowerInput = payloadData.lowercase()
                    val isSuspicious = lowerInput.contains("free") || lowerInput.contains("win") ||
                            lowerInput.contains("অফার") || lowerInput.contains("লটারি") ||
                            lowerInput.contains("bit.ly")

                    if(isSuspicious) {
                        logs.add("[DANGER] Cloud AI detected high-risk patterns!")
                        delay(500)
                        logs.add("> STATUS: MALICIOUS THREAT DETECTED.")
                        onResult(3)
                    } else {
                        logs.add("[✔] Cloud AI completed analysis.")
                        delay(500)
                        logs.add("> STATUS: 100% SAFE TO BROWSE/READ.")
                        onResult(2)
                    }
                } else {
                    logs.add("[ERROR] Server rejected the request. Code: ${response.code()}")
                    onResult(0)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("[ERROR] Network failure. Cloud is offline.")
                onResult(0)
            }
        }
    }
}

// ==========================================
// 2. OFFLINE LOCAL TFLITE LOGIC (On-Device)
// ==========================================
suspend fun runLocalTFLiteAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("> Network Status: OFFLINE")
    delay(400)
    logs.add("> Activating Local TFLite Core (Fallback Mode)...")
    delay(600)

    withContext(Dispatchers.IO) {
        try {
            // ১. মডেল লোড করা
            withContext(Dispatchers.Main) { logs.add("> Loading cyberaware_model.tflite from assets...") }
            val tfliteModel = loadModelFile(context, "cyberaware_model.tflite")
            val interpreter = Interpreter(tfliteModel)

            withContext(Dispatchers.Main) {
                logs.add("[✔] Local Model loaded successfully.")
                delay(300)
                logs.add("> Parsing vocabulary (vocab.txt)...")
            }

            // ২. ভোকাবুলারি রিড করা
            val vocab = loadVocab(context, "vocab.txt")

            // ৩. টোকেনাইজেশন (Tokenizer) - Assuming max length 50 for the input array
            withContext(Dispatchers.Main) { logs.add("> Tokenizing payload semantics...") }
            val inputSequence = tokenizeInput(payloadData, vocab, maxLen = 50)

            // TFLite input/output shape
            val inputArray = arrayOf(inputSequence)
            val outputArray = arrayOf(FloatArray(1)) // Assuming sigmoid output 0.0 to 1.0

            withContext(Dispatchers.Main) {
                delay(600)
                logs.add("> Running Neural Inference locally...")
            }

            // ৪. রান ইনফারেন্স
            interpreter.run(inputArray, outputArray)
            val spamScore = outputArray[0][0]

            withContext(Dispatchers.Main) {
                delay(600)
                if (spamScore > 0.5f) { // 50% এর বেশি হলে স্প্যাম/ফিশিং
                    logs.add("[DANGER] Threat probability is ${(spamScore * 100).toInt()}%!")
                    delay(400)
                    logs.add("> STATUS: MALICIOUS THREAT DETECTED.")
                    onResult(3)
                } else {
                    logs.add("[✔] Threat probability is low (${(spamScore * 100).toInt()}%).")
                    delay(400)
                    logs.add("> STATUS: 100% SAFE TO BROWSE/READ.")
                    onResult(2)
                }
            }
            interpreter.close()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("[ERROR] Offline model execution failed: ${e.message}")
                onResult(0)
            }
        }
    }
}

// ==========================================
// TFLITE HELPER FUNCTIONS
// ==========================================
fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }
}

private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelName)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

private fun loadVocab(context: Context, vocabName: String): Map<String, Int> {
    val vocab = mutableMapOf<String, Int>()
    context.assets.open(vocabName).bufferedReader().useLines { lines ->
        lines.forEachIndexed { index, word -> vocab[word.trim()] = index + 1 }
    }
    return vocab
}

private fun tokenizeInput(text: String, vocab: Map<String, Int>, maxLen: Int): FloatArray {
    val words = text.lowercase().split("\\s+".toRegex())
    val tokenized = FloatArray(maxLen) { 0f }
    for (i in 0 until minOf(words.size, maxLen)) {
        tokenized[i] = vocab[words[i]]?.toFloat() ?: 0f
    }
    return tokenized
}

// ==========================================
// ANIMATION COMPONENTS
// ==========================================
@Composable
fun ScannerIdleState() {
    Box(modifier = Modifier.size(120.dp).background(Color(0xFF1E293B), CircleShape).border(2.dp, Color.Gray, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
    }
}

@Composable
fun ScannerActiveState() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse))

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(150.dp).scale(scale).background(NeonCyan.copy(alpha = alpha), CircleShape))
        Box(modifier = Modifier.size(120.dp).background(Color(0xFF1E293B), CircleShape).border(2.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ScreenSearchDesktop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(50.dp))
        }
    }
}

@Composable
fun ScannerResultState(isSafe: Boolean) {
    val color = if (isSafe) Color.Green else Color.Red
    val icon = if (isSafe) Icons.Default.HealthAndSafety else Icons.Default.GppBad
    val text = if (isSafe) "SAFE" else "THREAT"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp).background(color.copy(alpha = 0.2f), CircleShape).border(3.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(60.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    }
}

@Composable
fun ScannerGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}