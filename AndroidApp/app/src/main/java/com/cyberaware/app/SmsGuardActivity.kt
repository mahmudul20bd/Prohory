package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Data Models
data class RealSpamMessage(val id: String, val sender: String, val payload: String, val timestamp: String, val rawMillis: Long)
data class BlockedEntity(val number: String, val spamCount: Int)

class SmsGuardActivity : ComponentActivity() {
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
        setContent {
            CyberAwareTheme {
                SmsGuardScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsGuardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)

    var selectedTab by remember { mutableIntStateOf(0) }
    var autoBlockThreshold by remember { mutableIntStateOf(3) }
    var isLoading by remember { mutableStateOf(true) }

    val archivedSpams = remember { mutableStateListOf<RealSpamMessage>() }
    val manuallyUnblocked = remember { mutableStateListOf<String>() }

    val blacklist by remember(autoBlockThreshold, archivedSpams.size, manuallyUnblocked.size) {
        derivedStateOf {
            val spamCounts = archivedSpams.groupingBy { it.sender }.eachCount()
            spamCounts.filter { it.value >= autoBlockThreshold && !manuallyUnblocked.contains(it.key) }
                .map { BlockedEntity(it.key, it.value) }
                .sortedByDescending { it.spamCount }
        }
    }

    fun loadThreatArchive() {
        coroutineScope.launch(Dispatchers.IO) {
            val savedThreats = sharedPref.getStringSet("quarantine_list", emptySet()) ?: emptySet()
            val spams = mutableListOf<RealSpamMessage>()

            for (threat in savedThreats) {
                // SMS log format checking
                val parts = threat.split("|||")
                if (parts.size >= 3) {
                    val sender = parts[0]
                    val body = parts[1]
                    val millis = parts[2].toLongOrNull() ?: System.currentTimeMillis()

                    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
                    spams.add(RealSpamMessage(UUID.randomUUID().toString(), sender, body, dateString, millis))
                } else if (threat.startsWith("Spam from:")) {
                    // For dynamically formatted local threats in the new system
                    val sender = threat.replace("Spam from: ", "")
                    spams.add(RealSpamMessage(UUID.randomUUID().toString(), sender, "Spam SMS detected by background sensor.", "Recent", System.currentTimeMillis()))
                }
            }

            spams.sortByDescending { it.rawMillis }

            withContext(Dispatchers.Main) {
                archivedSpams.clear()
                archivedSpams.addAll(spams)
                isLoading = false
            }
        }
    }

    fun clearArchive() {
        // In the unified logic, clearing this also clears the Central Archive's SMS portion.
        // It's acceptable for users who just want to clear it quickly.
        sharedPref.edit().remove("quarantine_list").apply()
        archivedSpams.clear()
        Toast.makeText(context, "Sensor Cache Cleared. Health Restored.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) { loadThreatArchive() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        AdvancedFirewallBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Security, contentDescription = "Firewall", tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("SMS GUARD", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Active Threat Interception", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AUTO-BLOCK CONFIGURATOR (Responsive)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = Color(0xFF131C2B).copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AUTO-DETECT LIMIT", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Warn me to block a number if it sends this many scams.", color = Color.Gray, fontSize = 9.sp, lineHeight = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF0B1221), RoundedCornerShape(20.dp)).border(1.dp, Color.DarkGray, RoundedCornerShape(20.dp))) {
                        IconButton(onClick = { if (autoBlockThreshold > 1) autoBlockThreshold-- }, modifier = Modifier.size(35.dp)) {
                            Icon(Icons.Default.Remove, null, tint = Color.White)
                        }
                        Text("$autoBlockThreshold", color = Color(0xFFFF9800), fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { if (autoBlockThreshold < 10) autoBlockThreshold++ }, modifier = Modifier.size(35.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FirewallTab("SCAM MESSAGES (${archivedSpams.size})", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                FirewallTab("NEEDS BLOCKING (${blacklist.size})", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Content (Fully Responsive with weight)
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                color = Color(0xFF0B1221).copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.05f))
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AnimatedScannerPulse()
                    }
                } else if (selectedTab == 0) {
                    // SPAM INBOX
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("RECENT SCAM ATTEMPTS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (archivedSpams.isNotEmpty()) {
                                IconButton(onClick = { clearArchive() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        }
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                        if(archivedSpams.isEmpty()) {
                            EmptyStateMessage("NO SCAM MESSAGES FOUND.\nYOUR INBOX IS SAFE.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                items(archivedSpams, key = { it.id }) { msg -> AnimatedSpamMessageCard(msg) }
                            }
                        }
                    }
                } else {
                    // BLACKLIST (Requires Action)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("THESE NUMBERS CROSSED YOUR LIMIT", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                        if(blacklist.isEmpty()) {
                            EmptyStateMessage("NO NUMBERS TO BLOCK.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                items(blacklist, key = { it.number }) { entity ->
                                    ActionableBlacklistCard(entity, context) {
                                        manuallyUnblocked.add(entity.number)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔥 UNIFIED ARCHIVE ROUTE (Direct to Archive Tab)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(20.dp).clickable {
                    // Dashboard e signal pathacchi jate direct tab 2 open hoy
                    val intent = Intent(context, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("TARGET_TAB", 2) // 2 means Intelligence Archive Tab
                    }
                    context.startActivity(intent)
                },
                color = Color(0xFFFF9800).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Storage, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("VIEW IN CENTRAL ARCHIVE", color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Rounded.ArrowForwardIos, null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ==========================================
// ADVANCED UI COMPONENTS
// ==========================================

@Composable
fun AnimatedSpamMessageCard(msg: RealSpamMessage) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Red.copy(0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp).animateContentSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(msg.sender, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(msg.timestamp, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val displayPayload = if (isExpanded) msg.payload else msg.payload.take(50) + if (msg.payload.length > 50) "..." else ""
            Text(displayPayload, color = Color.White.copy(0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)

            if (!isExpanded && msg.payload.length > 50) {
                Text("Tap to read full message", color = NeonCyan, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun ActionableBlacklistCard(entity: BlockedEntity, context: Context, onIgnore: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(entity.number, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Surface(color = Color.Red.copy(0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color.Red)) {
                    Text("${entity.spamCount} SCAMS", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f).height(35.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Gray),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("IGNORE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Button(
                    onClick = {
                        try {
                            val blockIntent = Intent(Intent.ACTION_INSERT).apply {
                                data = Uri.parse("tel:${entity.number}")
                            }
                            Toast.makeText(context, "Please block this number in your phone app.", Toast.LENGTH_LONG).show()
                            context.startActivity(blockIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open block settings.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(35.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("BLOCK IN PHONE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Green.copy(alpha = 0.5f), modifier = Modifier.size(50.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = Color.Gray, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

@Composable
fun FirewallTab(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFF131C2B)
    val borderColor = if (isSelected) Color(0xFFFF9800) else Color.DarkGray
    val textColor = if (isSelected) Color(0xFFFF9800) else Color.Gray

    Surface(modifier = modifier.height(40.dp).clickable { onClick() }, color = bgColor, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, borderColor)) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun AnimatedScannerPulse() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size((50 * pulseScale).dp).border(2.dp, Color(0xFFFF9800).copy(alpha = 0.5f), CircleShape))
        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(30.dp))
    }
}

@Composable
fun AdvancedFirewallBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt() + 1
        val rows = (size.height / gridSize).toInt() + 1

        for (i in 0..cols) {
            val x = (i * gridSize) - offsetAnim
            drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 2f)
        }
        for (i in 0..rows) {
            val y = (i * gridSize) - offsetAnim
            drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f)
        }
    }
}