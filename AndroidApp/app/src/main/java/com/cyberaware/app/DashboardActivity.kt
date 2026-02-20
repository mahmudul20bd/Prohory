package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberaware.app.network.DashboardStats
import com.cyberaware.app.network.SystemLog
import com.cyberaware.app.ui.theme.CyberAwareTheme
import kotlinx.coroutines.launch
import java.util.*
import android.widget.Toast

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                MainIntelligenceContainer()
            }
        }
    }
}

@Composable
fun MainIntelligenceContainer(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val myUsername = sharedPref.getString("username", "") ?: ""
    val myRole = sharedPref.getString("role", "Agent") ?: "Agent"

    // 🔥 RUNTIME PERMISSION LOGIC 🔥
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[android.Manifest.permission.RECEIVE_SMS] == true
        val notifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (!smsGranted || !notifGranted) {
            Toast.makeText(context, "Threat Shield requires SMS & Notification permissions.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        // ১. Request Permissions on Startup
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())

        // ২. Load Dashboard Data
        if (token.isNotEmpty()) {
            viewModel.loadDashboardData(token, myUsername, myRole)
        }
        viewModel.connectWebSocket()
    }
    // 🔥 RUNTIME PERMISSION LOGIC END 🔥

    val userProfile by viewModel.userProfile.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val liveThreat by viewModel.liveThreatLevel.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    val dbUserName = userProfile?.username ?: myUsername.ifEmpty { "Agent" }
    val dbUserRole = userProfile?.role ?: myRole

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            IntelligenceSidebar(
                userName = dbUserName,
                role = dbUserRole,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            containerColor = Color(0xFF020408),
            bottomBar = { CyberBottomBar(selectedTab) { selectedTab = it } }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedBackgroundGlow()

                when(selectedTab) {
                    0 -> HomeDashboard(
                        userName = dbUserName,
                        stats = stats,
                        liveThreat = liveThreat,
                        recentLogs = recentLogs,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    1 -> GlobalThreatMapScreen()
                    2 -> ThreatHistoryScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }
}
@Composable
fun HomeDashboard(userName: String, stats: DashboardStats?, liveThreat: Float, recentLogs: List<SystemLog>, onMenuClick: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val blocked = stats?.blockedScams ?: 0
    val safe = stats?.safeLinks ?: 0
    val baseScore = 100 - (blocked * 5) + (safe * 1)
    val finalScore = baseScore.coerceIn(10, 100)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        TopHeader(userName, greeting, hour, onMenuClick)
        Spacer(Modifier.height(30.dp))
        SystemHealthRing(finalScore)
        Spacer(Modifier.height(35.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("SCANS", "${stats?.totalScans ?: "-"}", Icons.Rounded.Analytics, Color(0xFF3B82F6), Modifier.weight(1f))
            StatBox("BLOCKED", "$blocked", Icons.Rounded.GppBad, Color(0xFFEF4444), Modifier.weight(1f))
            StatBox("SECURE", "$safe", Icons.Rounded.HealthAndSafety, Color(0xFF10B981), Modifier.weight(1f))
        }

        Spacer(Modifier.height(30.dp))
        SectionHeader("REAL-TIME THREAT INTENSITY")
        Spacer(Modifier.height(15.dp))
        ThreatSpikeGraph(liveThreat)
        Spacer(Modifier.height(30.dp))

        SectionHeader("SYSTEM DEFENSE MODULES")
        Spacer(Modifier.height(15.dp))
        ModuleGrid(context)

        Spacer(Modifier.height(30.dp))
        SectionHeader("RECENT SYSTEM LOGS")
        Spacer(Modifier.height(15.dp))
        RecentLogsSection(recentLogs)

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SystemHealthRing(targetScore: Int) {
    val animatedScore by animateIntAsState(targetValue = targetScore, animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing), label = "scoreAnim")
    val infiniteTransition = rememberInfiniteTransition(label = "hub")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(15000, easing = LinearEasing)), label = "r")

    val color = when {
        animatedScore >= 80 -> Color(0xFF10B981)
        animatedScore >= 50 -> Color.Yellow
        else -> Color.Red
    }

    val statusText = when {
        animatedScore >= 80 -> "SYSTEM OPTIMAL"
        animatedScore >= 50 -> "WARNING STATE"
        else -> "SYSTEM CRITICAL"
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        Box(Modifier.size(210.dp).rotate(rotation).border(2.dp, color.copy(0.3f), CircleShape))
        Surface(modifier = Modifier.size(160.dp), shape = CircleShape, color = Color(0xFF0F172A), border = BorderStroke(3.dp, color)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = "$animatedScore%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text(text = "SYSTEM HEALTH", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp))
                Text(text = statusText, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun RecentLogsSection(logs: List<SystemLog>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (logs.isEmpty()) {
            Text("No recent activity found. Syncing...", color = Color.Gray, fontSize = 12.sp)
        } else {
            logs.forEach { log ->
                val statusColor = when (log.actionType.uppercase()) {
                    "LOGIN", "REGISTRATION" -> Color.Cyan
                    "REPORT", "THREAT", "SCAM" -> Color.Red
                    else -> Color.Green
                }
                val time = log.timestamp?.take(10) ?: "Just now"
                val performer = if (log.username == null || log.username == "null") "System" else log.username
                val finalMessage = log.details.replace("Agent null", "Agent $performer")

                LogItem(finalMessage, time, statusColor)
            }
        }
    }
}

@Composable
fun LogItem(message: String, time: String, statusColor: Color) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(statusColor, CircleShape))
            Spacer(Modifier.width(15.dp))
            Column {
                Text(message, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(time, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = Color(0xFF131C2B), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ThreatSpikeGraph(liveThreat: Float) {
    val spikeData = remember { mutableStateListOf<Float>().apply { repeat(30) { add(0.1f) } } }

    LaunchedEffect(liveThreat) {
        spikeData.removeAt(0)
        spikeData.add(liveThreat)
    }

    Surface(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFF0D121D), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            val path = Path()
            val step = size.width / (spikeData.size - 1)

            path.moveTo(0f, size.height * (1f - spikeData[0]))
            for (i in 1 until spikeData.size) {
                val x1 = (i - 1) * step
                val y1 = size.height * (1f - spikeData[i - 1])
                val x2 = i * step
                val y2 = size.height * (1f - spikeData[i])
                path.quadraticBezierTo(x1 + step / 2, y1, x2, y2)
            }

            drawPath(path = path, brush = Brush.horizontalGradient(listOf(Color.Cyan, Color.Yellow, Color.Red)), style = Stroke(width = 5f))
        }
    }
}

@Composable
fun ModuleGrid(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("Phishing", Icons.Rounded.Link, Color(0xFF3B82F6), Modifier.weight(1f)) { context.startActivity(Intent(context, PhishingScannerActivity::class.java)) }
            FeatureTile("OCR AI", Icons.Rounded.DocumentScanner, Color(0xFF8B5CF6), Modifier.weight(1f)) { context.startActivity(Intent(context, ScreenshotAnalysisActivity::class.java)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("SMS Guard", Icons.Rounded.Sms, Color(0xFFFF9800), Modifier.weight(1f)) { context.startActivity(Intent(context, SmsGuardActivity::class.java)) }
            FeatureTile("Secure Vault", Icons.Rounded.Https, Color(0xFF10B981), Modifier.weight(1f)) { context.startActivity(Intent(context, SecureVaultActivity::class.java)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("Network Scan", Icons.Rounded.WifiTethering, Color(0xFFE91E63), Modifier.weight(1f)) { context.startActivity(Intent(context, NetworkScanActivity::class.java)) }
            FeatureTile("App Lock", Icons.Rounded.Lock, Color(0xFF00BCD4), Modifier.weight(1f)) { context.startActivity(Intent(context, AppLockActivity::class.java)) }
        }
    }
}

@Composable
fun IntelligenceSidebar(userName: String, role: String, onClose: () -> Unit) {
    val context = LocalContext.current
    ModalDrawerSheet(modifier = Modifier.width(310.dp).fillMaxHeight(), drawerContainerColor = Color(0xFF0B1221)) {
        Column(Modifier.padding(24.dp).fillMaxHeight()) {
            Icon(Icons.Rounded.AccountCircle, null, tint = Color.Cyan, modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(10.dp))
            Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(role, color = Color.Cyan.copy(0.7f), fontSize = 12.sp)
            Divider(Modifier.padding(vertical = 20.dp), color = Color.White.copy(0.05f))

            NavigationDrawerItem(label = { Text("Dashboard", color = Color.White) }, selected = true, onClick = onClose)
            NavigationDrawerItem(label = { Text("Neural Logs") }, selected = false, onClick = { onClose(); context.startActivity(Intent(context, NeuralLogsActivity::class.java)) })

            Spacer(Modifier.weight(1f))

            // 🔥 LOGOUT BUTTON ADDED HERE
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    onClose()
                    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply() // Clear Data
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                },
                color = Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Logout, contentDescription = "Logout", tint = Color.Red)
                    Spacer(Modifier.width(12.dp))
                    Text("SECURE LOGOUT", color = Color.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Prohory v1.0", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun FeatureTile(title: String, icon: ImageVector, themeColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(110.dp).clickable { onClick() }, shape = RoundedCornerShape(24.dp), color = Color(0xFF131C2B), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = themeColor, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CyberBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(modifier = Modifier.padding(20.dp).fillMaxWidth().height(70.dp), shape = RoundedCornerShape(25.dp), color = Color(0xFF131C2B).copy(alpha = 0.95f), border = BorderStroke(1.dp, Color.White.copy(0.1f))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(0) }) { Icon(Icons.Rounded.Home, null, tint = if(selected == 0) Color.Cyan else Color.Gray) }
            IconButton(onClick = { onSelect(1) }) { Icon(Icons.Rounded.Public, null, tint = if(selected == 1) Color.Cyan else Color.Gray) }
            IconButton(onClick = { onSelect(2) }) { Icon(Icons.Rounded.History, null, tint = if(selected == 2) Color.Cyan else Color.Gray) }
            IconButton(onClick = { onSelect(3) }) { Icon(Icons.Rounded.Settings, null, tint = if(selected == 3) Color.Cyan else Color.Gray) }
        }
    }
}

@Composable fun SectionHeader(title: String) { Text(title, color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth()) }

// 🔥 HEADER UI FIX (Name Ellipsis, Weight & Expert Chat Link)
@Composable
fun TopHeader(name: String, greeting: String, hour: Int, onMenuClick: () -> Unit) {
    // 🔥 Intent কল করার জন্য Context ডিক্লেয়ার করা হলো
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ১. Left Side: Menu Icon
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Rounded.Menu, null, tint = Color.White)
        }

        // ২. Center: Greeting & Name (Wrapped text)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            Icon(if (hour in 6..18) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay, null, tint = Color.Cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$greeting, $name",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1, // Only 1 line
                overflow = TextOverflow.Ellipsis // Shows ... if name is too long
            )
        }

        // ৩. Right Side: Expert Chat Icon (🔥 Updated)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Cyan.copy(0.1f), CircleShape)
                .border(1.dp, Color.Cyan.copy(0.3f), CircleShape)
                .clip(CircleShape) // ক্লিকের অ্যানিমেশনটা সার্কেলের ভেতর রাখার জন্য
                .clickable {
                    // চ্যাট অ্যাক্টিভিটি ওপেন করার লজিক
                    context.startActivity(Intent(context, ExpertChatActivity::class.java))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SupportAgent, contentDescription = "Expert Support", tint = Color.Cyan)
        }
    }
}

@Composable fun AnimatedBackgroundGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val alpha by infiniteTransition.animateFloat(0.1f, 0.3f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "a")
    Canvas(Modifier.fillMaxSize()) { drawCircle(Brush.radialGradient(listOf(Color(0xFF1E3A8A).copy(alpha), Color.Transparent), center = Offset(size.width * 0.8f, size.height * 0.2f), radius = 1000f)) }
}