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

    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {

        // 🔥 YOUR FAVORITE FALLING BUBBLE ANIMATION 🔥
        CyberMatrixBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(tween(800))
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
                LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(180.dp).scale(1.2f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800, delayMillis = 300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SYSTEM ACCESS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Authenticate Identity", color = NeonCyan, fontSize = 14.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

                    CyberTextField(value = loginId, onValueChange = { loginId = it }, label = "AGENT ID / EMAIL", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(16.dp))
                    CyberTextField(value = password, onValueChange = { password = it }, label = "ACCESS KEY", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })

                    Text("Lost Key?", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End).padding(top = 12.dp, bottom = 24.dp).clickable { })

                    NeonButton(
                        text = if (isLoading) "AUTHENTICATING..." else "INITIATE LOGIN",
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
                                            val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
                                            with(sharedPref.edit()) {
                                                putString("auth_token", authResponse.token)
                                                putString("username", authResponse.username)
                                                putString("agentId", authResponse.agentId ?: "UNKNOWN")
                                                apply()
                                            }
                                            Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                            context.startActivity(Intent(context, DashboardActivity::class.java))
                                            (context as? ComponentActivity)?.finish()
                                        } else {
                                            Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Server Error", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Credentials Required", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Unregistered? ", color = Color.Gray, fontSize = 13.sp)
                        Text(text = "Join Task Force", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            context.startActivity(Intent(context, RegisterActivity::class.java))
                        })
                    }
                }
            }
        }
    }
}

// Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text("Enter $label", color = Color.DarkGray, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) { { IconButton(onClick = onVisibilityChange) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } } } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = NeonCyan, focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF0B1221), unfocusedContainerColor = Color(0xFF050A14)
            ),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(55.dp)
        )
    }
}

@Composable
fun NeonButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent), contentPadding = PaddingValues(), shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(55.dp).background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), shape = RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
    ) {
        if (isLoading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp); Spacer(modifier = Modifier.width(12.dp)) }
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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