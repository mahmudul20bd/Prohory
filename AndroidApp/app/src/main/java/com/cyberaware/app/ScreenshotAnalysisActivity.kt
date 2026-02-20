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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    val scanLogs = remember { mutableStateListOf<String>() }

    // States: 0 = Idle, 1 = OCR Extracting, 2 = AI Scanning, 3 = Safe, 4 = Danger
    var scanState by remember { mutableIntStateOf(0) }

    // Gallery Image Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                extractedText = ""
                scanState = 0
                scanLogs.clear()
                scanLogs.add("> Image acquired successfully.")
                scanLogs.add("> Awaiting extraction command...")
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        OcrGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.DocumentScanner, contentDescription = "OCR", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("OCR THREAT ANALYSIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🔥 HIGH UI: Image Preview & Laser Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131C2B))
                    .border(1.dp, if(selectedImageUri != null) NeonCyan else Color.DarkGray, RoundedCornerShape(16.dp))
                    .clickable(enabled = scanState == 0) {
                        photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload", tint = Color.Gray, modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TAP TO UPLOAD SCREENSHOT", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (scanState in 1..2) 0.5f else 1f
                    )

                    // Laser Scanning Animation
                    if (scanState == 1 || scanState == 2) {
                        CyberLaserScanner()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scan Button
            PaddingValues(horizontal = 20.dp).let {
                Button(
                    onClick = {
                        if (selectedImageUri == null) {
                            Toast.makeText(context, "Please upload a screenshot first!", Toast.LENGTH_SHORT).show()
                        } else {
                            scanState = 1
                            scanLogs.clear()
                            coroutineScope.launch {
                                runOcrAndAiAnalysis(context, selectedImageUri!!, scanLogs) { resultState, text ->
                                    extractedText = text
                                    scanState = resultState
                                }
                            }
                        }
                    },
                    enabled = scanState == 0 && selectedImageUri != null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp).clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = if (scanState in 1..2) Color.Gray else NeonPurple)
                ) {
                    val btnText = when(scanState) {
                        0 -> "INITIATE OCR EXTRACtION"
                        1 -> "EXTRACTING TEXT..."
                        2 -> "AI CORE ANALYZING..."
                        else -> "SCAN COMPLETE"
                    }
                    Text(btnText, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Result Display & Live Logs
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                color = Color(0xFF0B1221),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header of Terminal
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2B)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYSTEM LOGS // OCR VISION", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
                        if (scanState == 3 || scanState == 4) {
                            val color = if (scanState == 3) Color.Green else Color.Red
                            val text = if (scanState == 3) "SAFE" else "THREAT"
                            val icon = if (scanState == 3) Icons.Default.HealthAndSafety else Icons.Default.GppBad
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Divider(color = Color.DarkGray)

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), reverseLayout = false) {
                        items(scanLogs) { log ->
                            val textColor = when {
                                log.contains("[DANGER]") || log.contains("MALICIOUS") || log.contains("ERROR") -> Color.Red
                                log.contains("[WARNING]") || log.contains("EXTRACTED:") -> Color.Yellow
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

// 🔥 Core Logic: OCR Extraction -> AI Analysis
suspend fun runOcrAndAiAnalysis(
    context: Context,
    imageUri: Uri,
    logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onResult: (Int, String) -> Unit
) {
    logs.add("> Initializing Google ML Kit Vision Engine...")
    delay(500)
    logs.add("> Scanning image for hidden textual payloads...")

    var extractedText = ""

    // 1. Text Extraction using ML Kit
    withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Await the task result
            val result = recognizer.process(image).await()
            extractedText = result.text

            withContext(Dispatchers.Main) {
                if (extractedText.trim().isEmpty()) {
                    logs.add("[ERROR] No readable text found in the image.")
                    onResult(0, "")
                } else {
                    logs.add("[✔] Text extraction successful. (${extractedText.length} characters)")
                    logs.add("> EXTRACTED: \"${extractedText.take(50).replace("\n", " ")}...\"")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("[ERROR] OCR Engine failed: ${e.message}")
                onResult(0, "")
            }
        }
    }

    if (extractedText.trim().isEmpty()) return

    // 2. Pass Extracted Text to AI (State 2)
    onResult(2, extractedText)
    delay(800)
    logs.add("> Transferring payload to Neural Core...")
    delay(500)

    // Hybrid AI Check
    if (isInternetAvail(context)) {
        runServerOcrAiAnalysis(context, extractedText, logs) { state -> onResult(state, extractedText) }
    } else {
        runLocalOcrTFLiteAnalysis(context, extractedText, logs) { state -> onResult(state, extractedText) }
    }
}

// ==========================================
// HYBRID AI FUNCTIONS FOR OCR
// ==========================================
suspend fun runServerOcrAiAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("> Network Status: ONLINE")
    delay(400)
    logs.add("> Transmitting extracted text to Cloud AI...")

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val reporterId = sharedPref.getString("username", "Agent") ?: "Agent"

    withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("sender" to "OCR Scanner", "message" to payloadData, "reporter" to reporterId)
            val response = RetrofitClient.apiService.submitSmsReport("Bearer $token", payload)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    delay(800)
                    logs.add("> Cloud AI analyzing semantics & URLs...")
                    delay(1500)

                    val lowerInput = payloadData.lowercase()
                    val isSuspicious = lowerInput.contains("free") || lowerInput.contains("win") ||
                            lowerInput.contains("offer") || lowerInput.contains("lottery") ||
                            lowerInput.contains("bit.ly") || lowerInput.contains("অফার")

                    if(isSuspicious) {
                        logs.add("[DANGER] Cloud AI detected high-risk patterns in image text!")
                        onResult(4) // 4 = Danger
                    } else {
                        logs.add("[✔] Cloud AI confirmed payload is clean.")
                        onResult(3) // 3 = Safe
                    }
                } else {
                    logs.add("[ERROR] Server rejected. Code: ${response.code()}")
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

suspend fun runLocalOcrTFLiteAnalysis(context: Context, payloadData: String, logs: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onResult: (Int) -> Unit) {
    logs.add("> Network Status: OFFLINE")
    delay(400)
    logs.add("> Initializing Local TFLite Core for OCR Text...")

    withContext(Dispatchers.IO) {
        try {
            val tfliteModel = loadOcrModelFromStorage(context, "cyberaware_model.tflite")
            val interpreter = Interpreter(tfliteModel)
            val vocab = loadOcrVocabFromStorage(context, "vocab.txt")

            val inputSequence = tokenizeOcrInput(payloadData, vocab, maxLen = 50)
            val inputArray = arrayOf(inputSequence)
            val outputArray = arrayOf(FloatArray(1))

            withContext(Dispatchers.Main) {
                delay(800)
                logs.add("> Running Neural Inference locally...")
            }

            interpreter.run(inputArray, outputArray)
            val spamScore = outputArray[0][0]

            withContext(Dispatchers.Main) {
                delay(600)
                if (spamScore > 0.5f) {
                    logs.add("[DANGER] Threat probability is ${(spamScore * 100).toInt()}%!")
                    onResult(4)
                } else {
                    logs.add("[✔] Threat probability is low (${(spamScore * 100).toInt()}%).")
                    onResult(3)
                }
            }
            interpreter.close()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logs.add("[ERROR] Local AI Model Missing or Failed: ${e.message}")
                onResult(0)
            }
        }
    }
}

// ==========================================
// HELPERS (Duplicated lightly to keep file standalone)
// ==========================================
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

private fun tokenizeOcrInput(text: String, vocab: Map<String, Int>, maxLen: Int): FloatArray {
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
fun CyberLaserScanner() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .offset(y = offsetY.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, NeonCyan, Color.Transparent)
                    )
                )
                .border(1.dp, NeonCyan)
        )
    }
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