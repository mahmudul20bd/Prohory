package com.cyberaware.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    // 🔥 THE FIX: using derivedStateOf ensures UI updates instantly when archivedSpams changes
    val blacklist by remember(autoBlockThreshold) {
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
                val parts = threat.split("|||")
                if (parts.size >= 3) {
                    val sender = parts[0]
                    val body = parts[1]
                    val millis = parts[2].toLongOrNull() ?: System.currentTimeMillis()

                    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
                    spams.add(RealSpamMessage(UUID.randomUUID().toString(), sender, body, dateString, millis))
                }
            }

            // Sort newest first
            spams.sortByDescending { it.rawMillis }

            withContext(Dispatchers.Main) {
                archivedSpams.clear()
                archivedSpams.addAll(spams)
                isLoading = false
            }
        }
    }

    fun clearArchive() {
        sharedPref.edit().remove("quarantine_list").apply()
        archivedSpams.clear()
        Toast.makeText(context, "Archive Cleared", Toast.LENGTH_SHORT).show()
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadThreatArchive()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        FirewallGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Security, contentDescription = "Firewall", tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("SMS GUARD", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Threat Interception Vault", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AUTO-BLOCK CONFIGURATOR
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = Color(0xFF131C2B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AUTO-BLOCK THRESHOLD", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Block sender if spam count reaches limit.", color = Color.Gray, fontSize = 9.sp, lineHeight = 14.sp)
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
                FirewallTab("THREAT INBOX (${archivedSpams.size})", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                FirewallTab("BLACKLIST (${blacklist.size})", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Content
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp).padding(bottom = 20.dp),
                color = Color(0xFF0B1221).copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.05f))
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF9800)) }
                } else if (selectedTab == 0) {
                    // THREAT INBOX (Quarantine)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("BACKGROUND INTERCEPTED SPAMS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            if (archivedSpams.isNotEmpty()) {
                                IconButton(onClick = { clearArchive() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        }
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                        if(archivedSpams.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("VAULT EMPTY. SYSTEM SECURE.", color = Color.Gray, fontFamily = FontFamily.Monospace) }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.animateContentSize()) {
                                items(archivedSpams, key = { it.id }) { msg -> SpamMessageCard(msg) }
                            }
                        }
                    }
                } else {
                    // BLACKLIST
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ENTITIES EXCEEDING THRESHOLD", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                        if(blacklist.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("BLACKLIST EMPTY.", color = Color.Gray, fontFamily = FontFamily.Monospace) }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.animateContentSize()) {
                                items(blacklist, key = { it.number }) { entity ->
                                    BlacklistCard(entity) {
                                        manuallyUnblocked.add(entity.number)
                                        Toast.makeText(context, "${entity.number} Unblocked.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpamMessageCard(msg: RealSpamMessage) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.Red.copy(0.3f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(msg.sender, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(msg.timestamp, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg.payload, color = Color.White.copy(0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BlacklistCard(entity: BlockedEntity, onUnblock: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.DarkGray)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(entity.number, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Surface(color = Color.Red.copy(0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color.Red)) {
                    Text("${entity.spamCount} SPAMS", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onUnblock,
                modifier = Modifier.fillMaxWidth().height(35.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESTORE / UNBLOCK", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
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
fun FirewallGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}