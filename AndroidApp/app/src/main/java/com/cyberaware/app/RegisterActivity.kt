package com.cyberaware.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A14))
    ) {
        CyberMatrixBackgroundReg()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Text("JOIN THE FORCE", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Create Secure Identity", color = NeonCyan, fontSize = 16.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))

            CyberTextFieldReg(value = name, onValueChange = { name = it }, label = "Codename (Username)", icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))

            CyberTextFieldReg(value = email, onValueChange = { email = it }, label = "Secure Email", icon = Icons.Default.Email)
            Spacer(modifier = Modifier.height(16.dp))

            CyberTextFieldReg(value = password, onValueChange = { password = it }, label = "Create Key (Password)", icon = Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityChange = { passwordVisible = !passwordVisible })
            Spacer(modifier = Modifier.height(16.dp))

            CyberTextFieldReg(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Verify Key", icon = Icons.Default.Lock, isPassword = true, passwordVisible = confirmPasswordVisible, onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible })
            Spacer(modifier = Modifier.height(40.dp))

            NeonButtonReg(
                text = if (isLoading) "CREATING AGENT..." else "INITIATE PROTOCOL",
                isLoading = isLoading,
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "⚠️ All fields required", Toast.LENGTH_SHORT).show()
                    } else if (password != confirmPassword) {
                        Toast.makeText(context, "❌ Keys do not match", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, "❌ Registration Failed: Username exists", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "⚠️ Server Error", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(text = "Already verified? ", color = Color.Gray)
                Text(text = "Access System", color = NeonBlue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as ComponentActivity).finish()
                })
            }
        }

        // 🔥 BEAUTIFUL SUCCESS DIALOG WITH COPY BUTTON
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(text = "ACCESS GRANTED", color = NeonCyan, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("Welcome to Prohory. Your Secure Agent ID has been generated:", color = Color.White)
                        Spacer(modifier = Modifier.height(15.dp))

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = newAgentId,
                                    color = NeonBlue,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                // 🔥 Copy Button Logic
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Agent ID", newAgentId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "ID Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(15.dp))
                        Text("You can use this ID, your Username, or Email to login.", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSuccessDialog = false
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        (context as ComponentActivity).finish()
                    }) {
                        Text("PROCEED TO LOGIN", color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0B1221)
            )
        }
    }
}

// --- UPDATED COMPONENTS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTextFieldReg(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false, passwordVisible: Boolean = false, onVisibilityChange: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text("Enter $label", color = Color.Gray) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
            trailingIcon = if (isPassword) { { IconButton(onClick = onVisibilityChange) { Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray) } } } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonBlue, focusedBorderColor = NeonBlue, unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF111827)),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NeonButtonReg(text: String, isLoading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent), contentPadding = PaddingValues(), shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(55.dp).background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), shape = RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun CyberMatrixBackgroundReg() {
    val particles = remember { List(50) { MatrixParticleReg() } }
    LaunchedEffect(Unit) { while (true) { particles.forEach { it.update() }; delay(16) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { drawCircle(color = NeonBlue.copy(alpha = it.alpha.value), radius = it.radius.value, center = Offset(it.x.value, it.y.value)) }
    }
}

class MatrixParticleReg {
    val x = mutableStateOf(Random.nextFloat() * 1080); val y = mutableStateOf(Random.nextFloat() * 2400)
    val radius = mutableStateOf(Random.nextFloat() * 5 + 2); val alpha = mutableStateOf(Random.nextFloat() * 0.5f + 0.1f)
    private val speed = Random.nextFloat() * 5 + 2
    fun update() { y.value += speed; if (y.value > 2400) { y.value = 0f; x.value = Random.nextFloat() * 1080 } }
}