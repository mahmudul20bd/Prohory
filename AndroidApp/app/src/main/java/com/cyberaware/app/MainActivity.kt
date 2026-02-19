package com.cyberaware.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import com.cyberaware.app.ui.theme.SuccessGreen
import com.cyberaware.app.ui.theme.ErrorRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                MainScreen(onBack = { finish() })
            }
        }
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // States
    var sender by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var isThreat by remember { mutableStateOf(false) }

    // Pulsing Dot Animation (Status)
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A14))
    ) {
        // Background
        CyberMatrixBackgroundReg()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LIVE MONITOR", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Green.copy(alpha = alpha))
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- SCANNER ANIMATION AREA ---
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B1221).copy(alpha = 0.8f))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        "ANALYZING PATTERNS...",
                        color = NeonCyan,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                        fontSize = 12.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Ready",
                        tint = Color.Gray,
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        "READY TO SCAN",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- INPUT FIELDS ---
            CyberTextFieldReg(
                value = sender,
                onValueChange = { sender = it },
                label = "Sender Number / ID",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Paste Message Here", color = Color.Gray) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    containerColor = Color(0x1AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- ACTION BUTTON ---
            Button(
                onClick = {
                    if (message.isEmpty()) {
                        Toast.makeText(context, "Input Error: No data to analyze", Toast.LENGTH_SHORT).show()
                    } else {
                        isScanning = true
                        showResult = false

                        scope.launch {
                            delay(2000) // Scanning Effect

                            // 🔥 FIXED: REAL AI INTEGRATION
                            val detector = SpamDetector.getInstance(context)

                            detector.classify(message) { labelIndex, _ ->
                                // 2=Spam, 3=Danger
                                isThreat = (labelIndex == 2 || labelIndex == 3)

                                isScanning = false
                                showResult = true

                                // Send Report (If Backend Exists)
                                // sendReportToServer(sender, message)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(if (isScanning) "SCANNING..." else "DETECT THREATS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // --- RESULT OVERLAY POPUP ---
        AnimatedVisibility(
            visible = showResult,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showResult = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B28)),
                    border = CardDefaults.outlinedCardBorder(true),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isThreat) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isThreat) ErrorRed else SuccessGreen,
                            modifier = Modifier.size(80.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (isThreat) "THREAT DETECTED" else "SYSTEM SECURE",
                            color = if (isThreat) ErrorRed else SuccessGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isThreat) "Content flagged as suspicious by Neural Engine. Do not click any links."
                            else "No malicious patterns found in analysis. Message appears safe.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showResult = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ACKNOWLEDGE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Backend Call Placeholder (Unchanged)
fun sendReportToServer(sender: String, msg: String) {
    try {
        val request = ReportRequest(
            type = if (msg.contains("win")) "Spam" else "Clean",
            description = msg,
            userId = sender.ifEmpty { "Unknown" }
        )

        RetrofitClient.instance.sendReport(request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {}
            override fun onFailure(call: Call<String>, t: Throwable) {}
        })
    } catch (e: Exception) {
        // Handle missing classes gracefully
    }
}