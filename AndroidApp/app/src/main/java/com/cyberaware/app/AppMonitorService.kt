package com.cyberaware.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppMonitorService : AccessibilityService() {

    // 🔥 THE FIX: Session Tracker
    companion object {
        var temporarilyUnlockedPackage: String? = null
    }

    private var currentApp: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        if (event.packageName != null) {
            val packageName = event.packageName.toString()

            // Avoid triggering on our own app or system UI to prevent infinite loops
            if (packageName == "com.cyberaware.app" || packageName.contains("systemui")) return

            if (packageName != currentApp) {
                currentApp = packageName
                Log.d("Prohory_AppMonitor", "User opened: $packageName")

                // 🔥 Check if the user just unlocked this specific app via Fingerprint
                if (packageName == temporarilyUnlockedPackage) {
                    Log.d("Prohory_AppMonitor", "🔓 Access Granted for session: $packageName")
                    return // Let them use the app
                } else {
                    // User switched to a new app, reset the temporary unlock session
                    temporarilyUnlockedPackage = null
                    checkIfAppIsLocked(packageName)
                }
            }
        }
    }

    private fun checkIfAppIsLocked(packageName: String) {
        val sharedPreferences = getSharedPreferences("CyberAwareAppLock", Context.MODE_PRIVATE)
        val lockedApps = sharedPreferences.getStringSet("locked_apps", emptySet()) ?: emptySet()

        if (lockedApps.contains(packageName)) {
            Log.d("Prohory_AppMonitor", "🚨 THREAT: Locked app accessed! Triggering Lockdown!")

            // Trigger the Block UI
            val blockIntent = Intent(this, BlockUIActivity::class.java).apply {
                putExtra("BLOCKED_APP", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(blockIntent)
        }
    }

    override fun onInterrupt() {
        Log.d("Prohory_AppMonitor", "Service Interrupted")
    }
}