package com.cyberaware.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.cyberaware.app.ui.theme.CyberAwareTheme
import com.cyberaware.app.ui.theme.NeonBlue
import com.cyberaware.app.ui.theme.NeonCyan
import com.cyberaware.app.ui.theme.NeonPurple
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                OnboardingScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(R.raw.scan_anim, "THREAT DETECTION", "Advanced neural network algorithms designed to intercept real-time zero-day vulnerabilities."),
        OnboardingPage(R.raw.cloud_anim, "GLOBAL INTELLIGENCE", "Synchronize with worldwide defense grids to stay ahead of modern phishing vectors."),
        OnboardingPage(R.raw.cyber_success, "SYSTEM SECURED", "Deploy the ultimate firewall. Your identity, your data, fully encrypted and locked.")
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF02040A))) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // Skip Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                TextButton(onClick = { finishOnboarding(context) }) {
                    Text("SKIP SEQUENCE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // Slider Section
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { position ->
                OnboardingPageContent(pages[position])
            }

            // Cyber Indicators
            Row(modifier = Modifier.height(40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) NeonCyan else Color.DarkGray
                    val width by animateDpAsState(targetValue = if (pagerState.currentPage == iteration) 30.dp else 10.dp, label = "indicator")

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .size(width = width, height = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < pages.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            finishOnboarding(context)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)))
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "INITIALIZE PROTOCOL" else "NEXT MODULE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {

        // Holographic Platform
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.width(200.dp).height(20.dp).background(Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color.Transparent))))
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.anim))
            LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(280.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Text Card
        Surface(
            color = Color(0xFF0B1221).copy(alpha = 0.8f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = page.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = page.desc, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        }
    }
}

fun finishOnboarding(context: Context) {
    val sharedPref = context.getSharedPreferences("AppPref", Context.MODE_PRIVATE)
    sharedPref.edit().putBoolean("FirstTime", false).apply()
    context.startActivity(Intent(context, LoginActivity::class.java))
}

data class OnboardingPage(val anim: Int, val title: String, val desc: String)