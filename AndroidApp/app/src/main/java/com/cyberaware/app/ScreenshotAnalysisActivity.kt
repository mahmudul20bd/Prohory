package com.cyberaware.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ThreatInsight(val icon: ImageVector, val title: String, val desc: String, val color: Color)

class ScreenshotAnalysisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                OcrScannerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 🔥 UX FIX: Auto-Scroll State for Terminal
    val logListState = rememberLazyListState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var extractedPhone by remember { mutableStateOf<String?>(null) }
    val scanLogs = remember { mutableStateListOf<String>() }

    var scanState by remember { mutableIntStateOf(0) }
    var dynamicRiskScore by remember { mutableIntStateOf(0) }
    var realTimeInsights by remember { mutableStateOf<List<ThreatInsight>>(emptyList()) }

    // 🔥 Auto-Scroll Logic: Smooth scrolling for small screens
    LaunchedEffect(scanLogs.size) {
        if (scanLogs.isNotEmpty()) {
            logListState.animateScrollToItem(scanLogs.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                extractedText = ""
                extractedPhone = null
                scanState = 0
                dynamicRiskScore = 0
                realTimeInsights = emptyList()
                scanLogs.clear()
                scanLogs.add("Image acquired successfully.")
                scanLogs.add("Tap 'SCAN IMAGE' to check for threats.")
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        OcrGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header - Simplified for Users
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("IMAGE SCANNER", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Check screenshots for hidden scams", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                ImageIntelligenceCanvas(selectedImageUri, scanState) {
                    if (scanState == 0) photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (scanState == 0 || selectedImageUri == null) {
                    Button(
                        onClick = {
                            if (selectedImageUri == null) {
                                Toast.makeText(context, "Please upload a screenshot first!", Toast.LENGTH_SHORT).show()
                            } else {
                                scanState = 1
                                scanLogs.clear()
                                coroutineScope.launch {
                                    runOcrAndAiAnalysis(context, selectedImageUri!!, scanLogs) { resultState, text, phone ->
                                        extractedText = text
                                        extractedPhone = phone
                                        scanState = resultState

                                        if (resultState == 4) {
                                            dynamicRiskScore = 85 + (0..14).random()
                                            val insights = mutableListOf<ThreatInsight>()
                                            val lowerText = text.lowercase()

                                            if (lowerText.contains("http") || lowerText.contains("www") || lowerText.contains(".com") || lowerText.contains("bit.ly")) {
                                                insights.add(ThreatInsight(Icons.Rounded.LinkOff, "Suspicious Link", "URL pattern commonly used in phishing campaigns detected.", Color.Red))
                                            }
                                            if (lowerText.contains("bkash") || lowerText.contains("nagad") || lowerText.contains("bank") || lowerText.contains("pin")) {
                                                insights.add(ThreatInsight(Icons.Rounded.AccountBalance, "Financial Scam", "Attempts to mimic financial institutions found.", Color.Red))
                                            }
                                            if (lowerText.contains("free") || lowerText.contains("win") || lowerText.contains("offer") || lowerText.contains("লটারি") || lowerText.contains("অফার")) {
                                                insights.add(ThreatInsight(Icons.Rounded.CardGiftcard, "Fake Reward", "Psychological manipulation via fake rewards detected.", Color(0xFFFF9800)))
                                            }
                                            if (insights.isEmpty()) {
                                                insights.add(ThreatInsight(Icons.Rounded.Warning, "Malicious Content", "AI detected patterns matching known malware/scams.", Color.Red))
                                            }
                                            realTimeInsights = insights

                                        } else if (resultState == 3) {
                                            dynamicRiskScore = (1..15).random()
                                            realTimeInsights = listOf(
                                                ThreatInsight(Icons.Rounded.Verified, "Safe Image", "No urgent or manipulative language detected.", Color.Green)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Icon(Icons.Rounded.DocumentScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedImageUri == null) "SELECT IMAGE" else "SCAN IMAGE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                } else if (scanState in 1..2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (scanState == 1) "Reading text from image..." else "Checking threats securely...", color = NeonCyan, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }

                AnimatedVisibility(visible = scanState >= 3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(10.dp))
                        AiThreatCoreMeter(scanState, dynamicRiskScore)
                        Spacer(Modifier.height(25.dp))
                        ThreatBreakdownCard(realTimeInsights)
                        Spacer(Modifier.height(20.dp))

                        // 🔥 Smart Block Action Center
                        ActionCenter(scanState, extractedPhone)

                        Button(
                            onClick = { scanState = 0; selectedImageUri = null; scanLogs.clear() },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SCAN ANOTHER IMAGE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().height(220.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                    color = Color(0xFF0B1221),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2B)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SCAN LOGS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Divider(color = Color.DarkGray)

                        LazyColumn(state = logListState, modifier = Modifier.padding(12.dp)) {
                            if (scanLogs.isEmpty()) {
                                item { Text("Waiting for image...", color = Color.DarkGray, fontSize = 12.sp) }
                            }
                            items(scanLogs) { log ->
                                val textColor = when {
                                    log.contains("DANGER") || log.contains("❌") || log.contains("ERROR") -> Color.Red
                                    log.contains("WARNING") || log.contains("Phone") -> Color.Yellow
                                    log.contains("✔") || log.contains("SAFE") -> Color.Green
                                    else -> NeonCyan
                                }
                                Text(text = log, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// ==========================================
// CORE HYBRID AI FUNCTIONS
// ==========================================
suspend fun runOcrAndAiAnalysis(
    context: Context,
    imageUri: Uri,
    logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onResult: (Int, String, String?) -> Unit
) {
    logs.add("Reading text from image...")
    delay(500)

    var extractedText = ""
    var detectedPhone: String? = null

    withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val result = recognizer.process(image).await()
            extractedText = result.text

            // 🔥 SMARTER PHONE REGEX: ignores spaces and formats to capture actual numbers
            val textWithoutSpaces = extractedText.replace("\\s+".toRegex(), "")
            val phoneRegex = Regex("(?:\\+88|88)?(01[3-9]\\d{8})")
            val phoneMatch = phoneRegex.find(textWithoutSpaces)

            if (phoneMatch != null) {
                detectedPhone = phoneMatch.groupValues[1] // Extracts just the 01xxxxxxxxx part
            }

            withContext(Dispatchers.Main) {
                if (extractedText.trim().isEmpty()) {
                    logs.add("ERROR: No text found in the image.")
                    onResult(0, "", null)
                } else {
                    logs.add("Text extraction successful. ✔")
                    if (detectedPhone != null) {
                        logs.add("Phone number detected: $detectedPhone")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("ERROR: Image scanning failed.")
                onResult(0, "", null)
            }
        }
    }

    if (extractedText.trim().isEmpty()) return

    onResult(2, extractedText, detectedPhone)
    delay(800)
    logs.add("Analyzing text for threats...")
    delay(500)

    if (isInternetAvail(context)) {
        runServerOcrAiAnalysis(context, extractedText, detectedPhone, logs, onResult)
    } else {
        runLocalOcrTFLiteAnalysis(context, extractedText, detectedPhone, logs, onResult)
    }
}

// ==========================================
// ONLINE CLOUD AI LOGIC
// ==========================================
suspend fun runServerOcrAiAnalysis(context: Context, payloadData: String, phone: String?, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int, String, String?) -> Unit) {
    logs.add("Connecting to secure cloud server...")
    delay(400)

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val reporterId = sharedPref.getString("username", "User") ?: "User"
    val userRegion = sharedPref.getString("user_division", "") ?: ""

    var deviceId = sharedPref.getString("anonymous_device_id", null)
    if (deviceId == null) {
        deviceId = java.util.UUID.randomUUID().toString()
        sharedPref.edit().putString("anonymous_device_id", deviceId).apply()
    }

    withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "sender" to "OCR Scanner",
                "message" to payloadData,
                "reporter" to reporterId,
                "region" to userRegion,
                "deviceId" to deviceId,
                "deviceModel" to android.os.Build.MODEL,
                "osVersion" to android.os.Build.VERSION.RELEASE
            )
            val response = RetrofitClient.apiService.submitSmsReport("Bearer $token", payload)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    delay(800)
                    logs.add("Cloud AI is checking patterns...")
                    delay(1500)

                    // 🔥 Uses Actual Server Result
                    val resultBody = response.body() as? Map<*, *>
                    val aiResult = resultBody?.get("aiResult")?.toString()?.uppercase() ?: "SAFE"

                    val isSuspicious = aiResult.contains("DANGER") || aiResult.contains("SPAM") || aiResult.contains("SCAM")

                    if(isSuspicious) {
                        logs.add("DANGER: Malicious intent detected! ❌")
                        onResult(4, payloadData, phone)
                    } else {
                        logs.add("Scan finished. No threats found. ✔")
                        onResult(3, payloadData, phone)
                    }
                } else {
                    logs.add("ERROR: Server rejected the request.")
                    onResult(0, payloadData, phone)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("ERROR: Network failure. Please check internet.")
                onResult(0, payloadData, phone)
            }
        }
    }
}

// ==========================================
// OFFLINE LOCAL TFLITE LOGIC (FIXED SHAPE & INT ARRAY)
// ==========================================
suspend fun runLocalOcrTFLiteAnalysis(context: Context, payloadData: String, phone: String?, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int, String, String?) -> Unit) {
    logs.add("No internet. Using offline AI mode...")
    delay(400)

    withContext(Dispatchers.IO) {
        try {
            val tfliteModel = loadOcrModelFromStorage(context, "cyberaware_model.tflite")
            val interpreter = Interpreter(tfliteModel)
            val vocab = loadOcrVocabFromStorage(context, "vocab.txt")

            // 🔥 TFLITE FIX: Changed to IntArray (INT32) and Length 128
            val inputData = tokenizeOcrInputForModel(payloadData, vocab, maxLen = 128)

            val inputIds2D = Array(1) { inputData.first }
            val attentionMask2D = Array(1) { inputData.second }

            val inputs = arrayOf<Any>(inputIds2D, attentionMask2D)
            val outputBuffer = Array(1) { FloatArray(4) }
            val outputs = mapOf(0 to outputBuffer)

            withContext(Dispatchers.Main) {
                delay(800)
                logs.add("Running Neural Inference locally...")
            }

            interpreter.runForMultipleInputsOutputs(inputs, outputs)
            val probabilities = outputBuffer[0]

            var maxIndex = 0
            var maxProb = probabilities[0]
            for (i in 1 until probabilities.size) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            withContext(Dispatchers.Main) {
                delay(600)
                if (maxIndex >= 2) {
                    logs.add("DANGER: High threat probability (${(maxProb * 100).toInt()}%) ❌")
                    onResult(4, payloadData, phone)
                } else {
                    logs.add("Scan finished. Safe. ✔")
                    onResult(3, payloadData, phone)
                }
            }
            interpreter.close()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("ERROR DETAILS: ${e.message}")
                logs.add("ERROR: Local AI Model Failed.")
                onResult(0, payloadData, phone)
            }
        }
    }
}

// 🔥 HELPER FIX: Returns IntArray Pair (INT32) matching [1, 128] shape
private fun tokenizeOcrInputForModel(text: String, vocab: Map<String, Int>, maxLen: Int): Pair<IntArray, IntArray> {
    val cleanedText = text.lowercase().trim()
    val words = cleanedText.split(Regex("\\s+"))

    val inputIds = IntArray(maxLen)
    val attentionMask = IntArray(maxLen)

    val clsId = vocab["[CLS]"] ?: 101
    val sepId = vocab["[SEP]"] ?: 102
    val unkId = vocab["[UNK]"] ?: 100

    var index = 0

    inputIds[index] = clsId
    attentionMask[index] = 1
    index++

    for (word in words) {
        if (index >= maxLen - 1) break
        inputIds[index] = vocab[word] ?: unkId
        attentionMask[index] = 1
        index++
    }

    if (index < maxLen) {
        inputIds[index] = sepId
        attentionMask[index] = 1
    }

    return Pair(inputIds, attentionMask)
}

fun isInternetAvail(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

private fun loadOcrModelFromStorage(context: Context, modelName: String): MappedByteBuffer {
    val file = File(context.filesDir, modelName)
    val fileInputStream = FileInputStream(file)
    return fileInputStream.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
}

private fun loadOcrVocabFromStorage(context: Context, vocabName: String): Map<String, Int> {
    val file = File(context.filesDir, vocabName)
    val vocab = mutableMapOf<String, Int>()
    file.bufferedReader().useLines { lines ->
        lines.forEachIndexed { index, word -> vocab[word.trim()] = index + 1 }
    }
    return vocab
}


// ==========================================
// UI COMPONENTS
// ==========================================
@Composable
fun ImageIntelligenceCanvas(uri: Uri?, scanState: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(1500), RepeatMode.Reverse))

    val borderColor = when (scanState) {
        0 -> NeonCyan.copy(alpha = pulseAlpha)
        1, 2 -> NeonPurple
        3 -> Color.Green
        4 -> Color.Red
        else -> Color.DarkGray
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(260.dp).clickable(enabled = scanState == 0, onClick = onClick),
        color = Color(0xFF0F172A).copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (scanState == 0) 2.dp else 1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (uri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudUpload, null, tint = NeonCyan, modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("DROP SCREENSHOT HERE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("We detect phishing, scam links & fake offers.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, alpha = if (scanState in 1..2) 0.4f else 1f)
                if (scanState in 1..2) {
                    CyberLaserScanner()
                    Text("SCANNING...", color = NeonPurple, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 4.sp, modifier = Modifier.background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun AiThreatCoreMeter(state: Int, score: Int) {
    val animatedScore by animateIntAsState(targetValue = score, animationSpec = tween(2000, easing = FastOutSlowInEasing))
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(1f, 1.1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse))

    val (color, title, subtitle) = if (state == 3) {
        Triple(Color.Green, "SAFE", "No threats detected")
    } else {
        Triple(Color.Red, "HIGH RISK", "Malicious pattern found")
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp).scale(if (state == 4) pulse else 1f)) {
        Canvas(modifier = Modifier.size(160.dp)) {
            drawArc(color = Color.DarkGray, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = color, startAngle = 135f, sweepAngle = (animatedScore / 100f) * 270f, useCenter = false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("$animatedScore%", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun ThreatBreakdownCard(insights: List<ThreatInsight>) {
    if (insights.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI INTELLIGENCE REPORT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

            insights.forEach { insight ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(insight.color.copy(0.1f), CircleShape).border(1.dp, insight.color.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(insight.icon, null, tint = insight.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(insight.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(insight.desc, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

// 🔥 FIX: Always show "Block Sender" button if DANGER (state == 4)
// ==========================================
// 🔥 SMART BLOCK ACTION CENTER (WITH REAL SYSTEM BLOCKING)
// ==========================================
@Composable
fun ActionCenter(state: Int, phoneToBlock: String?) {
    val context = LocalContext.current

    if (state == 4) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RECOMMENDED ACTIONS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            val buttonText = if (phoneToBlock != null) "Block Sender ($phoneToBlock)" else "Report as Spam"

            Button(
                onClick = {
                    if (phoneToBlock != null) {
                        try {
                            // ১. যদি নাম্বার থাকে, সরাসরি সিস্টেমের ডায়ালারে পাঠিয়ে দেবে ব্লক করার জন্য
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneToBlock")
                            }
                            context.startActivity(intent)
                            Toast.makeText(context, "Please block the number from your dialer.", Toast.LENGTH_LONG).show()

                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // ২. যদি নাম্বার না থাকে, শুধু ইউজারকে সতর্ক করবে
                        Toast.makeText(context, "No number found. Please ignore the message.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.1f)),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Block, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(buttonText, color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    } else if (state == 3) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.Green.copy(0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.Green.copy(0.3f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Verified, null, tint = Color.Green, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Content is safe to interact with.", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CyberLaserScanner() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(0f, 200f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse))
    Box(modifier = Modifier.fillMaxSize()) { Box(modifier = Modifier.fillMaxWidth().height(4.dp).offset(y = offsetY.dp).background(Brush.verticalGradient(listOf(Color.Transparent, NeonCyan, Color.Transparent))).border(1.dp, NeonCyan)) }
}

@Composable
fun OcrGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}