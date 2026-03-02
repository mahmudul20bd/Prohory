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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ThreatInsight(val icon: ImageVector, val title: String, val desc: String, val color: Color)

fun saveImageScanToHistory(context: Context, content: String, threatLevel: String, sourceName: String) {
    val sharedPref = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
    val currentThreats = sharedPref.getStringSet("scan_history_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    val logEntry = "$sourceName Entry|||$content|||${System.currentTimeMillis()}|||$threatLevel|||$sourceName"

    currentThreats.add(logEntry)
    sharedPref.edit().putStringSet("scan_history_list", currentThreats).apply()
}

class ScreenshotAnalysisActivity : ComponentActivity() {
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

    val logListState = rememberLazyListState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var extractedPhone by remember { mutableStateOf<String?>(null) }
    val scanLogs = remember { mutableStateListOf<String>() }

    var scanState by remember { mutableIntStateOf(0) }
    var dynamicRiskScore by remember { mutableIntStateOf(0) }
    var realTimeInsights by remember { mutableStateOf<List<ThreatInsight>>(emptyList()) }

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
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                scanState = 1
                                scanLogs.clear()
                                coroutineScope.launch {
                                    runOcrAndAiAnalysis(context, selectedImageUri!!, scanLogs) { resultState, text, phone ->
                                        extractedText = text
                                        extractedPhone = phone
                                        scanState = resultState

                                        val insights = mutableListOf<ThreatInsight>()
                                        val lowerText = text.lowercase()

                                        when (resultState) {
                                            5 -> {
                                                dynamicRiskScore = 85 + (0..14).random()
                                                if (lowerText.contains("bkash") || lowerText.contains("nagad") || lowerText.contains("bank") || lowerText.contains("pin")) {
                                                    insights.add(ThreatInsight(Icons.Rounded.AccountBalance, "Financial Scam", "Attempts to mimic financial institutions found.", Color.Red))
                                                }
                                                if (lowerText.contains("http") || lowerText.contains("www") || lowerText.contains(".com") || lowerText.contains("bit.ly")) {
                                                    insights.add(ThreatInsight(Icons.Rounded.LinkOff, "Phishing Link", "Dangerous URL designed to steal credentials.", Color.Red))
                                                }
                                                if (insights.isEmpty()) insights.add(ThreatInsight(Icons.Rounded.Warning, "Malicious Content", "AI detected patterns matching known malware/scams.", Color.Red))
                                            }
                                            4 -> {
                                                dynamicRiskScore = 50 + (0..20).random()
                                                insights.add(ThreatInsight(Icons.Rounded.ReportProblem, "Spam Message", "Unsolicited or repetitive message detected.", Color(0xFFFF9800)))
                                                if (lowerText.contains("free") || lowerText.contains("win") || lowerText.contains("lottery")) {
                                                    insights.add(ThreatInsight(Icons.Rounded.CardGiftcard, "Fake Reward", "Common spam tactic using false promises.", Color(0xFFFF9800)))
                                                }
                                            }
                                            3 -> {
                                                dynamicRiskScore = 15 + (0..15).random()
                                                insights.add(ThreatInsight(Icons.Rounded.LocalOffer, "Promotional Content", "Message appears to be marketing or advertisement.", Color(0xFF3B82F6)))
                                            }
                                            2 -> {
                                                dynamicRiskScore = (1..10).random()
                                                insights.add(ThreatInsight(Icons.Rounded.Verified, "Safe Image", "No urgent, manipulative, or malicious language detected.", Color.Green))
                                            }
                                        }
                                        realTimeInsights = insights
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
                } else if (scanState == 1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Reading text & checking threats...", color = NeonCyan, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }

                AnimatedVisibility(visible = scanState >= 2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(10.dp))
                        AiThreatCoreMeter(scanState, dynamicRiskScore)
                        Spacer(Modifier.height(25.dp))
                        ThreatBreakdownCard(realTimeInsights)
                        Spacer(Modifier.height(20.dp))

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
                                    log.contains("SPAM") || log.contains("WARNING") -> Color(0xFFFF9800)
                                    log.contains("PROMO") -> Color(0xFF3B82F6)
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

            val textWithoutSpaces = extractedText.replace("\\s+".toRegex(), "")
            val phoneRegex = Regex("(?:\\+88|88)?(01[3-9]\\d{8})")
            val phoneMatch = phoneRegex.find(textWithoutSpaces)

            if (phoneMatch != null) {
                detectedPhone = phoneMatch.groupValues[1]
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

    delay(800)
    logs.add("Analyzing text for threats...")
    delay(500)

    if (isInternetAvail(context)) {
        runServerOcrAiAnalysis(context, imageUri, extractedText, detectedPhone, logs, onResult)
    } else {
        runLocalOcrTFLiteAnalysis(context, extractedText, detectedPhone, logs, onResult)
    }
}

suspend fun runServerOcrAiAnalysis(
    context: Context,
    uri: Uri,
    localExtractedText: String,
    phone: String?,
    logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onResult: (Int, String, String?) -> Unit
) {
    withContext(Dispatchers.Main) {
        logs.add("Preparing HD Image for Bangla OCR...")
    }

    // 🔥 Retrieve the username and region from SharedPreferences
    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val loggedInUser = sharedPref.getString("username", "Unknown User") ?: "Unknown User"
    val savedRegion = sharedPref.getString("user_division", "UNKNOWN") ?: "UNKNOWN" // 🔥 অ্যাপ থেকে ডিভিশন নেওয়া হচ্ছে

    withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) throw Exception("Failed to decode image")

            val tempFile = File(context.cacheDir, "scan_image_clear.png")
            val outputStream = java.io.FileOutputStream(tempFile)

            val maxWidth = 1500f
            val scale = if (originalBitmap.width > maxWidth) maxWidth / originalBitmap.width else 1f
            val finalBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            withContext(Dispatchers.Main) {
                logs.add("Uploading to Master Brain...")
            }

            // Image part
            val requestBody = tempFile.asRequestBody("image/png".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

            // Other parts (Username, Text, and Region)
            val usernameBody = loggedInUser.toRequestBody("text/plain".toMediaTypeOrNull())
            val localTextBody = localExtractedText.toRequestBody("text/plain".toMediaTypeOrNull())
            val regionBody = savedRegion.toRequestBody("text/plain".toMediaTypeOrNull()) // 🔥 Region Add করা হলো

            // 🔥 Call API with 4 parts: file, username, localText, region
            val response = RetrofitClient.apiService.scanImage(imagePart, usernameBody, localTextBody, regionBody)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    logs.add("Brain extracted Bangla Text & Scanned Link! ✔")
                    delay(400)

                    val aiResult = response.body()?.result?.uppercase() ?: "SAFE"
                    val finalExtractedText = response.body()?.extractedText ?: localExtractedText

                    when {
                        aiResult.contains("DANGER") -> {
                            logs.add("DANGER: Malicious intent detected in image! ❌")
                            saveImageScanToHistory(context, finalExtractedText.take(50), "DANGER", "OCR Image Scanner")
                            onResult(5, finalExtractedText, phone)
                        }
                        aiResult.contains("SPAM") -> {
                            logs.add("WARNING: Spam content detected. \u26A0\uFE0F")
                            saveImageScanToHistory(context, finalExtractedText.take(50), "SPAM", "OCR Image Scanner")
                            onResult(4, finalExtractedText, phone)
                        }
                        aiResult.contains("PROMO") -> {
                            logs.add("INFO: Promotional content found. \u2139\uFE0F")
                            saveImageScanToHistory(context, finalExtractedText.take(50), "PROMO", "OCR Image Scanner")
                            onResult(3, finalExtractedText, phone)
                        }
                        else -> {
                            logs.add("Scan finished. No threats found. ✔")
                            saveImageScanToHistory(context, finalExtractedText.take(50), "SAFE", "OCR Image Scanner")
                            onResult(2, finalExtractedText, phone)
                        }
                    }
                } else {
                    logs.add("ERROR: Server rejected the request (Code: ${response.code()}).")
                    onResult(0, localExtractedText, phone)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("ERROR: Request failed. Server might be sleeping. (${e.message})")
                onResult(0, localExtractedText, phone)
            }
        }
    }
}

suspend fun runLocalOcrTFLiteAnalysis(context: Context, payloadData: String, phone: String?, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int, String, String?) -> Unit) {
    logs.add("No internet. Using offline AI mode...")
    delay(400)

    val detector = SpamDetector.getInstance(context)

    withContext(Dispatchers.Main) {
        logs.add("Loading Prohory Neural Engine...")
        delay(600)
        logs.add("Running Neural Inference locally...")
    }

    val labelIndex = suspendCoroutine<Int> { continuation ->
        detector.classify(payloadData) { index, _ ->
            continuation.resume(index)
        }
    }

    withContext(Dispatchers.Main) {
        delay(600)
        when (labelIndex) {
            3 -> {
                logs.add("DANGER: Malicious pattern found! ❌")
                saveImageScanToHistory(context, payloadData.take(50), "DANGER", "Local Scanner")
                onResult(5, payloadData, phone)
            }
            2 -> {
                logs.add("WARNING: Spam content detected. \u26A0\uFE0F")
                saveImageScanToHistory(context, payloadData.take(50), "SPAM", "Local Scanner")
                onResult(4, payloadData, phone)
            }
            1 -> {
                logs.add("INFO: Promotional content found. \u2139\uFE0F")
                saveImageScanToHistory(context, payloadData.take(50), "PROMO", "Local Scanner")
                onResult(3, payloadData, phone)
            }
            else -> {
                logs.add("Scan finished. Safe. ✔")
                saveImageScanToHistory(context, payloadData.take(50), "SAFE", "Local Scanner")
                onResult(2, payloadData, phone)
            }
        }
    }
}

fun isInternetAvail(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

@Composable
fun ImageIntelligenceCanvas(uri: Uri?, scanState: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(1500), RepeatMode.Reverse))

    val borderColor = when (scanState) {
        0 -> NeonCyan.copy(alpha = pulseAlpha)
        1 -> NeonPurple
        2 -> Color.Green
        3 -> Color(0xFF3B82F6)
        4 -> Color(0xFFFF9800)
        5 -> Color.Red
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
                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, alpha = if (scanState == 1) 0.4f else 1f)
                if (scanState == 1) {
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

    val (color, title, subtitle) = when (state) {
        2 -> Triple(Color.Green, "SAFE", "No threats detected")
        3 -> Triple(Color(0xFF3B82F6), "PROMO", "Promotional message")
        4 -> Triple(Color(0xFFFF9800), "SPAM", "Suspicious spam content")
        5 -> Triple(Color.Red, "DANGER", "Malicious pattern found")
        else -> Triple(Color.DarkGray, "UNKNOWN", "")
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp).scale(if (state >= 4) pulse else 1f)) {
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

@Composable
fun ActionCenter(state: Int, phoneToBlock: String?) {
    val context = LocalContext.current

    if (state >= 4) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RECOMMENDED ACTIONS", color = if(state == 5) Color.Red else Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            val buttonColor = if (state == 5) Color.Red else Color(0xFFFF9800)
            val buttonText = if (phoneToBlock != null) "Block Sender ($phoneToBlock)" else "Report as Spam"

            Button(
                onClick = {
                    if (phoneToBlock != null) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneToBlock")
                            }
                            context.startActivity(intent)
                            Toast.makeText(context, "Please block the number from your dialer.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No number found. Please ignore the message.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor.copy(0.1f)),
                border = BorderStroke(1.dp, buttonColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Block, null, tint = buttonColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(buttonText, color = buttonColor, fontWeight = FontWeight.Bold)
            }
        }
    } else if (state in 2..3) {
        val (bgColor, iconColor, text) = if (state == 2) {
            Triple(Color.Green.copy(0.1f), Color.Green, "Content is safe to interact with.")
        } else {
            Triple(Color(0xFF3B82F6).copy(0.1f), Color(0xFF3B82F6), "Promotional content. Proceed with normal caution.")
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = bgColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, iconColor.copy(0.3f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(if(state == 2) Icons.Rounded.Verified else Icons.Rounded.LocalOffer, null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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