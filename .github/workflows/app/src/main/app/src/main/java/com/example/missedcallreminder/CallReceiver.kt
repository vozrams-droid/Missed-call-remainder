package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object MissedCallScheduler {
    // Schedules a reminder for every ticked time. Returns how many were scheduled.
    fun scheduleChecked(context: Context, number: String): Int {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        // Keep this list the same as in MainActivity
        val choices = listOf(1, 2, 3)
        val selected = choices.filter { prefs.getBoolean("min_$it", true) }

        val tag = "missed_$number"
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(tag)
        val missedAt = System.currentTimeMillis()

        selected.forEach { m ->
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(m.toLong(), TimeUnit.MINUTES)
                .setInputData(workDataOf("number" to number, "min" to m.toLong(), "missedAt" to missedAt))
                .addTag(tag)
                .build()
            wm.enqueue(req)
        }
        return selected.size
    }
}

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val sp = context.getSharedPreferences("callstate", Context.MODE_PRIVATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val wasRinging = sp.getBoolean("ringing", false)
                val e = sp.edit().putBoolean("ringing", true)
                if (!wasRinging) e.putBoolean("answered", false)
                if (!number.isNullOrEmpty()) e.putString("number", number)
                e.apply()
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                sp.edit().putBoolean("answered", true).apply()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val missed = sp.getBoolean("ringing", false) && !sp.getBoolean("answered", false)
                if (missed) {
                    val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    if (settings.getBoolean("busy", true)) {
                        val raw = sp.getString("number", null)
                        if (CallTracker.recordCall(raw)) {
                            AlertHelper.triggerEmergencyEscalation(context, raw ?: "Unknown")
                        }
                        MissedCallScheduler.scheduleChecked(context, raw ?: "Unknown")
                    }
                }
                sp.edit().clear().apply()
            }
        }
    }
}
