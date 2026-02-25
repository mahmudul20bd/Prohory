package com.cyberaware.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.fragment.app.FragmentActivity

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

    // 🔥 FIX: User friendly loading text
    var bootingText by remember { mutableStateOf("Starting Prohory...") }
    var showSubtitle by remember { mutableStateOf(false) }

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(durationMillis = 1500), label = "subtitle_alpha"
    )

    fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(context, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // ইউজার ক্যানসেল করলে বা এরর দিলে লগইন পেজে পাঠিয়ে দেবে
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    context.finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Prohory") // 🔥 FIX: Simple and friendly title
            .setSubtitle("Please verify your identity to enter securely")
            .setNegativeButtonText("Use Password") // 🔥 FIX: Better UX than just "Cancel"
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)

        val isFirstTime = sharedPref.getBoolean("isFirstTime", true)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val isBiometricEnabled = sharedPref.getBoolean("biometric_login", false)

        showSubtitle = true

        // 🔥 FIX: Replaced hacker terms with user-friendly security terms
        delay(800); bootingText = "Checking system security..."
        delay(800); bootingText = "Updating threat database..."
        delay(1000); bootingText = "Activating SMS shield..."
        delay(600); bootingText = "Ready to protect."
        delay(500)

        when {
            isFirstTime -> {
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

            Text(
                text = "THE CYBER EYE",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(subtitleAlpha)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = bootingText,
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}