package com.cyberaware.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// আমরা শুধু ডার্ক থিম ব্যবহার করব (সাইবার লুকের জন্য)
private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonCyan,
    tertiary = NeonPurple,
    background = CyberBlack,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

@Composable
fun CyberAwareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // আমরা কাস্টম কালার চাই, তাই ডাইনামিক অফ রাখলাম
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // অলওয়েজ ডার্ক মোড

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // স্ট্যাটাস বার কালার
            window.statusBarColor = CyberBlack.toArgb()
            window.navigationBarColor = CyberBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // নিচে টাইপোগ্রাফি ফাইল দিচ্ছি
        content = content
    )
}