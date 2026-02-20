package com.cyberaware.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                SplashScreen()
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current

    // Typewriter effect state
    var bootingText by remember { mutableStateOf("") }
    val fullText = "INITIALIZING SECURE CORE..."

    LaunchedEffect(key1 = true) {
        // Typing animation
        for (i in fullText.indices) {
            bootingText = fullText.substring(0, i + 1)
            delay(50)
        }

        delay(1000) // Hold after typing

        val appPref = context.getSharedPreferences("AppPref", Context.MODE_PRIVATE)
        val isFirstTime = appPref.getBoolean("FirstTime", true)

        val authPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val token = authPref.getString("auth_token", "")

        val nextActivity = when {
            isFirstTime -> Intent(context, OnboardingActivity::class.java)
            !token.isNullOrEmpty() -> Intent(context, DashboardActivity::class.java)
            else -> Intent(context, LoginActivity::class.java)
        }

        context.startActivity(nextActivity)
        (context as ComponentActivity).finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Lottie Animation with glowing shadow effect
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
            Box(contentAlignment = Alignment.Center) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "PROHORY",
                color = NeonBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terminal Typing Text
            Text(
                text = bootingText,
                color = NeonCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}