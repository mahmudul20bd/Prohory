package com.cyberaware.app

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var newAgentId by remember { mutableStateOf("") }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {

        // 🔥 YOUR FAVORITE FALLING BUBBLE ANIMATION 🔥
        CyberMatrixBackgroundReg()

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(800)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Icon(Icons.Default.Security, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Text("JOIN THE FORCE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Create Secure Identity", color = NeonCyan, fontSize = 14.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp, bottom = 40.dp))

                CyberTextFieldReg(value = name, onValueChange = { name = it }, label = "CODENAME (USERNAME)", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(16.dp))
                CyberTextFieldReg(value = email, onValueChange = { email = it }, label = "SECURE EMAIL", icon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))
                CyberTextFieldReg(value = password, onValueChange = { password = it }, label = "CREATE KEY", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })
                Spacer(modifier = Modifier.height(16.dp))
                CyberTextFieldReg(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "VERIFY KEY", icon = Icons.Default.Lock, isPassword = true, passwordVisible = confirmPasswordVisible, onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible })
                Spacer(modifier = Modifier.height(40.dp))

                NeonButtonReg(
                    text = if (isLoading) "CREATING AGENT..." else "INITIATE PROTOCOL",
                    isLoading = isLoading,
                    onClick = {
                        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "All fields required", Toast.LENGTH_SHORT).show()
                        } else if (password != confirmPassword) {
                            Toast.makeText(context, "Keys do not match", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val userPayload = mapOf("username" to name, "email" to email, "password" to password)
                                    val response = RetrofitClient.apiService.registerUser(userPayload)

                                    if (response.isSuccessful && response.body()?.success == true) {
                                        newAgentId = response.body()?.agentId ?: "UNKNOWN"
                                        showSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Registration Failed", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Server Error", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Already verified? ", color = Color.Gray, fontSize = 13.sp)
                    Text(text = "Access System", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        (context as ComponentActivity).finish()
                    })
                }
            }
        }

        // Holographic ID Card Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = Color(0xFF0B1221),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Green)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ACCESS GRANTED", color = Color.Green, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Welcome to Prohory. Your unique identification matrix has been generated.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            color = Color(0xFF131C2B),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("AGENT ID:", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(text = newAgentId, color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Agent ID", newAgentId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "ID Copied to secure clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as ComponentActivity).finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("PROCEED TO LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTextFieldReg(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text("Enter $label", color = Color.DarkGray, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) { { IconButton(onClick = onVisibilityChange) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } } } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
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
fun NeonButtonReg(text: String, isLoading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent), contentPadding = PaddingValues(), shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(55.dp).background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), shape = RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
    ) {
        if (isLoading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp); Spacer(modifier = Modifier.width(12.dp)) }
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
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