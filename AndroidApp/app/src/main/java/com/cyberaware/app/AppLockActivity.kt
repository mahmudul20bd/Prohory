package com.cyberaware.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    var isLocked: Boolean
)

class AppLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                AppLockScreen(onBack = { finish() })
            }
        }
    }
}

// 🔥 Helper Function: Check if our Accessibility Service is ON
fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
    val expectedComponentName = ComponentName(context, service).flattenToString()
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    if (enabledServicesSetting == null) return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(expectedComponentName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔥 Real-time Permission State
    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context, AppMonitorService::class.java)) }

    // 🔥 Watch Lifecycle to detect when user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context, AppMonitorService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sharedPreferences = context.getSharedPreferences("CyberAwareAppLock", Context.MODE_PRIVATE)
    val lockedCount = installedApps.count { it.isLocked }

    // Background App Loading
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            // 🔥 Get ALL installed apps (requires QUERY_ALL_PACKAGES permission)
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val lockedPackages = sharedPreferences.getStringSet("locked_apps", emptySet()) ?: emptySet()

            val appList = mutableListOf<InstalledAppInfo>()
            for (appInfo in packages) {
                // 🔥 THE FIX: Keep USER installed apps, Remove SYSTEM apps
                // We also keep apps that the user explicitly installed but might be flagged as system updates
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val isLocked = lockedPackages.contains(appInfo.packageName)

                    // We don't need to load the icon here as it slows down compose heavily
                    appList.add(InstalledAppInfo(appName, appInfo.packageName, isLocked))
                }
            }
            appList.sortBy { it.appName.lowercase() } // Sort Alphabetically

            withContext(Dispatchers.Main) {
                installedApps = appList
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        VaultGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Security, contentDescription = "Vault", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VAULT // APP LOCK", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = Color(0xFF131C2B),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SYSTEM LOCKDOWN", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$lockedCount", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                            Text(" / ${installedApps.size}", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                        }
                        Text("Apps Secured", color = Color.Gray, fontSize = 12.sp)
                    }
                    VaultShieldAnimation(isActive = isServiceEnabled && lockedCount > 0)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App List Section
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 10.dp).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = Color(0xFF0B1221).copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("INSTALLED APPLICATIONS", color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                        Text(if (isLoading) "SCANNING..." else "READY", color = if (isLoading) NeonPurple else NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
                    } else if (installedApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No user apps found.", color = Color.Gray) }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(installedApps) { app ->
                                AppListItem(
                                    appInfo = app,
                                    onToggle = { isLocked ->
                                        if (isServiceEnabled) {
                                            val updatedList = installedApps.map { if (it.packageName == app.packageName) it.copy(isLocked = isLocked) else it }
                                            installedApps = updatedList
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val currentLocked = sharedPreferences.getStringSet("locked_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                                                if (isLocked) currentLocked.add(app.packageName) else currentLocked.remove(app.packageName)
                                                sharedPreferences.edit().putStringSet("locked_apps", currentLocked).apply()
                                            }
                                        } else {
                                            Toast.makeText(context, "Permission Required First!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 🔥 SMART PERMISSION OVERLAY
        if (!isServiceEnabled) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF050A14).copy(alpha = 0.9f)).clickable(enabled = false) {}, // Blocks background clicks
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF131C2B),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SYSTEM OVERRIDE REQUIRED", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "To protect your applications, Prohory requires background monitoring permission (Accessibility). Please activate 'Prohory Vault Monitor' in Settings.",
                            color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // 🔥 Deep Link to Accessibility Settings
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("AUTHORIZE ACCESS", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(appInfo: InstalledAppInfo, onToggle: (Boolean) -> Unit) {
    val borderColor = if (appInfo.isLocked) NeonCyan else Color.Transparent
    val bgColor = if (appInfo.isLocked) NeonCyan.copy(alpha = 0.05f) else Color(0xFF131C2B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(45.dp).background(Color(0xFF1E293B), CircleShape).border(1.dp, Color.DarkGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appInfo.appName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(appInfo.appName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(appInfo.packageName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                }
            }
            CyberSwitch(isChecked = appInfo.isLocked, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun CyberSwitch(isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackColor by animateColorAsState(targetValue = if (isChecked) NeonCyan.copy(alpha = 0.3f) else Color.DarkGray, label = "track")
    val thumbColor by animateColorAsState(targetValue = if (isChecked) NeonCyan else Color.Gray, label = "thumb")
    val thumbOffset by animateDpAsState(targetValue = if (isChecked) 24.dp else 0.dp, label = "offset")

    Box(
        modifier = Modifier
            .width(50.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(trackColor)
            .border(1.dp, if (isChecked) NeonCyan else Color.Gray, RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!isChecked) }.padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier.offset(x = thumbOffset).size(22.dp).background(thumbColor, CircleShape).border(1.dp, Color.White.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = if (isChecked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun VaultShieldAnimation(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        if (isActive) {
            Box(modifier = Modifier.size((60 * pulse).dp).border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape))
            Box(modifier = Modifier.size((40 * pulse).dp).border(1.dp, NeonCyan.copy(alpha = 0.8f), CircleShape))
        }
        Icon(Icons.Default.Shield, contentDescription = null, tint = if (isActive) NeonCyan else Color.Gray, modifier = Modifier.size(40.dp))
    }
}

@Composable
fun VaultGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}