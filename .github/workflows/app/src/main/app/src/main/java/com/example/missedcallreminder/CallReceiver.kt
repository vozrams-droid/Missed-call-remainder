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

    companion object {
        // true = reminders at 1, 2, 3 min (testing). Set false for 5, 15, 20 min.
        private const val TEST_MODE = true
    }

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
                    val num = sp.getString("number", null)
                    if (CallTracker.recordCall(num)) {
                        AlertHelper.triggerEmergencyEscalation(context, num ?: "Unknown")
                    }
                    scheduleReminders(context, num ?: "Unknown")
                }
                sp.edit().clear().apply()
            }
        }
    }

    private fun scheduleReminders(context: Context, number: String) {
        val delays = if (TEST_MODE) listOf(1L, 2L, 3L) else listOf(5L, 15L, 20L)
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
