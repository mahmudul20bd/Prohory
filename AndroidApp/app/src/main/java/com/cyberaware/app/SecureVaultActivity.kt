package com.cyberaware.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.fragment.app.FragmentActivity // 🔥 এটিই আসল ফিক্স
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// ==========================================
// MILITARY-GRADE AES-GCM CRYPTO MANAGER
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
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .build()
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
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun decryptFileToMemory(file: File): ByteArray? {
        return try {
            FileInputStream(file).use { inputStream ->
                val iv = ByteArray(12)
                inputStream.read(iv)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

                val encryptedBytes = inputStream.readBytes()
                cipher.doFinal(encryptedBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ==========================================
// SECURE VAULT ACTIVITY (Using FragmentActivity)
// ==========================================
class SecureVaultActivity : FragmentActivity() { // 🔥 AppCompatActivity এর বদলে FragmentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                SecureVaultScreen(
                    onBack = { finish() },
                    activity = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureVaultScreen(onBack: () -> Unit, activity: FragmentActivity) { // 🔥 এখানেও আপডেট করা হয়েছে
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vaultState by remember { mutableIntStateOf(0) }
    var encryptedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedDecryptedImage by remember { mutableStateOf<ByteArray?>(null) }

    val vaultDir = File(context.filesDir, "ProhoryVault")
    if (!vaultDir.exists()) vaultDir.mkdirs()

    fun loadVaultFiles() {
        encryptedFiles = vaultDir.listFiles()?.toList() ?: emptyList()
    }

    // Biometric Authentication Function
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
            .setTitle("Vault Authorization")
            .setSubtitle("Prohory Protocol Verification")
            .setDescription("Use your biometric credential to access the secure encrypted vault.")
            .setNegativeButtonText("CANCEL")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isProcessing = true
                coroutineScope.launch(Dispatchers.IO) {
                    val timestamp = System.currentTimeMillis()
                    val destFile = File(vaultDir, "ENCRYPTED_ARTIFACT_$timestamp.prohory")

                    val success = VaultCryptoManager.encryptFile(context, uri, destFile)

                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        if (success) {
                            Toast.makeText(context, "Artifact Encrypted & Secured!", Toast.LENGTH_SHORT).show()
                            loadVaultFiles()
                        } else {
                            Toast.makeText(context, "Encryption Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        authenticateToUnlock()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        VaultCyberBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.VpnKey, contentDescription = "Vault", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ZERO-KNOWLEDGE VAULT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            if (vaultState == 0) {
                // LOCKED STATE UI
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FingerprintScannerAnimation { authenticateToUnlock() }
                        Spacer(modifier = Modifier.height(30.dp))
                        Text("VAULT IS LOCKED", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Awaiting Biometric Override...", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                // UNLOCKED STATE UI
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            color = NeonCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("AES-256 ENCRYPTION ACTIVE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${encryptedFiles.size} Artifacts Secured", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isProcessing) {
                            LinearProgressIndicator(color = NeonPurple, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (encryptedFiles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("VAULT IS EMPTY. NO ARTIFACTS FOUND.", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
                            ) {
                                items(encryptedFiles) { file ->
                                    EncryptedFileItem(file = file) {
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val decryptedBytes = VaultCryptoManager.decryptFileToMemory(file)
                                            withContext(Dispatchers.Main) {
                                                selectedDecryptedImage = decryptedBytes
                                                isProcessing = false
                                                if (decryptedBytes == null) {
                                                    Toast.makeText(context, "Decryption Failed! Corrupted artifact.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        containerColor = NeonPurple,
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Artifact")
                    }
                }
            }
        }

        if (selectedDecryptedImage != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                val bitmap = BitmapFactory.decodeByteArray(selectedDecryptedImage, 0, selectedDecryptedImage!!.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Decrypted Artifact",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = { selectedDecryptedImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    "ZERO-TRACE DECRYPTION\nPROHORY SECURE VIEWER",
                    color = Color.Green.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                )
            }
        }
    }
}

@Composable
fun EncryptedFileItem(file: File, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.aspectRatio(1f).clickable { onClick() },
        color = Color(0xFF131C2B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.DarkGray)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("SECURED", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${file.length() / 1024} KB", color = NeonPurple, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun FingerprintScannerAnimation(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier.size(150.dp).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size((100 * pulse).dp).border(2.dp, NeonCyan.copy(alpha = 0.4f), CircleShape))
        Box(modifier = Modifier.size(100.dp).background(NeonCyan.copy(alpha = 0.1f), CircleShape).border(2.dp, NeonCyan, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(50.dp))
        }
    }
}

@Composable
fun VaultCyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 100f
        val cols = (size.width / gridSize).toInt()
        val rows = (size.height / gridSize).toInt()
        for (i in 0..cols) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 2f) }
        for (i in 0..rows) { drawLine(color = Color.White.copy(alpha = 0.02f), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 2f) }
    }
}