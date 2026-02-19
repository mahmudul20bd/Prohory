package com.cyberaware.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.cyberaware.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreenshotAnalysisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                ScreenshotAnalysisScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotAnalysisScreen(onBack: () -> Unit) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiResultLabel by remember { mutableIntStateOf(-1) } // 0=Safe, 1=Promo, 2=Spam, 3=Danger
    var statusText by remember { mutableStateOf("Waiting for Input...") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // গ্যালারি লঞ্চার
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            aiResultLabel = -1
            statusText = "Ready to Scan"
        }
    }

    // পারমিশন লঞ্চার
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) galleryLauncher.launch("image/*")
        else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
    }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("NEURAL OCR SCANNER",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        fontSize = 14.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF020408) // Ultra Deep Blue-Black
    ) { padding ->
        // 🔥 BACKGROUND CYBER EFFECTS
        Box(modifier = Modifier.fillMaxSize().padding(padding).drawBehind {
            drawCyberGrid()
            drawAmbientGlows()
        }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 櫨 THE MAIN SCANNER FRAME
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF080C15).copy(alpha = 0.8f))
                        .border(
                            BorderStroke(2.dp, Brush.linearGradient(listOf(NeonBlue, Color(0xFF8A2BE2), NeonBlue))),
                            RoundedCornerShape(32.dp)
                        )
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
                                galleryLauncher.launch("image/*")
                            } else {
                                permissionLauncher.launch(permissionToRequest)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri == null) {
                        PulseUploadState()
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Fit
                            )

                            // 🔥 ACTIVE LASER SCANNING ANIMATION
                            if (isAnalyzing) {
                                AdvancedUltraLaser()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 櫨 DYNAMIC STATUS CHIP
                StatusChip(statusText, isAnalyzing)

                Spacer(modifier = Modifier.height(20.dp))

                // 櫨 AI RESULT DISPLAY
                AnimatedVisibility(
                    visible = aiResultLabel != -1,
                    enter = slideInVertically() + fadeIn(),
                    exit = fadeOut()
                ) {
                    AdvancedResultCard(aiResultLabel)
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 櫨 HYPER-TECH ACTION BUTTON
                Button(
                    onClick = {
                        if (selectedImageUri != null) {
                            isAnalyzing = true
                            statusText = "Extracting Neural Text..."

                            val image = InputImage.fromFilePath(context, selectedImageUri!!)
                            recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    statusText = "AI Deep Analysis..."
                                    val detector = SpamDetector.getInstance(context)
                                    detector.classify(visionText.text) { label, _ -> //
                                        aiResultLabel = label
                                        isAnalyzing = false
                                        statusText = "Analysis Complete"
                                    }
                                }
                                .addOnFailureListener {
                                    isAnalyzing = false
                                    statusText = "OCR Extraction Failed"
                                }
                        } else {
                            permissionLauncher.launch(permissionToRequest)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .graphicsLayer { shadowElevation = 20f },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedImageUri == null) NeonBlue else Color(0xFF8A2BE2)
                    ),
                    enabled = !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(if(selectedImageUri == null) Icons.Default.AddPhotoAlternate else Icons.Default.Memory, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if(selectedImageUri == null) "SELECT SCREENSHOT" else "LAUNCH NEURAL SCAN",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// --- অ্যাডভান্সড ইউআই কম্পোনেন্টস ---

@Composable
fun StatusChip(text: String, active: Boolean) {
    Surface(
        color = if(active) NeonBlue.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        shape = CircleShape,
        border = BorderStroke(1.dp, if(active) NeonBlue else Color.Gray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if(active) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "")
                Box(modifier = Modifier.size(8.dp).background(NeonBlue, CircleShape).graphicsLayer { this.alpha = alpha })
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text.uppercase(), color = if(active) NeonBlue else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdvancedResultCard(label: Int) {
    // 0=Safe, 1=Promo, 2=Spam, 3=Danger
    val isDanger = label == 2 || label == 3
    val themeColor = if (isDanger) Color(0xFFFF3333) else Color(0xFF00FF9D)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(themeColor, Color.Transparent)))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if(isDanger) Icons.Default.GppBad else Icons.Default.VerifiedUser, null, tint = themeColor, modifier = Modifier.size(45.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(if(isDanger) "THREAT IDENTIFIED" else "SECURE CONTENT", color = themeColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    if(isDanger) "Our AI found malicious phishing patterns in this chat."
                    else "This screenshot appears safe for interaction.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun PulseUploadState() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Icon(Icons.Default.CloudUpload, null, tint = NeonBlue, modifier = Modifier.size(70.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("UPLOAD CHAT DATA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text("Tap to pick from gallery", color = NeonBlue.copy(0.5f), fontSize = 12.sp)
    }
}

@Composable
fun AdvancedUltraLaser() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val yPos by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse), label = "")

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val currentY = size.height * yPos

        // ১. লেজার বিম (Multi-color Neon)
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, NeonBlue, Color.White, NeonBlue, Color.Transparent)),
            start = Offset(0f, currentY),
            end = Offset(size.width, currentY),
            strokeWidth = 12f
        )

        // ২. ট্রেইলিং গ্লো (Trailing Glow)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(NeonBlue.copy(alpha = 0.2f), Color.Transparent),
                startY = currentY - 180f,
                endY = currentY
            ),
            topLeft = Offset(0f, currentY - 180f),
            size = Size(size.width, 180f)
        )
    }
}

// --- গ্রাফিক্স ড্রয়িং ফাংশনস ---

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberGrid() {
    val step = 45.dp.toPx()
    val lineColor = Color(0xFF00BFFF).copy(alpha = 0.04f)
    for (i in 0..size.width.toInt() step step.toInt()) drawLine(lineColor, Offset(i.toFloat(), 0f), Offset(i.toFloat(), size.height))
    for (i in 0..size.height.toInt() step step.toInt()) drawLine(lineColor, Offset(0f, i.toFloat()), Offset(size.width, i.toFloat()))
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientGlows() {
    drawCircle(Brush.radialGradient(listOf(NeonBlue.copy(0.05f), Color.Transparent)), radius = 400f, center = Offset(0f, 0f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF8A2BE2).copy(0.05f), Color.Transparent)), radius = 400f, center = Offset(size.width, size.height))
}