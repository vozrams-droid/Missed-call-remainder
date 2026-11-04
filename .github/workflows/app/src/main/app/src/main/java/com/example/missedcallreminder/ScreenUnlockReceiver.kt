package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            if (CallTracker.pendingNumbers.isNotEmpty()) {
                val num = CallTracker.pendingNumbers.first()
                AlertHelper.vibratePulse(context)
                AlertHelper.showNotification(context, num, isEmergency = true, prefix = "Pending Callback")
            }
        }
    }
}
