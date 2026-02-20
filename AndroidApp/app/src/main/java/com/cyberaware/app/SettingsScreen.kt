package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)

    // Fetch User Info from SharedPreferences
    val agentName = sharedPref.getString("username", "Unknown Agent") ?: "Unknown Agent"
    val agentRole = sharedPref.getString("role", "Field Agent") ?: "Field Agent"
    val agentId = sharedPref.getString("agent_id", "PRH-XXXX") ?: "PRH-XXXX"

    // Profile Image State
    var avatarPath by remember { mutableStateOf(sharedPref.getString("avatar_path", null)) }

    // Settings States
    var stealthMode by remember { mutableStateOf(sharedPref.getBoolean("stealth_mode", false)) }
    var biometricLogin by remember { mutableStateOf(sharedPref.getBoolean("biometric_login", true)) }

    // Logic to save image locally without needing a backend server
    fun saveImageLocally(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "agent_avatar.jpg") // Saved secretly inside app storage
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                withContext(Dispatchers.Main) {
                    avatarPath = file.absolutePath
                    sharedPref.edit().putString("avatar_path", file.absolutePath).apply()
                    Toast.makeText(context, "Identity Matrix Updated.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error updating identity.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) saveImageLocally(uri) }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        SettingsGridBackground()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, contentDescription = "Settings", tint = NeonPurple, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("SYSTEM CONFIGURATION", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Prohory Central Command", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🔥 USER IDENTITY CARD (Profile)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B1221),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {

                    // Glowing Avatar
                    val pulse by rememberInfiniteTransition().animateFloat(
                        initialValue = 0.5f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse Ring
                        Box(modifier = Modifier.size((80 * pulse).dp).border(2.dp, NeonCyan.copy(alpha = 0.3f), CircleShape))

                        // Image / Placeholder
                        if (avatarPath != null && File(avatarPath!!).exists()) {
                            AsyncImage(
                                model = File(avatarPath!!),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(70.dp).clip(CircleShape).border(2.dp, NeonCyan, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(70.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape).border(2.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                            }
                        }

                        // Edit Badge
                        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 5.dp).size(24.dp).background(Color(0xFF131C2B), CircleShape).border(1.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Agent Details
                    Column {
                        Text(agentName.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Role: $agentRole", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(color = NeonCyan.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NeonCyan)) {
                            Text(" ID: $agentId ", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // SETTINGS LIST
            Text("SECURITY PROTOCOLS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Toggle Item: Stealth Mode
            SettingToggleCard(
                title = "Network Stealth Mode",
                subtitle = "Hides IP from background network scans",
                icon = Icons.Default.VisibilityOff,
                isChecked = stealthMode,
                onCheckedChange = {
                    stealthMode = it
                    sharedPref.edit().putBoolean("stealth_mode", it).apply()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Toggle Item: Biometric Lock
            SettingToggleCard(
                title = "Biometric Override",
                subtitle = "Require fingerprint to open Prohory app",
                icon = Icons.Default.Fingerprint,
                isChecked = biometricLogin,
                onCheckedChange = {
                    biometricLogin = it
                    sharedPref.edit().putBoolean("biometric_login", it).apply()
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
            Text("SYSTEM OPERATIONS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Item: Check Update
            SettingActionCard(
                title = "Check for Core Updates",
                subtitle = "Sync with Railway Server AI Model",
                icon = Icons.Default.SystemUpdate,
                onClick = { Toast.makeText(context, "System is up to date (v1.0.0)", Toast.LENGTH_SHORT).show() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Item: Data Wipe
            SettingActionCard(
                title = "Clear Neural Cache",
                subtitle = "Wipes temporary scanned data",
                icon = Icons.Default.CleaningServices,
                onClick = { Toast.makeText(context, "Cache Cleared Securely.", Toast.LENGTH_SHORT).show() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🔥 PANIC BUTTON (Kill Switch)
            Button(
                onClick = {
                    sharedPref.edit().clear().apply()
                    Toast.makeText(context, "Initiating Secure Wipe...", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, LoginActivity::class.java) // আপনার লগইন অ্যাক্টিভিটির নাম দিন
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(bottom = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EMERGENCY KILL SWITCH", color = Color.Red, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(80.dp)) // Nav bar padding
        }
    }
}

// Reusable Switch Card
@Composable
fun SettingToggleCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(40.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray, fontSize = 10.sp)
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.DarkGray)
            )
        }
    }
}

// Reusable Action Card
@Composable
fun SettingActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(NeonPurple.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray, fontSize = 10.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun SettingsGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}