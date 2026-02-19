package com.cyberaware.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animated Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A14)) // Deep Black Background
    ) {
        CyberMatrixBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lottie Animation (Login Logo)
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_anim))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .size(200.dp)
                    .scale(1.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Header Text
            Text(
                text = "SYSTEM ACCESS",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Authenticate Identity",
                color = NeonCyan,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // High Visibility Input: Email
            CyberTextField(
                value = email,
                onValueChange = { email = it },
                label = "Agent ID / Email",
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            // High Visibility Input: Password
            CyberTextField(
                value = password,
                onValueChange = { password = it },
                label = "Access Key",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible }
            )

            // Forgot Password
            Text(
                text = "Lost Key?",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp, bottom = 32.dp)
                    .clickable { /* TODO: Forgot Password Logic */ }
            )

            // Gradient Neon Button
            NeonButton(
                text = "INITIATE LOGIN",
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, DashboardActivity::class.java))
                    } else {
                        Toast.makeText(context, "Credentials Required", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Register Link
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text(text = "New Agent? ", color = Color.Gray)
                Text(
                    text = "Join Force",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

// --- UPDATED COMPONENTS (HIGH VISIBILITY) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityChange: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Enter $label", color = Color.Gray) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onVisibilityChange) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
            singleLine = true,

            // 🔥 NEW HIGH VISIBILITY COLORS
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = NeonBlue,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B), // Slate Dark
                unfocusedContainerColor = Color(0xFF111827) // Darker
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NeonButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// --- ANIMATED BACKGROUND LOGIC ---
@Composable
fun CyberMatrixBackground() {
    val particles = remember { List(50) { MatrixParticle() } }
    LaunchedEffect(Unit) {
        while (true) {
            particles.forEach { it.update() }
            delay(16)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = NeonBlue.copy(alpha = particle.alpha.value),
                radius = particle.radius.value,
                center = Offset(particle.x.value, particle.y.value)
            )
        }
    }
}

class MatrixParticle {
    val x = mutableStateOf(Random.nextFloat() * 1080)
    val y = mutableStateOf(Random.nextFloat() * 2400)
    val radius = mutableStateOf(Random.nextFloat() * 5 + 2)
    val alpha = mutableStateOf(Random.nextFloat() * 0.5f + 0.1f)
    private val speed = Random.nextFloat() * 5 + 2

    fun update() {
        y.value += speed
        if (y.value > 2400) {
            y.value = 0f
            x.value = Random.nextFloat() * 1080
        }
    }
}