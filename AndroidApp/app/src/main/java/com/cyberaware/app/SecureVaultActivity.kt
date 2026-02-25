package com.cyberaware.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.sqrt

// ==========================================
// MILITARY-GRADE CRYPTO MANAGER (Optimized for OOM & Restore)
// ==========================================
object VaultCryptoManager {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val KEY_ALIAS = "ProhoryVaultMasterKey"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        val keySpec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(BLOCK_MODE).setEncryptionPaddings(PADDING).setKeySize(256).build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    fun encryptFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    outputStream.write(iv)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedBytes = cipher.update(buffer, 0, bytesRead)
                        if (encryptedBytes != null) outputStream.write(encryptedBytes)
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) outputStream.write(finalBytes)
                }
            }
            true
        } catch (e: Exception) { false }
    }

    // 🔥 FIX 1: OOM CRASH FIX (Streaming Decryption directly to Bitmap)
    fun decryptToBitmapSafe(file: File): Bitmap? {
        return try {
            FileInputStream(file).use { fis ->
                val iv = ByteArray(12)
                fis.read(iv)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                CipherInputStream(fis, cipher).use { cis ->
                    BitmapFactory.decodeStream(cis)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 🔥 FIX 2: RESTORE TO GALLERY LOGIC
    fun restoreArtifact(context: Context, file: File): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Prohory_Restored_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProhoryRestored")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    FileInputStream(file).use { fis ->
                        val iv = ByteArray(12)
                        fis.read(iv)
                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                        CipherInputStream(fis, cipher).use { cis ->
                            cis.copyTo(os)
                        }
                    }
                }
                file.delete() // Delete encrypted file after restore
                true
            } else false
        } catch (e: Exception) { false }
    }
}

// SMART URI RESOLVER
fun resolveRealMediaStoreUri(uri: Uri): Uri {
    try {
        val path = uri.path ?: ""
        if (path.contains("picker")) {
            val id = uri.lastPathSegment?.toLongOrNull()
            if (id != null) return android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        } else if (path.contains("document/image")) {
            val id = uri.lastPathSegment?.split(":")?.lastOrNull()?.toLongOrNull()
            if (id != null) return android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    } catch (e: Exception) { e.printStackTrace() }
    return uri
}

class SecureVaultActivity : FragmentActivity() {
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

        // 🔥 MEMORY HARDENING: Block Screenshots & Recents Preview
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            CyberAwareTheme {
                SecureVaultScreen(onBack = { finish() }, activity = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureVaultScreen(onBack: () -> Unit, activity: FragmentActivity) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vaultState by remember { mutableIntStateOf(0) }
    var encryptedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingText by remember { mutableStateOf("") }

    // Active View States
    var selectedDecryptedFile by remember { mutableStateOf<File?>(null) }
    var activeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val vaultDir = File(context.filesDir, "ProhoryVault")
    if (!vaultDir.exists()) vaultDir.mkdirs()

    fun loadVaultFiles() {
        encryptedFiles = vaultDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // 🔥 PANIC MODE: Shake to Lock
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val shakeListener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
            private val SHAKE_THRESHOLD = 800

            override fun onSensorChanged(event: SensorEvent) {
                if (vaultState != 1) return // Only monitor when unlocked
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 100) {
                    val diffTime = curTime - lastUpdate
                    lastUpdate = curTime
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                    if (speed > SHAKE_THRESHOLD) {
                        // SHAKE DETECTED -> INSTANT LOCK
                        vaultState = 0
                        selectedDecryptedFile = null
                        activeBitmap = null
                        Toast.makeText(context, "PANIC MODE: Vault Locked!", Toast.LENGTH_SHORT).show()
                    }
                    lastX = x; lastY = y; lastZ = z
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(shakeListener) }
    }

    fun authenticateToUnlock() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    vaultState = 1
                    loadVaultFiles()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(context, "Auth Failed: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Hardware Keystore Access")
            .setSubtitle("Military-grade verification required")
            .setDescription("Use your biometric credential to decrypt the sandbox.")
            .setNegativeButtonText("CANCEL")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) Toast.makeText(context, "Secured & Trashed! 🗑️", Toast.LENGTH_SHORT).show()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isProcessing = true
            processingText = "ENCRYPTING TO SANDBOX..."
            coroutineScope.launch(Dispatchers.IO) {
                val destFile = File(vaultDir, "ART_${System.currentTimeMillis()}.prohory")
                val success = VaultCryptoManager.encryptFile(context, uri, destFile)
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    if (success) {
                        loadVaultFiles()
                        val realUri = resolveRealMediaStoreUri(uri)
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(realUri))
                                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                            } else {
                                context.contentResolver.delete(realUri, null, null)
                            }
                        } catch (e: Exception) { }
                    } else Toast.makeText(context, "Encryption Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) { authenticateToUnlock() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        AdvancedVaultBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.VpnKey, contentDescription = "Vault", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("INTELLIGENCE VAULT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            if (vaultState == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AdvancedFingerprintScanner { authenticateToUnlock() }
                        Spacer(modifier = Modifier.height(30.dp))
                        Text("VAULT IS LOCKED", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("System hardened. Memory wiped.", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

                        // 🔥 ADVANCED INTELLIGENCE PANEL
                        val totalSizeKb = encryptedFiles.sumOf { it.length() } / 1024
                        val sizeDisplay = if (totalSizeKb > 1024) "${totalSizeKb / 1024} MB" else "$totalSizeKb KB"

                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Memory, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ANDROID KEYSTORE ACTIVE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("AES-GCM", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.DarkGray)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column { Text("Secured Artifacts", color = Color.Gray, fontSize = 10.sp); Text("${encryptedFiles.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                                    Column(horizontalAlignment = Alignment.End) { Text("Sandbox Size", color = Color.Gray, fontSize = 10.sp); Text(sizeDisplay, color = NeonPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        if (isProcessing) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(processingText, color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (encryptedFiles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("VAULT IS EMPTY.\nNO ARTIFACTS FOUND.", color = Color.DarkGray, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
                            ) {
                                items(encryptedFiles) { file ->
                                    AdvancedEncryptedItem(file = file) {
                                        isProcessing = true
                                        processingText = "DECRYPTING STREAM..."
                                        coroutineScope.launch(Dispatchers.IO) {
                                            delay(300)
                                            val bmp = VaultCryptoManager.decryptToBitmapSafe(file)
                                            withContext(Dispatchers.Main) {
                                                if (bmp != null) {
                                                    activeBitmap = bmp
                                                    selectedDecryptedFile = file
                                                } else Toast.makeText(context, "Decryption Failed!", Toast.LENGTH_SHORT).show()
                                                isProcessing = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        containerColor = NeonPurple, contentColor = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Add Artifact") }
                }
            }
        }

        // 🔥 FULL SCREEN SECURE VIEWER WITH RESTORE/DELETE ACTIONS
        if (activeBitmap != null && selectedDecryptedFile != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFA000000)).clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                // Image
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = activeBitmap!!.asImageBitmap(),
                        contentDescription = "Decrypted",
                        modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Close Button Top Right
                IconButton(
                    onClick = { activeBitmap = null; selectedDecryptedFile = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).background(Color(0xFF1E293B), CircleShape)
                ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }

                // Action Bar Bottom
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    color = Color(0xFF131C2B), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        // RESTORE BUTTON
                        Button(
                            onClick = {
                                isProcessing = true
                                processingText = "RESTORING TO GALLERY..."
                                val fileToRestore = selectedDecryptedFile!!
                                activeBitmap = null; selectedDecryptedFile = null
                                coroutineScope.launch(Dispatchers.IO) {
                                    val restored = VaultCryptoManager.restoreArtifact(context, fileToRestore)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        if (restored) {
                                            Toast.makeText(context, "Restored to Pictures/ProhoryRestored", Toast.LENGTH_LONG).show()
                                            loadVaultFiles()
                                        } else Toast.makeText(context, "Restore Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, NeonCyan), shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("RESTORE", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

                        // PERMANENT DELETE BUTTON
                        Button(
                            onClick = {
                                val fileToDelete = selectedDecryptedFile!!
                                activeBitmap = null; selectedDecryptedFile = null
                                fileToDelete.delete()
                                loadVaultFiles()
                                Toast.makeText(context, "Artifact Erased Permanently.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color.Red), shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UI COMPONENTS
// ==========================================
@Composable
fun AdvancedEncryptedItem(file: File, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val borderAlpha by infiniteTransition.animateFloat(0.2f, 0.8f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse))

    Surface(
        modifier = Modifier.aspectRatio(1f).clickable { onClick() },
        color = Color(0xFF0F172A), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, NeonCyan.copy(alpha = borderAlpha))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(10.dp))
            Text("SECURED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${file.length() / 1024} KB", color = NeonPurple, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AdvancedFingerprintScanner(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(0f, 120f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart))
    val pulse by infiniteTransition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Box(modifier = Modifier.size(150.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size((120 * pulse).dp).border(1.dp, NeonPurple.copy(alpha = 0.3f), CircleShape))
        Box(modifier = Modifier.size((140 * pulse).dp).border(1.dp, NeonCyan.copy(alpha = 0.1f), CircleShape))

        Box(modifier = Modifier.size(120.dp).background(Color(0xFF0B1221), CircleShape).border(2.dp, NeonCyan, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(70.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineY = scanLineY.dp.toPx()
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, NeonCyan.copy(alpha = 0.6f)), startY = lineY - 40f, endY = lineY), topLeft = Offset(0f, lineY - 40f), size = Size(size.width, 40f))
                drawLine(color = NeonCyan, start = Offset(0f, lineY), end = Offset(size.width, lineY), strokeWidth = 4f)
            }
        }
    }
}

@Composable
fun AdvancedVaultBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetAnim by infiniteTransition.animateFloat(0f, 50f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart))
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 120f
        val cols = (size.width / gridSize).toInt() + 1
        val rows = (size.height / gridSize).toInt() + 1
        for (i in 0..cols) { val x = (i * gridSize) - offsetAnim; drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { val y = (i * gridSize) - offsetAnim; drawLine(color = Color.White.copy(alpha = 0.03f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f) }
    }
}