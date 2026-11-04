package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val pendingCount = CallTracker.getPendingMissedCount()
            if (pendingCount > 0) {
                AlertHelper.triggerUnlockReminder(context, pendingCount)
                // Clears count after alerting so it won't repeatedly buzz on every single unlock
                CallTracker.clearPending()
            }
        }
    }
}
