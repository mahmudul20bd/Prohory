package com.cyberaware.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cyberaware.app.ui.theme.CyberAwareTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberAwareTheme {
                // SettingsScreen-কে কল করা হচ্ছে
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}