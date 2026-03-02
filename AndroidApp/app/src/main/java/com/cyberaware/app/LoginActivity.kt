package com.cyberaware.app

import android.content.Context
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 🔥 Email er jaygay loginId use korchi jate PRH ID o nite pare
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {

        CyberMatrixBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(tween(800))
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
                Box(modifier = Modifier.size(160.dp).background(NeonCyan.copy(alpha = 0.05f), RoundedCornerShape(40.dp)).border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(40.dp)), contentAlignment = Alignment.Center) {
                    LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(130.dp))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800, delayMillis = 300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome Back!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Log in to secure your messages", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))

                    // 🔥 Placeholder clear korar jonno update kora holo ebong PRH ID add kora holo
                    PremiumTextField(value = loginId, onValueChange = { loginId = it }, label = "Email, Username or Agent ID", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(20.dp))
                    PremiumTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })

                    Text("Forgot Password?", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.End).padding(top = 16.dp, bottom = 30.dp).clickable {
                        // Navigating to Forgot Password Page
                        context.startActivity(Intent(context, ForgotPasswordActivity::class.java))
                    })

                    PremiumButton(
                        text = if (isLoading) "LOGGING IN..." else "LOG IN",
                        isLoading = isLoading,
                        onClick = {
                            if (loginId.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val credentials = mapOf("username" to loginId, "password" to password)
                                        val response = RetrofitClient.apiService.loginUser(credentials)

                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val authResponse = response.body()!!

                                            // 🔥 FIX: Role check kora hocche. Shudhu "User" role login korte parbe
                                            if (authResponse.role.equals("User", ignoreCase = true)) {
                                                val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
                                                with(sharedPref.edit()) {
                                                    putBoolean("isLoggedIn", true)
                                                    putString("auth_token", authResponse.token)
                                                    putString("username", authResponse.username)
                                                    putString("agentId", authResponse.agentId ?: "UNKNOWN")
                                                    putString("role", authResponse.role) // 🔥 Role save kora hocche
                                                    apply()
                                                }
                                                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                                                context.startActivity(Intent(context, DashboardActivity::class.java))
                                                (context as? ComponentActivity)?.finish()
                                            } else {
                                                // Jodi Expert ba Admin login korte chay
                                                Toast.makeText(context, "Access Denied: App is only for Users.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Incorrect Credentials", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter both fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Don't have an account? ", color = Color.Gray, fontSize = 14.sp)
                        Text(text = "Create Account", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            context.startActivity(Intent(context, RegisterActivity::class.java))
                        })
                    }
                }
            }
        }
    }
}

// ===============================================
// 🔥 PREMIUM ADVANCED UI COMPONENTS 🔥
// ===============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color(0xFF1E293B), animationSpec = tween(300))
    val iconColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color.Gray, animationSpec = tween(300))

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // 🔥 Ekhane placeholder er color Color.DarkGray theke Color(0xFFA0AABF) (Light Slate) kora hoyeche jate sposto bujha jay
            placeholder = { Text(label, color = Color(0xFFA0AABF), fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp)) },
            trailingIcon = if (isPassword) { { IconButton(onClick = onVisibilityChange) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } } } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = NeonCyan,
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun PremiumButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.02f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(if (isLoading) 1f else pulseScale)
            .background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// 🔥 FALLING MATRIX BUBBLES 🔥
@Composable
fun CyberMatrixBackground() {
    val particles = remember { List(60) { MatrixParticle() } }
    LaunchedEffect(Unit) { while (true) { particles.forEach { it.update() }; delay(16) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach {
            drawCircle(
                color = NeonCyan.copy(alpha = it.alpha.value),
                radius = it.radius.value,
                center = Offset(it.x.value, it.y.value)
            )
        }
    }
}

class MatrixParticle {
    val x = mutableStateOf(Random.nextFloat() * 1500f)
    val y = mutableStateOf(Random.nextFloat() * 3000f)
    val radius = mutableStateOf(Random.nextFloat() * 6f + 2f)
    val alpha = mutableStateOf(Random.nextFloat() * 0.4f + 0.1f)
    private val speed = Random.nextFloat() * 6f + 3f
    fun update() {
        y.value += speed
        if (y.value > 3000f) {
            y.value = 0f
            x.value = Random.nextFloat() * 1500f
        }
    }
}