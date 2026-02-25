package com.cyberaware.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import androidx.fragment.app.FragmentActivity // BiometricPrompt needs FragmentActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : FragmentActivity() {
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
    val context = LocalContext.current as FragmentActivity

    var bootingText by remember { mutableStateOf("Initializing Core Systems...") }
    var showSubtitle by remember { mutableStateOf(false) }

    // 🔥 Restored: Beautiful Subtitle Fade-In Animation
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(durationMillis = 1500), label = "subtitle_alpha"
    )

    // 🔥 BIOMETRIC LOGIC
    fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(context, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    context.finish() // Close app if failed/cancelled
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure System Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)

        // 🔥 Restored: Original Navigation States
        val isFirstTime = sharedPref.getBoolean("isFirstTime", true)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val isBiometricEnabled = sharedPref.getBoolean("biometric_login", true) // Default true

        // Trigger the subtitle animation
        showSubtitle = true

        // Terminal Typing Animation
        delay(800); bootingText = "Loading Cyber Neural Engine..."
        delay(800); bootingText = "Connecting to Satellite Server..."
        delay(1000); bootingText = "Bypassing Security Protocols..."
        delay(600); bootingText = "Access Granted."
        delay(500)

        // 🔥 Restored: Smart Navigation (Slider -> Login -> Biometric Dashboard)
        when {
            isFirstTime -> {
                // Show Onboarding Slider for the first time
                context.startActivity(Intent(context, OnboardingActivity::class.java))
                context.finish()
            }
            isLoggedIn -> {
                if (isBiometricEnabled) {
                    showBiometricPrompt(onSuccess = {
                        context.startActivity(Intent(context, DashboardActivity::class.java))
                        context.finish()
                    })
                } else {
                    context.startActivity(Intent(context, DashboardActivity::class.java))
                    context.finish()
                }
            }
            else -> {
                // If not logged in, go to Login
                context.startActivity(Intent(context, LoginActivity::class.java))
                context.finish()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Lottie Animation
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
            Box(contentAlignment = Alignment.Center) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Main Title
            Text(
                text = "PROHORY",
                color = NeonBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )

            // 🔥 Restored: Animated Subtitle
            Text(
                text = "THE CYBER EYE",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(subtitleAlpha) // Fade-in effect
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Terminal Text
            Text(
                text = bootingText,
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}