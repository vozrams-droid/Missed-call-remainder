package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val sp = context.getSharedPreferences("callstate", Context.MODE_PRIVATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                sp.edit().putBoolean("ringing", true).putBoolean("answered", false).apply()
                if (!number.isNullOrEmpty()) sp.edit().putString("number", number).apply()
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                sp.edit().putBoolean("answered", true).apply()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val missed = sp.getBoolean("ringing", false) && !sp.getBoolean("answered", false)
                if (missed) {
                    val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    if (settings.getBoolean("busy", true)) {
                        val num = sp.getString("number", null) ?: "Unknown"
                        schedule(context, num, settings.getBoolean("test", false))
                    }
                }
                sp.edit().clear().apply()
            }
        }
    }

    private fun schedule(context: Context, number: String, testMode: Boolean) {
        val delays = if (testMode) listOf(1L, 2L, 3L) else listOf(5L, 15L, 20L)
        val tag = "missed_$number"
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(tag)
        val missedAt = System.currentTimeMillis()
        delays.forEach { min ->
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(min, TimeUnit.MINUTES)
                .setInputData(workDataOf("number" to number, "min" to min, "missedAt" to missedAt))
                .addTag(tag)
                .build()
            wm.enqueue(req)
        }
    }
}
