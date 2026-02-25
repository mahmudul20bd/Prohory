package com.cyberaware.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class WifiMonitorService(private val context: Context) {

    fun startMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val isAutoScanEnabled = sharedPref.getBoolean("auto_scan_wifi", true)
                if (!isAutoScanEnabled) return

                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    Log.d("WIFI_MONITOR", "New WiFi Connected. Scanning...")

                    // 🔥 NEW LOGIC: Check if it's genuinely a Public (Open) Wi-Fi
                    if (isConnectedToPublicWifi(context)) {
                        sendWifiWarningNotification(
                            "🚨 Public WiFi Detected",
                            "Open network! Ensure your VPN is active to protect data."
                        )
                    } else {
                        Log.d("WIFI_MONITOR", "✅ Secured Private WiFi Detected. No Warning Needed.")
                    }
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    // 🛡️ আসল সিকিউরিটি চেকিং ফাংশন
    @android.annotation.SuppressLint("MissingPermission")
    private fun isConnectedToPublicWifi(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        if (wifiInfo.networkId == -1) return false

        // Android 12 (API 31) বা তার ওপরের ভার্সনের জন্য
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val securityType = wifiInfo.currentSecurityType
            return securityType == WifiInfo.SECURITY_TYPE_OPEN || securityType == WifiInfo.SECURITY_TYPE_OWE
        }

        // Android 11 বা তার নিচের ভার্সনের জন্য
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val scanResults = wifiManager.scanResults
            for (network in scanResults) {
                if (network.BSSID == wifiInfo.bssid) {
                    val capabilities = network.capabilities
                    // WEP, WPA, PSK, EAP না থাকলে ওপেন বা পাবলিক ওয়াইফাই ধরা হবে
                    return !capabilities.contains("WEP") &&
                            !capabilities.contains("WPA") &&
                            !capabilities.contains("PSK") &&
                            !capabilities.contains("EAP")
                }
            }
        }
        return false
    }

    private fun sendWifiWarningNotification(title: String, message: String) {
        val channelId = "WIFI_WARNINGS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // নোটিফিকেশনের গুরুত্ব IMPORTANCE_HIGH করে দিয়েছি যাতে পপ-আপ করে সামনে আসে
            val channel = NotificationChannel(channelId, "WiFi Security", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // আইকন অ্যালার্টে চেঞ্জ করা হয়েছে
            .setColor(Color.RED) // কালার রেড করা হয়েছে
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}