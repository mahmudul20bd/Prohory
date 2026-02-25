package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.*
import com.cyberaware.app.network.RetrofitClient
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                RegisterScreen()
            }
        }
    }
}

@Composable
fun RegisterScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current // Clipboard service for copy
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var newAgentId by remember { mutableStateOf("") }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {

        CyberMatrixBackgroundReg()

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(tween(800))
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
                Box(modifier = Modifier.size(140.dp).background(NeonPurple.copy(alpha = 0.05f), RoundedCornerShape(35.dp)).border(1.dp, NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(35.dp)), contentAlignment = Alignment.Center) {
                    LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(110.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800, delayMillis = 300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create Account", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Join to secure your digital life", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 30.dp))

                    PremiumTextFieldReg(value = username, onValueChange = { username = it }, label = "Full Name", icon = Icons.Default.Person)
                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextFieldReg(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextFieldReg(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })
                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextFieldReg(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })

                    Spacer(modifier = Modifier.height(30.dp))

                    PremiumButtonReg(
                        text = if (isLoading) "CREATING ACCOUNT..." else "SIGN UP",
                        isLoading = isLoading,
                        onClick = {
                            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                                if (password == confirmPassword) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val registerData = mapOf("username" to username, "email" to email, "password" to password)
                                            val response = RetrofitClient.apiService.registerUser(registerData)

                                            if (response.isSuccessful && response.body()?.success == true) {
                                                newAgentId = response.body()?.agentId ?: "PRH-0000"
                                                showSuccessDialog = true
                                            } else {
                                                Toast.makeText(context, "Registration Failed. Try another email.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Network Error. Please try again.", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                        Text(text = "Log In", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as? ComponentActivity)?.finish()
                        })
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }

        // ===============================================
        // 🔥 PREMIUM SUCCESS DIALOG WITH CONFETTI 🔥
        // ===============================================
        if (showSuccessDialog) {
            Dialog(onDismissRequest = { /* Prevent dismiss on outside click */ }) {
                Box(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Dialog Content Box
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0B1221),
                        border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("REGISTRATION SUCCESSFUL!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Welcome to Prohory. Please copy your unique Agent ID and save it securely.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)

                            Spacer(modifier = Modifier.height(24.dp))

                            // 🔥 Copyable ID Container
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF02040A), RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(newAgentId))
                                        Toast.makeText(context, "Agent ID Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = newAgentId, color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)

                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = NeonCyan, modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            PremiumButtonReg(text = "PROCEED TO LOGIN", isLoading = false) {
                                showSuccessDialog = false
                                context.startActivity(Intent(context, LoginActivity::class.java))
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                    }

                    // 🔥 Confetti Overlay Animation (Plays Once)
                    val confettiComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
                    LottieAnimation(
                        composition = confettiComposition,
                        iterations = 1, // Only play once like a birthday popper
                        modifier = Modifier.fillMaxSize().scale(1.5f) // Scale to cover edges
                    )
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
fun PremiumTextFieldReg(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color(0xFF1E293B), animationSpec = tween(300))
    val iconColor by animateColorAsState(targetValue = if (isFocused) NeonCyan else Color.Gray, animationSpec = tween(300))

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
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
fun PremiumButtonReg(text: String, isLoading: Boolean, onClick: () -> Unit) {
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
fun CyberMatrixBackgroundReg() {
    val particles = remember { List(60) { MatrixParticleReg() } }
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

class MatrixParticleReg {
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