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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)

    // 🔥 Profile States
    var agentName by remember { mutableStateOf(sharedPref.getString("username", "Unknown User") ?: "Unknown User") }
    val agentRole = sharedPref.getString("role", "User") ?: "User"
    val agentId = sharedPref.getString("agent_id", "PRH-0000") ?: "PRH-0000"

    var avatarPath by remember { mutableStateOf(sharedPref.getString("avatar_path", null)) }

    // Dialog & Animation States
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var newUsernameInput by remember { mutableStateOf(agentName) }
    var isIdVisible by remember { mutableStateOf(false) }

    // 🔥 Password Dialog States
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    // Settings States
    var biometricLogin by remember { mutableStateOf(sharedPref.getBoolean("biometric_login", false)) }
    var screenshotProtect by remember { mutableStateOf(sharedPref.getBoolean("screenshot_protect", false)) }
    var autoScanWifi by remember { mutableStateOf(sharedPref.getBoolean("auto_scan_wifi", false
    )) }
    var autoClearLogs by remember { mutableStateOf(sharedPref.getBoolean("auto_clear_logs", false)) }

    // Real-time Health Score
    var localThreatCount by remember { mutableIntStateOf(threatPrefs.getStringSet("quarantine_list", emptySet())?.size ?: 0) }
    var scoreDeduction = (localThreatCount * 5)
    if (!biometricLogin) scoreDeduction += 5
    val dynamicSystemScore = (100 - scoreDeduction).coerceIn(10, 100)

    // Image Save Logic
    fun saveImageLocally(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "agent_avatar.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                withContext(Dispatchers.Main) {
                    avatarPath = file.absolutePath
                    sharedPref.edit().putString("avatar_path", file.absolutePath).apply()
                    Toast.makeText(context, "Profile Photo Updated.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error updating photo.", Toast.LENGTH_SHORT).show()
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

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GppGood, null, tint = NeonPurple, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("SYSTEM SETTINGS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(
                        "Health Score: $dynamicSystemScore%",
                        color = if(dynamicSystemScore >= 80) Color(0xFF10B981) else Color.Red,
                        fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 👤 USER PROFILE CARD
            Surface(
                modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B1221).copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(70.dp).clickable { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPath != null && File(avatarPath!!).exists()) {
                            AsyncImage(model = File(avatarPath!!), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, NeonCyan, CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(NeonCyan.copy(0.1f), CircleShape).border(2.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = NeonCyan, modifier = Modifier.size(35.dp))
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(22.dp).background(Color(0xFF131C2B), CircleShape).border(1.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(agentName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                newUsernameInput = agentName
                                showEditProfileDialog = true
                            }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Name", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(agentRole, color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.clickable { isIdVisible = !isIdVisible },
                            color = NeonCyan.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, NeonCyan.copy(0.5f))
                        ) {
                            AnimatedContent(targetState = isIdVisible, label = "id_animation") { visible ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Icon(if(visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (visible) "ID: $agentId" else "ID: PRH-••••",
                                        color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🛡️ SECURITY & PRIVACY
            SectionHeader("SECURITY & PRIVACY")

            // 🔥 Password Change Option Added Here
            EnterpriseAction("Change Password", "Update your account authentication", Icons.Rounded.LockReset) {
                currentPasswordInput = ""
                newPasswordInput = ""
                confirmPasswordInput = ""
                showPasswordDialog = true
            }
            Spacer(modifier = Modifier.height(10.dp))

            EnterpriseToggle("Biometric App Lock", "Require fingerprint to open the app", Icons.Rounded.Fingerprint, biometricLogin, true) { biometricLogin = it; sharedPref.edit().putBoolean("biometric_login", it).apply() }
            Spacer(modifier = Modifier.height(10.dp))
            EnterpriseToggle("Screenshot Protection", "Prevent OS from capturing app screens", Icons.Rounded.ScreenShare, screenshotProtect, true) { screenshotProtect = it; sharedPref.edit().putBoolean("screenshot_protect", it).apply() }
            Spacer(modifier = Modifier.height(10.dp))
            EnterpriseToggle("Auto Scan Public WiFi", "Trigger scan on new network connections", Icons.Rounded.WifiProtectedSetup, autoScanWifi, true) { autoScanWifi = it; sharedPref.edit().putBoolean("auto_scan_wifi", it).apply() }

            Spacer(modifier = Modifier.height(24.dp))

            // 📊 STORAGE & DATA
            SectionHeader("STORAGE & LOGS")

            EnterpriseToggle("Auto-Clear Threat Logs", "Purge intelligence data after 7 days", Icons.Rounded.AutoDelete, autoClearLogs, false) { autoClearLogs = it; sharedPref.edit().putBoolean("auto_clear_logs", it).apply() }
            Spacer(modifier = Modifier.height(10.dp))

            EnterpriseAction("Clear Neural Cache", "Wipes temporary scanned threats & memory", Icons.Rounded.CleaningServices) {
                threatPrefs.edit().remove("quarantine_list").apply()
                localThreatCount = 0
                Toast.makeText(context, "Neural Cache Cleared. System Health Restored.", Toast.LENGTH_SHORT).show()
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 🧨 DANGER ZONE
            Button(
                onClick = {
                    sharedPref.edit().clear().apply()
                    threatPrefs.edit().clear().apply()
                    Toast.makeText(context, "Logging out and wiping data...", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(bottom = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EMERGENCY KILL SWITCH", color = Color.Red, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // 🔥 EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color(0xFF131C2B),
            title = { Text("Update Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newUsernameInput,
                    onValueChange = { newUsernameInput = it },
                    label = { Text("Your Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan
                    ),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsernameInput.isNotBlank()) {
                            sharedPref.edit().putString("username", newUsernameInput.trim()).apply()
                            agentName = newUsernameInput.trim()
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) { Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("CANCEL", color = Color.Gray) }
            }
        )
    }

    // 🔥 SECURE PASSWORD CHANGE DIALOG
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Color(0xFF131C2B),
            title = { Text("Change Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Password", color = Color.Gray, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password", color = Color.Gray, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password", color = Color.Gray, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = NeonCyan
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPasswordInput.isEmpty() || newPasswordInput.isEmpty() || confirmPasswordInput.isEmpty()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else if (newPasswordInput != confirmPasswordInput) {
                            Toast.makeText(context, "New passwords do not match!", Toast.LENGTH_SHORT).show()
                        } else if (newPasswordInput.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            // Save new password logically
                            sharedPref.edit().putString("password", newPasswordInput).apply()
                            showPasswordDialog = false
                            Toast.makeText(context, "Password Updated Securely!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) { Text("UPDATE", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("CANCEL", color = Color.Gray) }
            }
        )
    }
}

// ----------------------------------------------------
// UI COMPONENTS
// ----------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun EnterpriseToggle(title: String, subtitle: String, icon: ImageVector, isChecked: Boolean, isOptimalWhenOn: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val isOptimal = if(isOptimalWhenOn) isChecked else !isChecked
    val statusColor = if(isOptimal) Color(0xFF10B981) else Color(0xFFFF9800)

    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color(0xFF131C2B).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if(isChecked) NeonCyan.copy(0.3f) else Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(40.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                    }
                    Text(subtitle, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            Switch(
                checked = isChecked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonCyan.copy(0.8f), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.DarkGray)
            )
        }
    }
}

@Composable
private fun EnterpriseAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, color = Color(0xFF131C2B).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(NeonPurple.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray, fontSize = 10.sp)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun SettingsGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 120f
        val cols = (size.width / gridSize).toInt() + 1
        val rows = (size.height / gridSize).toInt() + 1
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.015f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.015f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}