package com.cyberaware.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhishingScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                PhishingScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingScreen(onBack: () -> Unit) {
    var urlLink by remember { mutableStateOf("") }
    var scanStatus by remember { mutableStateOf("IDLE") } // IDLE, SCANNING, RESULT
    var resultType by remember { mutableStateOf("SAFE") } // SAFE, SCAM, SUSPICIOUS

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DEEP LINK SCANNER", color = Color.White, fontSize = 16.sp, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050A14))
            )
        },
        containerColor = Color(0xFF050A14)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 櫨 CYBER RADAR ANIMATION SECTION
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                if (scanStatus == "SCANNING") {
                    RadarAnimation()
                } else {
                    // Result Icon Logic
                    val icon = when {
                        scanStatus == "RESULT" && resultType == "SCAM" -> Icons.Default.BugReport
                        scanStatus == "RESULT" && resultType == "SUSPICIOUS" -> Icons.Default.Warning
                        else -> Icons.Default.Security
                    }
                    val tint = when {
                        scanStatus == "RESULT" && resultType == "SCAM" -> ErrorRed
                        scanStatus == "RESULT" && resultType == "SUSPICIOUS" -> Color(0xFFFF9800) // Orange
                        else -> NeonCyan
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 櫨 INPUT FIELD (HIGH CONTRAST)
            OutlinedTextField(
                value = urlLink,
                onValueChange = { urlLink = it },
                label = { Text("Enter Suspicious URL") },
                placeholder = { Text("https://example.com", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = NeonBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = Color(0xFF161B28),
                    unfocusedContainerColor = Color(0xFF0B1221)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 櫨 SCAN BUTTON WITH BACKGROUND THREADING
            Button(
                onClick = {
                    if(urlLink.isNotEmpty()) {
                        scanStatus = "SCANNING"

                        scope.launch {
                            // Simulate Animation Delay
                            delay(2000)

                            try {
                                val detector = SpamDetector.getInstance(context)

                                // 🔥 FIXED: Using Callback instead of Return
                                detector.classify(urlLink) { labelIndex, _ ->

                                    // Map Int Label to String logic
                                    // 0=Safe, 1=Promo, 2=Spam, 3=Danger
                                    val detectedType = when (labelIndex) {
                                        2, 3 -> "SCAM"
                                        1 -> "SUSPICIOUS"
                                        else -> "SAFE"
                                    }

                                    // Update UI
                                    resultType = detectedType
                                    scanStatus = "RESULT"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                resultType = "SAFE"
                                scanStatus = "RESULT"
                            }
                        }
                    } else {
                        Toast.makeText(context, "Input cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                if (scanStatus == "SCANNING") {
                    Text("ANALYZING...", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                } else {
                    Text("INITIATE SCAN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 櫨 RESULT DISPLAY
            if (scanStatus == "RESULT") {
                ScanResultCard(resultType)
            }
        }
    }
}

// --- ANIMATION COMPONENT ---
@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rot"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Outer Rings
        drawCircle(color = NeonBlue.copy(alpha = 0.3f), style = Stroke(width = 2f))
        drawCircle(color = NeonBlue.copy(alpha = 0.1f), radius = size.minDimension / 3, style = Stroke(width = 2f))

        // Scanning Line
        rotate(rotation) {
            // Gradient Sweep Effect
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, NeonBlue)),
                topLeft = center,
                size = androidx.compose.ui.geometry.Size(size.width / 2, 4f)
            )
            // Solid Line
            drawLine(
                color = NeonBlue,
                start = center,
                end = Offset(center.x + size.width / 2, center.y),
                strokeWidth = 4f
            )
        }
    }
}

// --- RESULT CARD COMPONENT ---
@Composable
fun ScanResultCard(type: String) {
    val color = when(type) {
        "SCAM" -> ErrorRed
        "SUSPICIOUS" -> Color(0xFFFF9800) // Orange
        else -> SuccessGreen
    }

    val title = when(type) {
        "SCAM" -> "CRITICAL THREAT"
        "SUSPICIOUS" -> "SUSPICIOUS LINK"
        else -> "LINK IS SAFE"
    }

    val icon = when(type) {
        "SCAM" -> Icons.Default.BugReport
        "SUSPICIOUS" -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }

    val desc = when(type) {
        "SCAM" -> "This URL matches known phishing signatures. Do not click."
        "SUSPICIOUS" -> "This link looks unusual. Proceed with extreme caution."
        else -> "No malicious signatures found in our database."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}