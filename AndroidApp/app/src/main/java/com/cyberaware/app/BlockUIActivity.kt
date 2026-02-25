package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple

// 🔥 FIX: Changed to FragmentActivity for BiometricPrompt support
class BlockUIActivity : FragmentActivity() {
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
        val blockedPackageName = intent.getStringExtra("BLOCKED_APP") ?: "Unknown App"

        setContent {
            CyberAwareTheme {
                BlockScreen(
                    appName = blockedPackageName,
                    activity = this,
                    onExit = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onUnlockSuccess = {
                        // 🔥 Smart Session Whitelist
                        AppMonitorService.temporarilyUnlockedPackage = blockedPackageName
                        Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show()
                        finish() // Go back to the app they were trying to open
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun BlockScreen(appName: String, activity: FragmentActivity, onExit: () -> Unit, onUnlockSuccess: () -> Unit) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    fun authenticateToUnlock() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vault Authentication")
            .setSubtitle("Prohory System Override")
            .setDescription("Use your fingerprint or PIN to access this application.")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B1221)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier.size(150.dp).scale(scale).background(Color.Red.copy(alpha = 0.1f), CircleShape).border(2.dp, Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text("ACCESS DENIED", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Prohory System has locked access to:\n$appName", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp), color = Color(0xFF131C2B),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("SECURITY PROTOCOL", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("This application is restricted by the Vault. Authentication required to bypass.", color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 🔥 Added Authentication Button
            Button(
                onClick = { authenticateToUnlock() },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AUTHENTICATE TO UNLOCK", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO SAFETY", color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}