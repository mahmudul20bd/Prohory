package com.cyberaware.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                ForgotPasswordScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // States for Multi-step Process
    var currentStep by remember { mutableIntStateOf(1) } // 1: Email, 2: OTP, 3: New Password
    var isLoading by remember { mutableStateOf(false) }

    // Input States
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        // 🔥 Falling Matrix Bubbles
        CyberMatrixBg()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back Button
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentStep > 1) currentStep-- else onBack()
                }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Text("PASSWORD RECOVERY", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lottie Icon (Changes based on step)
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim)) // You can use a different lock/email animation here if you have one
                Box(modifier = Modifier.size(120.dp).background(NeonCyan.copy(alpha = 0.05f), RoundedCornerShape(30.dp)).border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                    LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(90.dp))
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 🔥 Smooth Multi-Step Transition
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    }, label = "step_transition"
                ) { step ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        when (step) {
                            1 -> {
                                Text("Find Your Account", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Enter your registered email address to receive a secure recovery code.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                                RecoveryTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
                                Spacer(modifier = Modifier.height(30.dp))

                                RecoveryButton(text = if (isLoading) "SENDING CODE..." else "SEND RESET CODE", isLoading = isLoading) {
                                    if (email.isNotEmpty()) {
                                        isLoading = true
                                        coroutineScope.launch {
                                            // TODO: Call your Retrofit send OTP API here
                                            delay(1500) // Dummy delay
                                            isLoading = false
                                            currentStep = 2
                                            Toast.makeText(context, "Recovery code sent to email", Toast.LENGTH_SHORT).show()
                                        }
                                    } else Toast.makeText(context, "Enter your email", Toast.LENGTH_SHORT).show()
                                }
                            }
                            2 -> {
                                Text("Enter Recovery Code", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("We've sent a 6-digit code to\\n$email", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                                RecoveryTextField(value = otpCode, onValueChange = { otpCode = it }, label = "6-Digit Code", icon = Icons.Default.VpnKey, isNumber = true)
                                Spacer(modifier = Modifier.height(30.dp))

                                RecoveryButton(text = if (isLoading) "VERIFYING..." else "VERIFY CODE", isLoading = isLoading) {
                                    if (otpCode.length >= 4) {
                                        isLoading = true
                                        coroutineScope.launch {
                                            // TODO: Call your Retrofit verify OTP API here
                                            delay(1500) // Dummy delay
                                            isLoading = false
                                            currentStep = 3
                                        }
                                    } else Toast.makeText(context, "Enter a valid code", Toast.LENGTH_SHORT).show()
                                }
                            }
                            3 -> {
                                Text("Secure New Password", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Create a strong and unique password for your Prohory account.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                                RecoveryTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New Password", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })
                                Spacer(modifier = Modifier.height(16.dp))
                                RecoveryTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })
                                Spacer(modifier = Modifier.height(30.dp))

                                RecoveryButton(text = if (isLoading) "UPDATING..." else "RESET PASSWORD", isLoading = isLoading) {
                                    if (newPassword == confirmPassword && newPassword.length >= 6) {
                                        isLoading = true
                                        coroutineScope.launch {
                                            // TODO: Call your Retrofit reset password API here
                                            delay(1500) // Dummy delay
                                            isLoading = false
                                            Toast.makeText(context, "Password Reset Successful!", Toast.LENGTH_LONG).show()
                                            onBack() // Go back to login
                                        }
                                    } else Toast.makeText(context, "Passwords do not match or too short", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// UI Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, isNumber: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color(0xFF1E293B), animationSpec = tween(300))
    val iconColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color.Gray, animationSpec = tween(300))

    OutlinedTextField(
        value = value, onValueChange = onValueChange, placeholder = { Text(label, color = Color.DarkGray, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp)) },
        trailingIcon = if (isPassword) { { IconButton(onClick = onVisibilityChange) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray) } } } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else if (isNumber) KeyboardType.Number else KeyboardType.Email),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan,
            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().height(60.dp).background(Color(0xFF0F172A), RoundedCornerShape(16.dp)).border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
    )
}

@Composable
fun RecoveryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent), contentPadding = PaddingValues(), shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(60.dp).background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), shape = RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
    ) {
        if (isLoading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp); Spacer(modifier = Modifier.width(12.dp)) }
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun CyberMatrixBg() {
    val particles = remember { List(40) { MatrixDot() } }
    LaunchedEffect(Unit) { while (true) { particles.forEach { it.update() }; delay(16) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { drawCircle(color = NeonCyan.copy(alpha = it.alpha.value), radius = it.radius.value, center = Offset(it.x.value, it.y.value)) }
    }
}

class MatrixDot {
    val x = mutableStateOf(Random.nextFloat() * 1500f)
    val y = mutableStateOf(Random.nextFloat() * 3000f)
    val radius = mutableStateOf(Random.nextFloat() * 5f + 2f)
    val alpha = mutableStateOf(Random.nextFloat() * 0.3f + 0.1f)
    private val speed = Random.nextFloat() * 5f + 2f
    fun update() { y.value += speed; if (y.value > 3000f) { y.value = 0f; x.value = Random.nextFloat() * 1500f } }
}