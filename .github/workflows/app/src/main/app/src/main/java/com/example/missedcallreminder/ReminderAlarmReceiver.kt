package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("PHONE") ?: return
        val min = intent.getIntExtra("MIN", 5)

        if (CallTracker.pendingNumbers.contains(number)) {
            AlertHelper.vibrateUrgent(context)
            AlertHelper.showNotification(context, number, isEmergency = true, prefix = "Reminder ($min min)")
        }
    }
}
