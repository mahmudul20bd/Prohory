package com.cyberaware.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExpertChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                AdvancedChatScreen(onBack = { finish() })
            }
        }
    }
}

// --- DATA MODELS ---
enum class MsgType { TEXT, IMAGE, AUDIO }
data class ChatMessage(
    val id: String,
    val content: String,
    val isMe: Boolean,
    val type: MsgType = MsgType.TEXT,
    val time: String = "Now"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // States
    var message by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    // Dummy Real-time Data
    val chatHistory = remember { mutableStateListOf(
        ChatMessage("1", "Hello Agent Prohory! 👋", false),
        ChatMessage("2", "Our AI detected a potential threat in your network.", false),
        ChatMessage("3", "Can you send the screenshot?", false)
    )}

    // Auto Scroll to Bottom when new message arrives
    LaunchedEffect(chatHistory.size) {
        listState.animateScrollToItem(chatHistory.size - 1)
    }

    Scaffold(
        // 🔥 HEADER WITH CALL OPTIONS
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Cyber Expert", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online | Typing...", color = SuccessGreen, fontSize = 12.sp)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Voice Calling...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = NeonCyan)
                    }
                    IconButton(onClick = { Toast.makeText(context, "Video Calling...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Rounded.Videocam, contentDescription = "Video", tint = NeonCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1221))
            )
        },
        containerColor = Color(0xFF050A14)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 🔥 CHAT LIST
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), // Input bar এর জন্য জায়গা
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { chat ->
                    ChatBubbleItem(chat)
                }
            }

            // 🔥 ATTACHMENT MENU (ANIMATED)
            AnimatedVisibility(
                visible = showAttachMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
            ) {
                AttachmentPopup { type ->
                    // Fake File Upload Logic
                    chatHistory.add(ChatMessage(System.currentTimeMillis().toString(), "File Uploaded: $type", true, MsgType.IMAGE))
                    showAttachMenu = false
                }
            }

            // 🔥 BOTTOM INPUT BAR
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF0B1221))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Attach Button
                    IconButton(onClick = { showAttachMenu = !showAttachMenu }) {
                        Icon(
                            if(showAttachMenu) Icons.Default.Close else Icons.Rounded.AttachFile,
                            contentDescription = "Attach",
                            tint = Color.Gray,
                            modifier = Modifier.rotate(45f) // Pin style look
                        )
                    }

                    // Input Field
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161B28),
                            unfocusedContainerColor = Color(0xFF161B28),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = NeonBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Mic or Send Button
                    FloatingActionButton(
                        onClick = {
                            if (message.isNotEmpty()) {
                                chatHistory.add(ChatMessage(System.currentTimeMillis().toString(), message, true))
                                message = ""
                                // Fake Reply
                                scope.launch {
                                    delay(2000)
                                    chatHistory.add(ChatMessage("x", "We are analyzing your request...", false))
                                }
                            } else {
                                isRecording = !isRecording
                            }
                        },
                        containerColor = if(isRecording) ErrorRed else NeonBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            if (message.isEmpty()) Icons.Rounded.Mic else Icons.Rounded.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}

// --- 💬 MESSAGE BUBBLES ---
@Composable
fun ChatBubbleItem(chat: ChatMessage) {
    val isMe = chat.isMe
    val align = if (isMe) Alignment.End else Alignment.Start
    val color = if (isMe) NeonBlue else Color(0xFF1E293B)
    val shape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp)
    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 2.dp)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(color)
                .padding(12.dp)
        ) {
            when(chat.type) {
                MsgType.TEXT -> {
                    Text(chat.content, color = Color.White, fontSize = 15.sp)
                }
                MsgType.IMAGE -> {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        Text("screenshot.jpg", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                    }
                }
                MsgType.AUDIO -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(modifier = Modifier.width(100.dp), color = Color.White, thickness = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("0:15", color = Color.White)
                    }
                }
            }
        }
        Text(
            text = chat.time,
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

// --- 📎 ATTACHMENT MENU ---
@Composable
fun AttachmentPopup(onSelect: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttachItem(Icons.Rounded.Description, Color(0xFF7C4DFF), "Document") { onSelect("DOC") }
            AttachItem(Icons.Rounded.CameraAlt, Color(0xFFFF4081), "Camera") { onSelect("CAM") }
            AttachItem(Icons.Rounded.Image, Color(0xFF448AFF), "Gallery") { onSelect("IMG") }
        }
    }
}

@Composable
fun AttachItem(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

// Helper for Rotation
fun Modifier.rotate(degrees: Float) = this.then(Modifier.graphicsLayer(rotationZ = degrees))