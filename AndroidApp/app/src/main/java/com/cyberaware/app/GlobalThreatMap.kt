package com.cyberaware.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlin.random.Random

// Data class for live attack logs
data class ThreatLog(val ip: String, val origin: String, val type: String, val color: Color)

@Composable
fun GlobalThreatMapScreen() {
    val attackLogs = remember { mutableStateListOf<ThreatLog>() }

    // Simulate incoming global threats
    LaunchedEffect(Unit) {
        val origins = listOf("RU-MOS", "CN-BEI", "US-NY", "KP-PYO", "IR-TEH", "BR-SAO")
        val types = listOf("DDoS", "SQLi", "Ransomware", "Brute-force", "Zero-day")
        val colors = listOf(Color.Red, Color.Yellow, NeonPurple)

        while (true) {
            delay(Random.nextLong(800, 2500))
            if (attackLogs.size > 30) attackLogs.removeAt(attackLogs.size - 1)

            val newLog = ThreatLog(
                ip = "${Random.nextInt(10, 255)}.${Random.nextInt(10, 255)}.${Random.nextInt(1, 255)}.${Random.nextInt(1, 255)}",
                origin = origins.random(),
                type = types.random(),
                color = colors.random()
            )
            attackLogs.add(0, newLog)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A))
            .padding(16.dp)
    ) {
        // Top Bar
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Public, contentDescription = "Global Map", tint = NeonCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("GLOBAL THREAT INTELLIGENCE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Live Satellite & Neural Network Monitoring", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Holographic Map/Radar Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0B1221)
        ) {
            HolographicThreatRadar()

            // Map Overlay Info
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Threat Terminal
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
            color = Color(0xFF131C2B),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INCOMING ATTACK VECTORS", color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attackLogs) { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(log.color, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(log.origin, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text(log.ip, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(log.type, color = log.color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicThreatRadar() {
    val infiniteTransition = rememberInfiniteTransition()

    // Radar Sweep Animation
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)
    )

    // Pulsing Nodes Animation
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // Moving Laser Trajectories
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(centerX, centerY) * 0.9f

        // Draw World Map "Dots" (Stylized abstract map representation)
        val dotRadius = 2f
        for (i in 0..20) {
            for (j in 0..10) {
                val x = (size.width / 20) * i
                val y = (size.height / 10) * j
                if (Random.nextFloat() > 0.4f) { // Creates abstract continents
                    drawCircle(color = Color.White.copy(alpha = 0.1f), radius = dotRadius, center = Offset(x, y))
                }
            }
        }

        // Draw Radar Circles
        drawCircle(color = NeonCyan.copy(alpha = 0.2f), radius = maxRadius, center = Offset(centerX, centerY), style = Stroke(width = 2f))
        drawCircle(color = NeonCyan.copy(alpha = 0.1f), radius = maxRadius * 0.66f, center = Offset(centerX, centerY), style = Stroke(width = 1f))
        drawCircle(color = NeonCyan.copy(alpha = 0.1f), radius = maxRadius * 0.33f, center = Offset(centerX, centerY), style = Stroke(width = 1f))

        // Draw Radar Sweeper
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color.Transparent, NeonCyan.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(centerX, centerY)
            ),
            startAngle = radarRotation, sweepAngle = 70f, useCenter = true,
            topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )

        // Draw Central "Home" Node
        drawCircle(color = NeonCyan, radius = 8f, center = Offset(centerX, centerY))
        drawCircle(color = NeonCyan.copy(alpha = pulseAlpha), radius = 16f, center = Offset(centerX, centerY), style = Stroke(width = 2f))

        // Generate Target Nodes (Incoming Attacks)
        val targetNodes = listOf(
            Offset(centerX * 0.2f, centerY * 0.3f), // NA
            Offset(centerX * 1.8f, centerY * 0.4f), // ASIA
            Offset(centerX * 1.5f, centerY * 1.6f), // AUS
            Offset(centerX * 0.5f, centerY * 1.5f)  // SA
        )

        targetNodes.forEach { node ->
            // Draw Threat Nodes
            drawCircle(color = Color.Red, radius = 6f, center = node)
            drawCircle(color = Color.Red.copy(alpha = pulseAlpha), radius = 12f, center = node, style = Stroke(width = 2f))

            // Draw Attack Arcs (Bezier Curves) from Threats to Center
            val path = Path().apply {
                moveTo(node.x, node.y)
                quadraticBezierTo(
                    node.x + (centerX - node.x) / 2, node.y, // Control point
                    centerX, centerY // Destination
                )
            }

            drawPath(
                path = path,
                color = Color.Red.copy(alpha = 0.3f),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), progress * 20f))
            )

            // Draw moving "laser packet" along the path (Approximation using lerp)
            val laserX = node.x + (centerX - node.x) * progress
            val laserY = node.y + (centerY - node.y) * progress
            drawCircle(color = Color.White, radius = 4f, center = Offset(laserX, laserY))
        }
    }
}