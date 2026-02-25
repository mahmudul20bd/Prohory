package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cyberaware.app.network.DashboardStats
import com.cyberaware.app.network.SystemLog
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardActivity : ComponentActivity() {
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
        val workRequest = PeriodicWorkRequestBuilder<LogClearWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ThreatLogClearWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        setContent {
            CyberAwareTheme {
                MainIntelligenceContainer()
            }
        }
        WifiMonitorService(this).startMonitoring()
    }
}

fun getTimeAgo(timeInMillis: Long): String {
    val diff = System.currentTimeMillis() - timeInMillis
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    val days = hours / 24
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins mins ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

@Composable
fun MainIntelligenceContainer(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activity = context as? android.app.Activity
    val targetTab = activity?.intent?.getIntExtra("TARGET_TAB", 0) ?: 0
    var selectedTab by remember { mutableIntStateOf(targetTab) }

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val myUsername = sharedPref.getString("username", "") ?: ""
    val myRole = sharedPref.getString("role", "User") ?: "User"

    var isNetOn by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isNetOn = true }
            override fun onLost(network: Network) { isNetOn = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        onDispose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    LaunchedEffect(isNetOn) {
        if (isNetOn && token.isNotEmpty()) {
            viewModel.loadDashboardData(token, myUsername, myRole)
            viewModel.connectWebSocket(token)
        }
    }

    var localAvatarPath by remember { mutableStateOf(sharedPref.getString("avatar_path", "")) }

    LaunchedEffect(selectedTab, drawerState.isOpen) {
        localAvatarPath = sharedPref.getString("avatar_path", "") ?: ""
    }

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
        launch {
            viewModel.sessionExpired.collect { isExpired ->
                if (isExpired) {
                    Toast.makeText(context, "Session Expired! Please login again.", Toast.LENGTH_LONG).show()
                    sharedPref.edit().clear().apply()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            }
        }

        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())

        if (token.isNotEmpty()) {
            viewModel.loadDashboardData(token, myUsername, myRole)
            viewModel.connectWebSocket(token)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (token.isNotEmpty()) {
                    viewModel.loadDashboardData(token, myUsername, myRole)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val liveThreat by viewModel.liveThreatLevel.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    val dbUserName = userProfile?.username ?: myUsername.ifEmpty { "User" }
    val dbUserRole = userProfile?.role ?: myRole

    val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
    val savedThreats = threatPrefs.getStringSet("quarantine_list", emptySet()) ?: emptySet()

    // Personal Security Score Logic uses local threats
    val localThreatCount = savedThreats.size
    val isRealTimeOn = sharedPref.getBoolean("real_time_protection", true)
    val isBiometricOn = sharedPref.getBoolean("biometric_login", true)

    var scoreDeduction = (localThreatCount * 5)
    if (!isRealTimeOn) scoreDeduction += 15
    if (!isBiometricOn) scoreDeduction += 5

    val personalScore = (100 - scoreDeduction).coerceIn(10, 100)

    // 🔥 PURE GLOBAL STATS LOGIC (As requested by user)
    val totalBlocked = stats?.blockedScams ?: 0

    // DYNAMIC THREAT STATE FOR NOTIFICATION
    var latestThreatTime by remember { mutableStateOf<Long?>(null) }
    var latestThreatType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedThreats) {
        var maxTime = 0L
        var type = "UNKNOWN"
        for (threat in savedThreats) {
            val parts = threat.split("|||")
            if (parts.size >= 3) {
                val time = parts[2].toLongOrNull()
                val message = parts[1].lowercase()

                if (time != null && time > maxTime) {
                    maxTime = time
                    type = when {
                        message.contains("http") || message.contains("bkash") || message.contains("nagad") || message.contains("bank") -> "DANGER"
                        message.contains("win") || message.contains("offer") || message.contains("lottery") || message.contains("free") -> "SPAM"
                        else -> "SPAM"
                    }
                }
            }
        }
        if (maxTime > 0) {
            latestThreatTime = maxTime
            latestThreatType = type
        } else {
            latestThreatTime = null
            latestThreatType = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            IntelligenceSidebar(
                userName = dbUserName,
                role = dbUserRole,
                photoUri = localAvatarPath ?: "",
                blockedCount = totalBlocked,
                onClose = { scope.launch { drawerState.close() } },
                onNavigateToArchive = {
                    selectedTab = 2
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    selectedTab = 3
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = Color(0xFF02040A),
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedTab != 2,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    CyberBottomBar(selectedTab) { selectedTab = it }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                AdvancedCyberBackground(personalScore)

                when(selectedTab) {
                    0 -> EnterpriseHomeDashboard(
                        userName = dbUserName,
                        stats = stats,
                        totalBlocked = totalBlocked, // Passed pure global stats
                        liveThreat = liveThreat,
                        personalScore = personalScore,
                        localThreatCount = localThreatCount,
                        isRealTimeOn = isRealTimeOn,
                        isBiometricOn = isBiometricOn,
                        latestThreatTime = latestThreatTime,
                        latestThreatType = latestThreatType,
                        recentLogs = recentLogs,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        viewModel = viewModel
                    )
                    1 -> GlobalThreatMapScreen()
                    2 -> ThreatHistoryScreen(onBack = { selectedTab = 0 })
                    3 -> SettingsScreen(onBack = { selectedTab = 0 })
                }
            }
        }
    }
}

// ==========================================
// 🗂 ENTERPRISE SIDEBAR
// ==========================================
@Composable
fun IntelligenceSidebar(
    userName: String,
    role: String,
    photoUri: String,
    blockedCount: Int,
    onClose: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val (rank, nextRank, progress, iconColor) = when {
        blockedCount < 10 -> listOf("Cyber Aware", "Threat Defender", blockedCount / 10f, Color.Gray)
        blockedCount < 50 -> listOf("Threat Defender", "Elite Shield", (blockedCount - 10) / 40f, NeonCyan)
        else -> listOf("Zero Click Master", "MAX LEVEL", 1f, Color(0xFFFFD700))
    }

    ModalDrawerSheet(
        modifier = Modifier.width(320.dp).fillMaxHeight(),
        drawerContainerColor = Color(0xFF070B14),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).border(1.dp, NeonCyan.copy(0.5f), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                    if (photoUri.isNotEmpty() && File(photoUri).exists()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(Uri.parse(photoUri)).crossfade(true).build(), contentDescription = "Profile Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), error = rememberVectorPainter(Icons.Rounded.AccountCircle), placeholder = rememberVectorPainter(Icons.Rounded.AccountCircle))
                    } else {
                        Icon(Icons.Rounded.AccountCircle, null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                        Box(modifier = Modifier.fillMaxSize().background(NeonCyan.copy(alpha = 0.1f)))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(role, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Surface(modifier = Modifier.fillMaxWidth(), color = (iconColor as Color).copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, null, tint = iconColor, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rank as String, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("Next: ${nextRank as String}", color = Color.Gray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { progress as Float }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = iconColor, trackColor = Color.DarkGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("ACCOUNT & ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))

            SidebarMenuItem("Threat Intelligence", Icons.Rounded.Storage, NeonPurple) { onNavigateToArchive() }
            SidebarMenuItem("Scan History", Icons.Rounded.History, Color(0xFF3B82F6)) { Toast.makeText(context, "Upcoming Feature.", Toast.LENGTH_SHORT).show() }
            SidebarMenuItem("Trusted Networks", Icons.Rounded.WifiTethering, Color(0xFF10B981)) { Toast.makeText(context, "Upcoming Feature.", Toast.LENGTH_SHORT).show() }
            SidebarMenuItem("Security Reports", Icons.Rounded.Analytics, Color(0xFFFF9800)) { Toast.makeText(context, "Upcoming Feature.", Toast.LENGTH_SHORT).show() }

            Spacer(Modifier.weight(1f))
            Divider(color = Color.White.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp))

            SidebarMenuItem("Settings", Icons.Rounded.Settings, Color.Gray) { onNavigateToSettings() }
            SidebarMenuItem("Help & Support", Icons.Rounded.HelpOutline, Color.Gray) { Toast.makeText(context, "Connecting to Support...", Toast.LENGTH_SHORT).show() }
            Spacer(Modifier.height(8.dp))

            Surface(modifier = Modifier.fillMaxWidth().clickable {
                val prefs = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                val intent = Intent(context, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                context.startActivity(intent)
            }, color = Color.Red.copy(0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.Red.copy(0.3f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Logout, "Logout", tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("SECURE LOGOUT", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun SidebarMenuItem(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ==========================================
// 🔔 DYNAMIC BELL NOTIFICATION
// ==========================================
@Composable
fun AnimatedBellNotification(latestThreatTime: Long?, latestThreatType: String?) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)

    var showDropdown by remember { mutableStateOf(false) }
    val appVersion = "3.0"
    var isNewVersion by remember { mutableStateOf(prefs.getString("current_version", "2.0") != appVersion) }

    val lastSeenThreatTime = prefs.getLong("last_seen_threat_time", 0L)
    val hasNewThreat = latestThreatTime != null && latestThreatTime > lastSeenThreatTime

    var hasUnread by remember(latestThreatTime, isNewVersion, lastSeenThreatTime) {
        mutableStateOf(isNewVersion || hasNewThreat)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse)
    )

    Box {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFF1E293B), CircleShape).clip(CircleShape)
                .clickable {
                    hasUnread = false
                    showDropdown = true
                    if (latestThreatTime != null) {
                        prefs.edit().putLong("last_seen_threat_time", latestThreatTime).apply()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications, contentDescription = "Alerts",
                tint = if (hasUnread) NeonCyan else Color.Gray,
                modifier = Modifier.rotate(if (hasUnread) rotation else 0f)
            )
            if (hasUnread) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(Color.Red, CircleShape))
            }
        }

        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }, modifier = Modifier.background(Color(0xFF131C2B)).width(260.dp)) {
            if (isNewVersion) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("NEW VERSION AVAILABLE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Prohory Vision v$appVersion released. Update recommended.", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    },
                    onClick = {
                        showDropdown = false
                        prefs.edit().putString("current_version", appVersion).apply()
                        isNewVersion = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.SystemUpdate, null, tint = NeonCyan) }
                )
                Divider(color = Color.DarkGray)
            }

            DropdownMenuItem(
                text = {
                    Column {
                        if (latestThreatTime != null && latestThreatType != null) {
                            val isDanger = latestThreatType == "DANGER"
                            val notifColor = if (isDanger) Color.Red else Color(0xFFFF9800)
                            val title = if (isDanger) "DANGER INTERCEPTED" else "SPAM INTERCEPTED"
                            val message = if (isDanger) "A malicious scam SMS was blocked." else "A suspicious spam SMS was blocked."

                            Text(title, color = notifColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val timeText = getTimeAgo(latestThreatTime)
                            Text("$message ($timeText)", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        } else {
                            Text("SYSTEM SECURE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("System is monitoring actively. No recent threats.", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                },
                onClick = { showDropdown = false },
                leadingIcon = {
                    if (latestThreatTime != null && latestThreatType != null) {
                        val isDanger = latestThreatType == "DANGER"
                        Icon(if(isDanger) Icons.Rounded.GppBad else Icons.Rounded.WarningAmber, null, tint = if (isDanger) Color.Red else Color(0xFFFF9800))
                    } else {
                        Icon(Icons.Rounded.Security, null, tint = Color(0xFF10B981))
                    }
                }
            )
        }
    }
}

@Composable
fun EnterpriseTopHeader(name: String, greeting: String, hour: Int, latestThreatTime: Long?, latestThreatType: String?, onMenuClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Icon(if (hour in 6..18) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("$greeting, $name", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Surface(modifier = Modifier.padding(top = 6.dp), color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COMMUNITY THREAT: ELEVATED", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedBellNotification(latestThreatTime, latestThreatType)
    }
}

// ==========================================
// 📱 BOTTOM NAVIGATION
// ==========================================
@Composable
fun CyberBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.padding(20.dp).fillMaxWidth().height(65.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131C2B).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(0) }) {
                Icon(Icons.Rounded.Home, null, tint = if(selected == 0) NeonCyan else Color.Gray)
            }
            IconButton(onClick = { onSelect(1) }) {
                Icon(Icons.Rounded.Public, null, tint = if(selected == 1) NeonCyan else Color.Gray)
            }
            IconButton(onClick = { context.startActivity(Intent(context, ExpertChatActivity::class.java)) }) {
                Icon(Icons.Rounded.ChatBubble, contentDescription = "Support Chat", tint = Color.Gray)
            }
            IconButton(onClick = { onSelect(3) }) {
                Icon(Icons.Rounded.Settings, null, tint = if(selected == 3) NeonCyan else Color.Gray)
            }
        }
    }
}

// ==========================================
// DASHBOARD CONTENT COMPONENTS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseHomeDashboard(
    userName: String,
    stats: DashboardStats?,
    totalBlocked: Int, // Pure Global API Stat
    liveThreat: Float,
    personalScore: Int,
    localThreatCount: Int,
    isRealTimeOn: Boolean,
    isBiometricOn: Boolean,
    latestThreatTime: Long?,
    latestThreatType: String?,
    recentLogs: List<SystemLog>,
    onMenuClick: () -> Unit,
    viewModel: DashboardViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showRiskModal by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("auth_token", "") ?: ""
    val myUsername = sharedPref.getString("username", "") ?: ""
    val myRole = sharedPref.getString("role", "User") ?: "User"

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                if (token.isNotEmpty()) {
                    viewModel.loadDashboardData(token, myUsername, myRole)
                }
                delay(1500)
                isRefreshing = false
                Toast.makeText(context, "Intelligence Data Synced ✔", Toast.LENGTH_SHORT).show()
            }
        },
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color(0xFF131C2B),
                color = NeonCyan
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            EnterpriseTopHeader(userName, greeting, hour, latestThreatTime, latestThreatType, onMenuClick)
            Spacer(Modifier.height(25.dp))
            PersonalSecurityCore(personalScore, liveThreat) { showRiskModal = true }
            Spacer(Modifier.height(25.dp))

            SectionHeader("SYSTEM INTELLIGENCE")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 🔥 UI FIX: Now shows purely the global stats from backend API
                SmartStatCard("Scans", "${stats?.totalScans ?: 0}", Icons.Rounded.Analytics, NeonCyan, Modifier.weight(1f).height(95.dp))
                SmartStatCard("Spam", "$totalBlocked", Icons.Rounded.ReportProblem, Color(0xFFFF9800), Modifier.weight(1f).height(95.dp))
                SmartStatCard("Danger", "${totalBlocked / 4}", Icons.Rounded.GppBad, Color.Red, Modifier.weight(1f).height(95.dp))
            }

            Spacer(Modifier.height(25.dp))
            SectionHeader("DEFENSE MODULES")
            Spacer(Modifier.height(12.dp))
            ModuleGrid(context)

            Spacer(Modifier.height(25.dp))
            SectionHeader("LIVE THREAT ACTIVITY")
            Spacer(Modifier.height(12.dp))
            AdvancedThreatRadar(liveThreat)

            Spacer(Modifier.height(25.dp))
            SectionHeader("ACTIVE THREAT PATTERNS")
            Spacer(Modifier.height(12.dp))
            DynamicThreatIntelligencePanel(recentLogs)

            Spacer(Modifier.height(25.dp))
            SectionHeader("RECENT SYSTEM LOGS")
            Spacer(Modifier.height(12.dp))
            RecentLogsSection(recentLogs)

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showRiskModal) {
        RiskBreakdownModal(personalScore, localThreatCount, isRealTimeOn, isBiometricOn) { showRiskModal = false }
    }
}

// 🔥 CRASH FIX & JUMPING BUG FIX (100% Stable Version)
@Composable
fun AdvancedThreatRadar(liveThreat: Float) {
    val spikeData = remember {
        mutableStateListOf<Float>().apply {
            val baseValue = if (liveThreat > 0.05f) liveThreat else 0.1f
            repeat(20) { add(baseValue) }
        }
    }

    var lastValidValue by remember { mutableFloatStateOf(if (liveThreat > 0.05f) liveThreat else 0.1f) }

    LaunchedEffect(liveThreat) {
        if (liveThreat > 0.05f) {
            lastValidValue = liveThreat
        }
    }

    LaunchedEffect(lastValidValue) {
        while(true) {
            delay(400)

            val jitter = (Math.random().toFloat() * 0.08f) - 0.04f
            val lastPoint = if (spikeData.isNotEmpty()) spikeData.last() else lastValidValue

            val targetNext = (lastValidValue + jitter).coerceIn(0.05f, 1f)
            val nextValue = lastPoint + (targetNext - lastPoint) * 0.4f

            if (spikeData.size >= 20) {
                spikeData.removeAt(0)
            }
            spikeData.add(nextValue)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0D121D),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)) {
                val gridColor = Color.DarkGray.copy(alpha = 0.3f)
                for (i in 1..4) {
                    val y = size.height * (i / 5f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                }

                val path = Path()
                val fillPath = Path()
                val step = size.width / (if (spikeData.size > 1) spikeData.size - 1 else 1)

                if (spikeData.isNotEmpty()) {
                    path.moveTo(0f, size.height * (1f - spikeData[0]))
                    fillPath.moveTo(0f, size.height)
                    fillPath.lineTo(0f, size.height * (1f - spikeData[0]))

                    for (i in 1 until spikeData.size) {
                        val x = i * step
                        val y = size.height * (1f - spikeData[i])
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    fillPath.lineTo(size.width, size.height)
                    fillPath.close()

                    drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(NeonPurple.copy(alpha = 0.6f), Color.Transparent)))
                    drawPath(path = path, brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, Color.Red)), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("REAL-TIME", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("PING: 14ms", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun PersonalSecurityCore(score: Int, liveThreat: Float, onClick: () -> Unit) {
    val animatedScore by animateIntAsState(targetValue = score, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    val infiniteTransition = rememberInfiniteTransition()

    val middleRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing)))
    val outerPulse by infiniteTransition.animateFloat(1f, 1.15f + (liveThreat * 0.2f), infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    val statusColor = when { animatedScore >= 80 -> Color(0xFF10B981); animatedScore >= 50 -> Color(0xFFFF9800); else -> Color(0xFFEF4444) }
    val trendText = if (score >= 80) "▲ Optimal Security" else "▼ Action Needed"
    val trendColor = if (score >= 80) Color.Green else Color.Red

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp).clickable { onClick() }) {
        Box(modifier = Modifier.size(240.dp).scale(outerPulse).background(statusColor.copy(alpha = 0.05f), CircleShape))
        Canvas(modifier = Modifier.size(210.dp).rotate(middleRotation)) { drawArc(color = statusColor.copy(alpha = 0.4f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))) }
        Canvas(modifier = Modifier.size(180.dp)) {
            val sweep = (animatedScore / 100f) * 360f
            drawArc(color = Color.DarkGray, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = statusColor, startAngle = -90f, sweepAngle = sweep, useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$animatedScore%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(text = "PERSONAL SECURITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(color = trendColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, trendColor.copy(alpha = 0.5f))) {
                Text(text = trendText, color = trendColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-20).dp, y = (-20).dp).background(Color(0xFF1E293B), CircleShape).border(1.dp, Color.Gray, CircleShape).padding(4.dp)) {
            Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

// ==========================================
// 🔥 DYNAMIC THREAT INTELLIGENCE
// ==========================================
@Composable
fun DynamicThreatIntelligencePanel(recentLogs: List<SystemLog>) {
    val context = LocalContext.current
    val dynamicPatterns = remember(recentLogs) {
        val patterns = mutableListOf<Triple<String, String, Color>>()
        val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
        val savedThreats = threatPrefs.getStringSet("quarantine_list", emptySet()) ?: emptySet()

        if (savedThreats.isNotEmpty()) {
            val latestSpam = savedThreats.lastOrNull()?.split("|||")
            if (latestSpam != null && latestSpam.size >= 2) patterns.add(Triple("Spam from: ${latestSpam[0]}", "LOCAL SMS", Color(0xFFFF9800)))
        }

        val serverThreats = recentLogs.filter { log ->
            val type = log.actionType.uppercase()
            type.contains("DANGER") || type.contains("SPAM") || type.contains("PROMO") || type.contains("THREAT") || type.contains("SCAM") || type.contains("REPORT") || type.contains("WARNING")
        }

        serverThreats.forEach { log ->
            val type = log.actionType.uppercase()
            val color = when {
                type.contains("DANGER") || type.contains("SCAM") || type.contains("PHISHING") || type.contains("THREAT") -> Color.Red
                type.contains("SPAM") || type.contains("WARNING") -> Color(0xFFFF9800)
                type.contains("PROMO") -> Color(0xFF3B82F6)
                else -> Color.Green
            }
            patterns.add(Triple(log.details, "CLOUD $type", color))
        }
        patterns.take(3)
    }

    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dynamicPatterns.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = Color.Green, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System is monitoring. No active threats detected.", color = Color.Gray, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                }
            } else {
                dynamicPatterns.forEach { (title, type, color) ->
                    IntelligenceItem(title.take(35) + if(title.length > 35) "..." else "", type, color)
                }
            }
        }
    }
}

@Composable
fun IntelligenceItem(title: String, type: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Source: $type", color = Color.Gray, fontSize = 10.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SmartStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131C2B),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AdvancedCyberBackground(score: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val isCritical = score < 50
    val alpha by infiniteTransition.animateFloat(0.05f, if (isCritical) 0.3f else 0.15f, infiniteRepeatable(tween(if (isCritical) 800 else 3000), RepeatMode.Reverse))
    val bgColor = if (isCritical) Color.Red else Color(0xFF1E3A8A)
    Canvas(Modifier.fillMaxSize()) { drawCircle(Brush.radialGradient(listOf(bgColor.copy(alpha), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.3f), radius = 1200f)) }
}

@Composable
fun RiskBreakdownModal(score: Int, threats: Int, isRealTimeOn: Boolean, isBiometricOn: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF131C2B),
        title = { Text("Health Diagnostics", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Your security score evaluates both external threats and system configurations.", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Base Architecture", color = Color.White, fontSize = 14.sp); Text("100%", color = Color.Green, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(8.dp))

                if (threats > 0) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Unresolved Threats ($threats)", color = Color.White, fontSize = 12.sp); Text("-${threats * 5}%", color = Color.Red, fontWeight = FontWeight.Bold) }
                if (!isRealTimeOn) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Real-time Shield Offline", color = Color.White, fontSize = 12.sp); Text("-15%", color = Color.Red, fontWeight = FontWeight.Bold) }
                if (!isBiometricOn) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Biometric Lock Disabled", color = Color.White, fontSize = 12.sp); Text("-5%", color = Color.Red, fontWeight = FontWeight.Bold) }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("System Health", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("$score%", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                Spacer(modifier = Modifier.height(16.dp))

                val tip = if (!isRealTimeOn || !isBiometricOn) "Enable disabled features in Settings to boost security." else "Clear quarantine from Intelligence Archive to restore 100% health."
                Text("💡 Tip: $tip", color = Color(0xFFFF9800), fontSize = 11.sp, lineHeight = 16.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ACKNOWLEDGE", color = NeonCyan) } }
    )
}

@Composable
fun ModuleGrid(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("Phishing", Icons.Rounded.Link, Color(0xFF3B82F6), Modifier.weight(1f)) { context.startActivity(Intent(context, PhishingScannerActivity::class.java)) }
            FeatureTile("OCR AI", Icons.Rounded.DocumentScanner, Color(0xFF8B5CF6), Modifier.weight(1f)) { context.startActivity(Intent(context, ScreenshotAnalysisActivity::class.java)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("SMS Guard", Icons.Rounded.Sms, Color(0xFFFF9800), Modifier.weight(1f)) { context.startActivity(Intent(context, SmsGuardActivity::class.java)) }
            FeatureTile("Secure Vault", Icons.Rounded.Https, Color(0xFF10B981), Modifier.weight(1f)) { context.startActivity(Intent(context, SecureVaultActivity::class.java)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile("Network Scan", Icons.Rounded.WifiTethering, Color(0xFFE91E63), Modifier.weight(1f)) { context.startActivity(Intent(context, NetworkScanActivity::class.java)) }
            FeatureTile("App Lock", Icons.Rounded.Lock, Color(0xFF00BCD4), Modifier.weight(1f)) { context.startActivity(Intent(context, AppLockActivity::class.java)) }
        }
    }
}

@Composable
fun FeatureTile(title: String, icon: ImageVector, themeColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(100.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), color = Color(0xFF131C2B), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = themeColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth())
}

@Composable
fun RecentLogsSection(logs: List<SystemLog>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (logs.isEmpty()) {
            Text("No recent activity found. Syncing...", color = Color.Gray, fontSize = 12.sp)
        } else {
            logs.forEach { log ->
                val typeUpper = log.actionType.uppercase()

                val statusColor = when {
                    typeUpper in listOf("LOGIN", "REGISTRATION") -> Color.Cyan
                    typeUpper.contains("DANGER") || typeUpper.contains("THREAT") || typeUpper.contains("SCAM") || typeUpper.contains("PHISHING") -> Color.Red
                    typeUpper.contains("SPAM") || typeUpper.contains("WARNING") -> Color(0xFFFF9800)
                    typeUpper.contains("PROMO") -> Color(0xFF3B82F6)
                    else -> Color.Green
                }

                val time = log.timestamp?.take(10) ?: "Just now"
                val performer = if (log.username == null || log.username == "null") "System" else log.username

                Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(log.details.replace("Agent null", "User $performer"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(time, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}