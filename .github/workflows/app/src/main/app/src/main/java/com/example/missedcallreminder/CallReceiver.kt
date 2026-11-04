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
    // Schedules one reminder for every selected time. Returns how many.
    fun scheduleSelected(context: Context, number: String): Int {
        val selected = Prefs.selected(context)
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag("missed_$number")
        val missedAt = System.currentTimeMillis()
        selected.forEach { m ->
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(m.toLong(), TimeUnit.MINUTES)
                .setInputData(workDataOf("number" to number, "min" to m.toLong(), "missedAt" to missedAt))
                .addTag("missed_$number")
                .build()
            wm.enqueue(req)
        }
        return selected.size
    }
}

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
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
                        val raw = sp.getString("number", null)
                        val num = raw ?: "Unknown"
                        Prefs.saveLastMissed(context, num)
                        if (Prefs.busy(context)) {
                            if (CallTracker.recordCall(raw)) {
                                AlertHelper.triggerEmergencyEscalation(context, num)
                            }
                            MissedCallScheduler.scheduleSelected(context, num)
                        }
                    }
                    sp.edit().clear().apply()
                }
            }
        } catch (t: Throwable) {
            // never crash from a phone-state event
        }
    }
}
