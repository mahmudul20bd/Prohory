package com.cyberaware.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.text.SimpleDateFormat
import java.util.*

// Data Models
data class ChatMessage(val id: String, val text: String, val isMe: Boolean, val timestamp: String, val isSystem: Boolean = false, val isFile: Boolean = false)

data class BackendChatMessage(
    val senderUsername: String,
    val receiverUsername: String,
    val content: String,
    val fileUrl: String? = null,
    val timestamp: String? = null
)

enum class SupportState { CONNECTING, READY_TO_CHAT, EXPERT_JOINED }

class ExpertChatActivity : ComponentActivity() {
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
                DynamicSupportScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun DynamicSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val prefs = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
    val currentUser = prefs.getString("USERNAME", "User") ?: "User"

    var currentState by remember { mutableStateOf(SupportState.CONNECTING) }
    var currentExpertName by remember { mutableStateOf("Support Pool") }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var stompClient by remember { mutableStateOf<StompClient?>(null) }
    val compositeDisposable = remember { CompositeDisposable() }

    fun getTime() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    // 🔥 REAL WEBSOCKET CONNECTION 🔥
    LaunchedEffect(Unit) {
        messages.add(ChatMessage(UUID.randomUUID().toString(), "Establishing secure connection to Server...", isMe = false, getTime(), isSystem = true))

        try {
            val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://cyber-aware.onrender.com/ws/websocket")
            client.connect()
            stompClient = client

            delay(1000)
            currentState = SupportState.READY_TO_CHAT
            messages.add(ChatMessage(UUID.randomUUID().toString(), "Connected. Please describe your issue below.", isMe = false, getTime(), isSystem = true))

            val topicSubscription = client.topic("/topic/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage ->
                    val payload = Gson().fromJson(topicMessage.payload, BackendChatMessage::class.java)

                    if (payload.senderUsername != currentUser) {
                        if (currentState != SupportState.EXPERT_JOINED) {
                            currentState = SupportState.EXPERT_JOINED
                            currentExpertName = payload.senderUsername
                            messages.add(ChatMessage(UUID.randomUUID().toString(), "$currentExpertName has joined the secure chat.", isMe = false, getTime(), isSystem = true))
                        }

                        messages.add(
                            ChatMessage(id = UUID.randomUUID().toString(), text = payload.content, isMe = false, timestamp = getTime())
                        )
                        coroutineScope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }
                }, { error ->
                    Log.e("ChatError", "Error receiving message", error)
                })

            compositeDisposable.add(topicSubscription)

        } catch (e: Exception) {
            messages.add(ChatMessage(UUID.randomUUID().toString(), "Connection Failed. Try again.", isMe = false, getTime(), isSystem = true))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            compositeDisposable.dispose()
            stompClient?.disconnect()
        }
    }

    // Message Send Logic (Called from Input Component)
    fun sendMessage(textToSend: String) {
        if (stompClient?.isConnected == true) {
            messages.add(ChatMessage(UUID.randomUUID().toString(), textToSend, isMe = true, getTime()))
            coroutineScope.launch { listState.animateScrollToItem(messages.size - 1) }

            val msgObj = BackendChatMessage(senderUsername = currentUser, receiverUsername = currentExpertName, content = textToSend)
            val jsonPayload = Gson().toJson(msgObj)

            stompClient!!.send("/app/chat.send", jsonPayload)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ Log.d("Chat", "Sent successfully") }, { Log.e("Chat", "Send failed") })
        }
    }

    // UI Structure
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 🔥 Isolated Header Component
            ChatHeader(
                currentState = currentState,
                currentExpertName = currentExpertName,
                onBack = onBack
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg) }
            }

            // 🔥 Isolated Input Box Component (Fixes Focus Bug)
            ChatInputArea(
                isInputEnabled = currentState != SupportState.CONNECTING,
                onSendMessage = { text -> sendMessage(text) }
            )
        }
    }
}

// ==========================================
// 🔥 ISOLATED COMPONENTS (For Performance)
// ==========================================

@Composable
fun ChatHeader(currentState: SupportState, currentExpertName: String, onBack: () -> Unit) {
    Surface(color = Color(0xFF0B1221).copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan) }

                val infiniteTransition = rememberInfiniteTransition()
                val pulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse))
                val dotColor = when(currentState) {
                    SupportState.CONNECTING -> Color.Gray
                    SupportState.READY_TO_CHAT -> Color(0xFFFF9800)
                    SupportState.EXPERT_JOINED -> Color.Green
                }

                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(modifier = Modifier.size(45.dp).background(NeonBlue.copy(0.1f), CircleShape).border(1.dp, NeonBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if(currentState == SupportState.EXPERT_JOINED) Icons.Default.SupportAgent else Icons.Default.Computer, contentDescription = "Avatar", tint = NeonCyan, modifier = Modifier.size(28.dp))
                    }
                    Box(modifier = Modifier.size(12.dp).background(dotColor.copy(alpha = pulse), CircleShape).border(2.dp, Color(0xFF0B1221), CircleShape))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (currentState == SupportState.EXPERT_JOINED) currentExpertName.uppercase() else "WAITING FOR AGENT...",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when(currentState) {
                            SupportState.CONNECTING -> "Connecting to server..."
                            SupportState.READY_TO_CHAT -> "In Support Queue"
                            SupportState.EXPERT_JOINED -> "Secure Channel Locked"
                        },
                        color = dotColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(isInputEnabled: Boolean, onSendMessage: (String) -> Unit) {
    // 🔥 State is now inside the input area to prevent full screen refresh
    var messageText by rememberSaveable { mutableStateOf("") }

    Surface(color = Color(0xFF0B1221), modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            IconButton(
                onClick = { /* File Picker */ },
                enabled = isInputEnabled,
                modifier = Modifier.background(if(isInputEnabled) Color(0xFF1E293B) else Color.DarkGray, CircleShape).size(45.dp)
            ) { Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = if(isInputEnabled) NeonCyan else Color.Gray) }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                enabled = isInputEnabled,
                placeholder = { Text(text = if(isInputEnabled) "Type your issue here..." else "Connecting...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B), disabledContainerColor = Color(0xFF0F172A),
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Transparent, disabledBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = {
                    onSendMessage(messageText)
                    messageText = "" // মেসেজ পাঠানোর পর বক্স ক্লিয়ার হবে
                },
                enabled = isInputEnabled && messageText.isNotBlank(),
                modifier = Modifier.background(if(isInputEnabled && messageText.isNotBlank()) Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)) else Brush.horizontalGradient(listOf(Color.DarkGray, Color.DarkGray)), CircleShape).size(45.dp)
            ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = if(isInputEnabled && messageText.isNotBlank()) Color.White else Color.Gray) }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    if (msg.isSystem) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Surface(color = Color(0xFF1E293B).copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.DarkGray)) {
                Text(msg.text, color = Color.Yellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        return
    }

    val align = if (msg.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isMe) NeonBlue.copy(alpha = 0.2f) else Color(0xFF1E293B)
    val borderColor = if (msg.isMe) NeonCyan else Color.DarkGray
    val shape = if (msg.isMe) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Column(horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start) {
            Surface(color = bgColor, shape = shape, border = BorderStroke(1.dp, borderColor), modifier = Modifier.widthIn(max = 280.dp)) {
                Text(text = msg.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.timestamp, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}