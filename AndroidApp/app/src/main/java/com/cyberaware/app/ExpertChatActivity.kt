package com.cyberaware.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// 🔥 ADVANCED DATA MODELS
enum class MessageType { USER, EXPERT, SYSTEM_INFO }
data class ChatMessage(val id: String, val text: String, val type: MessageType, val timestamp: String, val fileUrl: String? = null)

data class BackendChatMessage(
    val senderUsername: String,
    val receiverUsername: String,
    val content: String,
    val fileUrl: String? = null,
    val timestamp: String? = null
)

// 🔥 NEW: Data class for Expert Profile
data class ExpertProfile(
    val username: String,
    val specialization: String?,
    val aboutMe: String?,
    val averageRating: Double?,
    val ratingCount: Int?,
    val profileImage: String?,
    val coverImage: String?
)

enum class SupportState { CONNECTING, IN_QUEUE, EXPERT_JOINED, CLOSED }

class ExpertChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("screenshot_protect", false)) {
            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            CyberAwareTheme {
                LiveSupportChatScreen(onBack = { finish() })
            }
        }
    }
}

fun saveSessionMessages(prefs: SharedPreferences, sessionId: String, msgs: List<ChatMessage>) {
    val json = Gson().toJson(msgs)
    prefs.edit().putString("chat_msgs_$sessionId", json).apply()
}

fun loadSessionMessages(prefs: SharedPreferences, sessionId: String): List<ChatMessage> {
    val json = prefs.getString("chat_msgs_$sessionId", null) ?: return emptyList()
    val type = object : TypeToken<List<ChatMessage>>() {}.type
    return try { Gson().fromJson(json, type) } catch(e: Exception) { emptyList() }
}

@Composable
fun LiveSupportChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val prefs = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("auth_token", "") ?: ""

    var allSessions by remember {
        mutableStateOf(prefs.getStringSet("all_chat_sessions", setOf())?.toList()?.reversed() ?: emptyList())
    }

    var sessionUserId by remember {
        mutableStateOf(
            prefs.getString("active_chat_session", null) ?: run {
                val newId = "CASE-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
                prefs.edit().putString("active_chat_session", newId).apply()

                val currentSessions = prefs.getStringSet("all_chat_sessions", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                currentSessions.add(newId)
                prefs.edit().putStringSet("all_chat_sessions", currentSessions).apply()
                allSessions = currentSessions.toList().reversed()

                newId
            }
        )
    }

    var currentState by remember { mutableStateOf(SupportState.CONNECTING) }
    var currentExpertName by remember { mutableStateOf("Support Team") }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var stompClient by remember { mutableStateOf<StompClient?>(null) }
    val compositeDisposable = remember { CompositeDisposable() }

    var messageText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // 🔥 NEW: States for Expert Profile Modal
    var expertProfile by remember { mutableStateOf<ExpertProfile?>(null) }
    var showExpertProfileModal by remember { mutableStateOf(false) }

    fun getTime() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 🔥 NEW: Fetch Expert Profile Data when an Expert Joins
    LaunchedEffect(currentExpertName, currentState) {
        if (currentState == SupportState.EXPERT_JOINED && currentExpertName != "Support Team") {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val requestBuilder = Request.Builder().url("https://cyber-aware.onrender.com/api/users")
                    if (token.isNotEmpty()) {
                        requestBuilder.addHeader("Authorization", if (token.startsWith("Bearer")) token else "Bearer $token")
                    }
                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.isSuccessful) {
                        val respString = response.body?.string()
                        val usersArray = JSONArray(respString ?: "[]")
                        for (i in 0 until usersArray.length()) {
                            val userObj = usersArray.getJSONObject(i)
                            if (userObj.optString("username") == currentExpertName) {
                                val profile = ExpertProfile(
                                    username = userObj.optString("username"),
                                    specialization = userObj.optString("specialization", "Security Expert"),
                                    aboutMe = userObj.optString("aboutMe", "This expert is a verified member of the Prohory Cyber Security unit."),
                                    averageRating = userObj.optDouble("averageRating", 0.0),
                                    ratingCount = userObj.optInt("ratingCount", 0),
                                    profileImage = if (userObj.isNull("profileImage")) null else userObj.getString("profileImage"),
                                    coverImage = if (userObj.isNull("coverImage")) null else userObj.getString("coverImage")
                                )
                                withContext(Dispatchers.Main) { expertProfile = profile }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ExpertProfile", "Failed to fetch expert profile", e)
                }
            }
        } else {
            expertProfile = null
        }
    }

    fun connectWebSocket() {
        coroutineScope.launch {
            try {
                val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://cyber-aware.onrender.com/ws/websocket")
                client.connect()
                stompClient = client

                delay(500)
                if (currentState == SupportState.CONNECTING) {
                    currentState = SupportState.IN_QUEUE
                    messages.add(ChatMessage(UUID.randomUUID().toString(), "Connected. How can we help you today?", MessageType.SYSTEM_INFO, getTime()))
                    saveSessionMessages(prefs, sessionUserId, messages)
                }

                val topicSubscription = client.topic("/topic/messages")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ topicMessage ->
                        val payload = Gson().fromJson(topicMessage.payload, BackendChatMessage::class.java)

                        if (payload.receiverUsername == sessionUserId || payload.receiverUsername == "All") {
                            if (payload.senderUsername != sessionUserId) {

                                if (payload.content == "[SYS:CLOSED]") {
                                    if (currentState != SupportState.CLOSED) {
                                        currentState = SupportState.CLOSED
                                        messages.add(ChatMessage(UUID.randomUUID().toString(), "🔒 The support agent has securely closed this session.", MessageType.SYSTEM_INFO, getTime()))
                                        saveSessionMessages(prefs, sessionUserId, messages)
                                    }
                                    return@subscribe
                                }

                                if (currentState != SupportState.EXPERT_JOINED && currentState != SupportState.CLOSED) {
                                    currentState = SupportState.EXPERT_JOINED
                                    currentExpertName = payload.senderUsername
                                    messages.add(ChatMessage(UUID.randomUUID().toString(), "$currentExpertName has joined the chat.", MessageType.SYSTEM_INFO, getTime()))
                                }

                                val lastMsg = messages.lastOrNull { it.type == MessageType.EXPERT }
                                if (lastMsg == null || lastMsg.text != payload.content) {
                                    messages.add(ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        text = payload.content ?: "",
                                        type = MessageType.EXPERT,
                                        timestamp = getTime(),
                                        fileUrl = payload.fileUrl
                                    ))
                                    saveSessionMessages(prefs, sessionUserId, messages)
                                }
                            }
                        }
                    }, { error ->
                        Log.e("ChatError", "Error receiving message", error)
                    })

                compositeDisposable.add(topicSubscription)
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(sessionUserId, refreshTrigger) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    messages.clear()
                    currentState = SupportState.CONNECTING
                }

                val client = OkHttpClient()
                val requestBuilder = Request.Builder().url("https://cyber-aware.onrender.com/api/chat/history/$sessionUserId")

                if (token.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", if(token.startsWith("Bearer")) token else "Bearer $token")
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val respString = response.body?.string()
                    val historyArray = JSONArray(respString ?: "[]")

                    val loadedMessages = mutableListOf<ChatMessage>()
                    var isClosed = false
                    var expertName = "Support Team"

                    for (i in 0 until historyArray.length()) {
                        val msgObj = historyArray.getJSONObject(i)
                        val content = msgObj.optString("content", "")
                        val sender = msgObj.optString("senderUsername", "")
                        val timestamp = msgObj.optString("timestamp", getTime())
                        val fileUrl = if (msgObj.isNull("fileUrl")) null else msgObj.getString("fileUrl")

                        if (content == "[SYS:CLOSED]") {
                            isClosed = true
                            loadedMessages.add(ChatMessage(UUID.randomUUID().toString(), "🔒 The session was securely closed.", MessageType.SYSTEM_INFO, timestamp))
                            continue
                        }

                        if (sender == "SYSTEM") continue

                        if (sender == sessionUserId) {
                            loadedMessages.add(ChatMessage(UUID.randomUUID().toString(), content, MessageType.USER, timestamp, fileUrl))
                        } else {
                            expertName = sender
                            loadedMessages.add(ChatMessage(UUID.randomUUID().toString(), content, MessageType.EXPERT, timestamp, fileUrl))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        messages.clear()
                        if (loadedMessages.isEmpty()) {
                            connectWebSocket()
                        } else {
                            messages.addAll(loadedMessages)
                            if (expertName != "Support Team") currentExpertName = expertName

                            if (isClosed) {
                                currentState = SupportState.CLOSED
                            } else {
                                if (loadedMessages.any { it.type == MessageType.EXPERT }) {
                                    currentState = SupportState.EXPERT_JOINED
                                } else {
                                    currentState = SupportState.IN_QUEUE
                                }
                                connectWebSocket()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        messages.clear()
                        connectWebSocket()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages.clear()
                    connectWebSocket()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            compositeDisposable.dispose()
            stompClient?.disconnect()
        }
    }

    fun sendMessage(textToSend: String, fileUrl: String? = null, isHidden: Boolean = false) {
        if (textToSend.isBlank() && fileUrl == null) return

        if (stompClient?.isConnected == true) {
            if (!isHidden) {
                messages.add(ChatMessage(UUID.randomUUID().toString(), textToSend, MessageType.USER, getTime(), fileUrl))
                saveSessionMessages(prefs, sessionUserId, messages)
            }

            val receiver = if (currentState == SupportState.EXPERT_JOINED) currentExpertName else "Support Pool"

            val msgObj = BackendChatMessage(
                senderUsername = sessionUserId,
                receiverUsername = receiver,
                content = textToSend.ifEmpty { "Attached Evidence" },
                fileUrl = fileUrl
            )

            stompClient!!.send("/app/chat.send", Gson().toJson(msgObj))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ Log.d("Chat", "Sent successfully") }, { Log.e("Chat", "Send failed") })

            if (!isHidden) messageText = ""
        } else {
            if (!isHidden) Toast.makeText(context, "Not connected to server", Toast.LENGTH_SHORT).show()
        }
    }

    fun startNewSession() {
        stompClient?.disconnect()
        compositeDisposable.clear()
        messages.clear()

        val newId = "CASE-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        prefs.edit().putString("active_chat_session", newId).apply()

        val currentSessions = prefs.getStringSet("all_chat_sessions", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSessions.add(newId)
        prefs.edit().putStringSet("all_chat_sessions", currentSessions).apply()
        allSessions = currentSessions.toList().reversed()

        currentState = SupportState.CONNECTING
        currentExpertName = "Support Team"
        expertProfile = null
        sessionUserId = newId
    }

    fun uploadImageToBackend(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isUploading = true }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val maxDim = 1080f
                val scale = minOf(maxDim / originalBitmap.width, maxDim / originalBitmap.height, 1f)
                val width = Math.round(scale * originalBitmap.width)
                val height = Math.round(scale * originalBitmap.height)
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                val file = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.close()

                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull())).build()

                val scanRequest = Request.Builder()
                    .url("https://cyber-aware.onrender.com/api/malware/scan")
                    .addHeader("Authorization", if(token.startsWith("Bearer")) token else "Bearer $token")
                    .post(requestBody).build()

                val scanResponse = client.newCall(scanRequest).execute()
                if (scanResponse.isSuccessful) {
                    val scanJson = JSONObject(scanResponse.body?.string() ?: "{}")
                    val status = scanJson.optString("status", "SAFE")
                    val threatType = scanJson.optString("threatType", "Unknown Threat")

                    if (status == "DANGER") {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "⚠️ Upload Blocked: Malicious signature detected!", Toast.LENGTH_LONG).show()
                            sendMessage("🚨 [SYSTEM ALERT] User attempted to send a malicious file (${file.name}). Blocked by Prohory Engine. (Threat: $threatType)", null)
                            isUploading = false
                        }
                        return@launch
                    }
                }

                val uploadRequest = Request.Builder().url("https://cyber-aware.onrender.com/api/chat/upload").addHeader("Authorization", if(token.startsWith("Bearer")) token else "Bearer $token").post(requestBody).build()
                val response = client.newCall(uploadRequest).execute()

                if (response.isSuccessful) {
                    val respString = response.body?.string()
                    val json = JSONObject(respString ?: "{}")
                    withContext(Dispatchers.Main) { sendMessage("Attached Evidence", json.getString("fileUrl")) }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Upload Error", Toast.LENGTH_SHORT).show() }
            } finally {
                withContext(Dispatchers.Main) { isUploading = false }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uploadImageToBackend(uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070B14))) {
        Column(modifier = Modifier.fillMaxSize()) {

            ModernChatHeader(
                currentState = currentState,
                expertName = currentExpertName,
                sessionId = sessionUserId.split("-").getOrElse(1) { sessionUserId },
                expertProfile = expertProfile,
                onBack = onBack,
                onMenuAction = { action ->
                    when (action) {
                        "history" -> showHistoryScreen = true
                        "sync" -> {
                            refreshTrigger++
                            Toast.makeText(context, "Syncing with database...", Toast.LENGTH_SHORT).show()
                        }
                        "export" -> Toast.makeText(context, "Transcript exported securely.", Toast.LENGTH_SHORT).show()
                        "clear" -> {
                            messages.clear()
                            saveSessionMessages(prefs, sessionUserId, messages)
                            Toast.makeText(context, "Local chat cleared.", Toast.LENGTH_SHORT).show()
                        }
                        "end" -> {
                            sendMessage("[SYS:CLOSED]", null, isHidden = true)
                            messages.add(ChatMessage(UUID.randomUUID().toString(), "🔒 You securely ended the session.", MessageType.SYSTEM_INFO, getTime()))
                            saveSessionMessages(prefs, sessionUserId, messages)
                            currentState = SupportState.CLOSED
                        }
                    }
                },
                onProfileClick = { if(expertProfile != null) showExpertProfileModal = true }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg ->
                    if (msg.text.startsWith("🚨 [SYSTEM ALERT]")) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(color = Color(0xFF3B0711), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.Red)) {
                                Text(msg.text, color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        when (msg.type) {
                            MessageType.USER -> ModernUserBubble(msg)
                            MessageType.EXPERT -> ModernExpertBubble(msg, currentExpertName)
                            MessageType.SYSTEM_INFO -> ModernSystemBubble(msg.text)
                        }
                    }
                }

                if (currentState == SupportState.IN_QUEUE && messages.count { it.type == MessageType.USER } == 0) {
                    item { SmartActionChips(onClick = { suggestion -> messageText = suggestion }) }
                }
            }

            if (currentState == SupportState.CLOSED) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { startNewSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.fillMaxWidth().height(55.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START NEW CASE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            } else {
                ModernInputArea(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    isInputEnabled = currentState != SupportState.CONNECTING,
                    isUploading = isUploading,
                    onAttachClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSendClick = { sendMessage(messageText) }
                )
            }
        }

        // 🔥 EXPERT PROFILE DIALOG
        if (showExpertProfileModal && expertProfile != null) {
            ExpertProfileDialog(profile = expertProfile!!) {
                showExpertProfileModal = false
            }
        }

        AnimatedVisibility(
            visible = showHistoryScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            ChatHistoryScreen(
                sessions = allSessions,
                activeSession = sessionUserId,
                onClose = { showHistoryScreen = false },
                onSessionSelect = { selectedId ->
                    sessionUserId = selectedId
                    prefs.edit().putString("active_chat_session", selectedId).apply()
                    showHistoryScreen = false
                }
            )
        }
    }
}

// ==========================================
// 🔥 EXPERT PROFILE POPUP UI
// ==========================================
@Composable
fun ExpertProfileDialog(profile: ExpertProfile, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0A0F1C),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column {
                    // Cover Image & Avatar
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        if (profile.coverImage != null) {
                            AsyncImage(model = profile.coverImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f)))))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), CircleShape).size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }

                        // Avatar overlapping bottom
                        Box(modifier = Modifier.align(Alignment.BottomStart).offset(x = 24.dp, y = 30.dp)) {
                            if (profile.profileImage != null) {
                                AsyncImage(model = profile.profileImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(70.dp).clip(CircleShape).border(3.dp, Color(0xFF0A0F1C), CircleShape))
                            } else {
                                Box(modifier = Modifier.size(70.dp).background(Color(0xFF1E293B), CircleShape).border(3.dp, Color(0xFF0A0F1C), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Security, null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp)) // Space for avatar overlap

                    // Info
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text((profile.specialization ?: "CYBER SECURITY EXPERT").uppercase(), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            // Rating Badge
                            Surface(color = Color(0xFFFFCA00).copy(alpha = 0.1f), border = BorderStroke(1.dp, Color(0xFFFFCA00).copy(alpha=0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFCA00), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(String.format("%.1f", profile.averageRating ?: 0.0), color = Color(0xFFFFCA00), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("(${profile.ratingCount ?: 0})", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verified Prohory Agent", color = Color.LightGray, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha=0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("ABOUT EXPERT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(profile.aboutMe ?: "This expert is a verified member of the Prohory Cyber Security unit, dedicated to protecting users from digital threats and scams.", color = Color(0xFF94A3B8), fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 🔥 CHAT HISTORY UI COMPONENT
// ==========================================
@Composable
fun ChatHistoryScreen(sessions: List<String>, activeSession: String, onClose: () -> Unit, onSessionSelect: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070B14)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past sessions found.", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sessions) { sessionId ->
                        val isActive = sessionId == activeSession
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSessionSelect(sessionId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if(isActive) Color(0xFF1E293B) else Color.Transparent,
                            border = BorderStroke(1.dp, if(isActive) NeonCyan else Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(if(isActive) NeonCyan.copy(alpha=0.2f) else Color(0xFF1E293B), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = if(isActive) NeonCyan else Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Case ID: ${sessionId.split("-").getOrElse(1){sessionId}}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(if(isActive) "Currently Active" else "Archived Session", color = if(isActive) NeonCyan else Color.Gray, fontSize = 12.sp)
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 🔥 MODERN CHAT COMPONENTS
// ==========================================

@Composable
fun ModernChatHeader(
    currentState: SupportState,
    expertName: String,
    sessionId: String,
    expertProfile: ExpertProfile?,
    onBack: () -> Unit,
    onMenuAction: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(color = Color(0xFF131C2B), modifier = Modifier.fillMaxWidth(), shadowElevation = 10.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
            }

            // 🔥 Clickable Profile Section
            Row(
                modifier = Modifier.clickable(enabled = currentState == SupportState.EXPERT_JOINED && expertProfile != null) { onProfileClick() }.padding(vertical = 4.dp, horizontal = 4.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(modifier = Modifier.size(42.dp).background(Color(0xFF1E293B), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                        if (expertProfile?.profileImage != null) {
                            AsyncImage(model = expertProfile.profileImage, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(if(currentState == SupportState.EXPERT_JOINED) Icons.Rounded.SupportAgent else Icons.Rounded.Support, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    val dotColor = if(currentState == SupportState.EXPERT_JOINED) Color(0xFF10B981) else if(currentState == SupportState.CLOSED) Color.Gray else Color(0xFFFF9800)
                    Box(modifier = Modifier.size(12.dp).background(dotColor, CircleShape).border(2.dp, Color(0xFF131C2B), CircleShape))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(if(currentState == SupportState.EXPERT_JOINED) expertName else "Live Support", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    if (currentState == SupportState.EXPERT_JOINED && expertProfile != null) {
                        Text("${expertProfile.specialization ?: "Security Expert"} • View Profile", color = NeonCyan, fontSize = 10.sp, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Session: #$sessionId", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                }
                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp))) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B)).width(210.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chat History", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.History, null, tint = NeonCyan) },
                            onClick = { expanded = false; onMenuAction("history") }
                        )
                        DropdownMenuItem(
                            text = { Text("Sync Live DB", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.Sync, null, tint = Color(0xFF10B981)) },
                            onClick = { expanded = false; onMenuAction("sync") }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Transcript", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.Download, null, tint = Color.LightGray) },
                            onClick = { expanded = false; onMenuAction("export") }
                        )
                        if (currentState != SupportState.CLOSED) {
                            DropdownMenuItem(
                                text = { Text("End Session", color = Color(0xFFF59E0B)) },
                                leadingIcon = { Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color(0xFFF59E0B)) },
                                onClick = { expanded = false; onMenuAction("end") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSystemBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF1E293B).copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)) {
            Text(text, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModernUserBubble(msg: ChatMessage) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(modifier = Modifier.widthIn(max = 280.dp), shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp), color = Color.Transparent) {
                Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0072FF), Color(0xFF00C6FF))))) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (msg.fileUrl != null) AsyncImage(model = msg.fileUrl, contentDescription = "Attachment", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).padding(bottom = 8.dp), contentScale = ContentScale.Crop)
                        if (msg.text.isNotEmpty() && msg.text != "Attached Evidence") Text(text = msg.text, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }
            Text(text = msg.timestamp, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, end = 4.dp))
        }
    }
}

@Composable
fun ModernExpertBubble(msg: ChatMessage, expertName: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp, start = 12.dp)) {
                Icon(Icons.Rounded.Security, null, tint = NeonPurple, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(expertName, color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(modifier = Modifier.widthIn(max = 280.dp), shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (msg.fileUrl != null) AsyncImage(model = msg.fileUrl, contentDescription = "Attachment", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).padding(bottom = 8.dp), contentScale = ContentScale.Crop)
                    if (msg.text.isNotEmpty()) Text(text = msg.text, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            Text(text = msg.timestamp, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

@Composable
fun SmartActionChips(onClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp)) {
        Text("Suggestions:", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("I clicked a suspicious link", onClick)
            ActionChip("Money deducted without OTP", onClick)
            ActionChip("Need help with device scan", onClick)
        }
    }
}

@Composable
fun ActionChip(text: String, onClick: (String) -> Unit) {
    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.clickable { onClick(text) }) {
        Text(text, color = Color(0xFF00C6FF), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernInputArea(messageText: String, onMessageChange: (String) -> Unit, isInputEnabled: Boolean, isUploading: Boolean, onAttachClick: () -> Unit, onSendClick: () -> Unit) {
    Surface(color = Color(0xFF131C2B), modifier = Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 16.dp) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onAttachClick, enabled = isInputEnabled && !isUploading, modifier = Modifier.padding(bottom = 2.dp)) {
                if (isUploading) CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Attach", tint = if(isInputEnabled) Color.Gray else Color.DarkGray, modifier = Modifier.size(28.dp))
            }
            OutlinedTextField(value = messageText, onValueChange = onMessageChange, enabled = isInputEnabled, placeholder = { Text("Message", color = Color.Gray) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), disabledContainerColor = Color(0xFF0F172A), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, disabledBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White), maxLines = 4)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp).padding(bottom = 2.dp).background(if (isInputEnabled && messageText.isNotBlank()) Color(0xFF0072FF) else Color(0xFF1E293B), CircleShape).clip(CircleShape).clickable(enabled = isInputEnabled && messageText.isNotBlank()) { onSendClick() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = if (isInputEnabled && messageText.isNotBlank()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}