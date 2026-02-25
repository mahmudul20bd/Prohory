package com.cyberaware.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class LogClearWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val sharedPref = context.getSharedPreferences("CyberAwarePrefs", Context.MODE_PRIVATE)
        val isAutoClearEnabled = sharedPref.getBoolean("auto_clear_logs", false)

        if (isAutoClearEnabled) {
            val threatPrefs = context.getSharedPreferences("CyberAwareThreats", Context.MODE_PRIVATE)
            val currentThreats = threatPrefs.getStringSet("quarantine_list", emptySet())?.toMutableSet() ?: mutableSetOf()

            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000 // 7 Days in Milliseconds
            val currentTime = System.currentTimeMillis()

            val updatedThreats = currentThreats.filter { log ->
                val parts = log.split("|||")
                if (parts.size >= 3) {
                    val logTime = parts[2].toLongOrNull() ?: 0L
                    (currentTime - logTime) < sevenDaysInMillis // Keep if newer than 7 days
                } else {
                    false
                }
            }.toSet()

            threatPrefs.edit().putStringSet("quarantine_list", updatedThreats).apply()
        }
        return Result.success()
    }
}