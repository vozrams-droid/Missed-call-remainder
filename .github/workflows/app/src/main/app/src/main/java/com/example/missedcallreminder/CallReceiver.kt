package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var ringStartTime = 0L
        private var callerNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                ringStartTime = System.currentTimeMillis()
                callerNumber = number
                lastState = TelephonyManager.CALL_STATE_RINGING
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    val duration = System.currentTimeMillis() - ringStartTime
                    val phone = callerNumber ?: number
                    val callCount = CallTracker.recordCall(phone)

                    if (duration < 2500) {
                        // User actively hit the red decline button
                        if (callCount >= 3) {
                            CallTracker.markPending(phone)
                            AlertHelper.vibrateUrgent(context)
                            AlertHelper.scheduleIntervals(context, phone)
                            AlertHelper.showNotification(context, phone, isEmergency = true)
                        } else {
                            AlertHelper.showDeclinedOptions(context, phone)
                        }
                    } else {
                        // Regular missed call
                        CallTracker.markPending(phone)
                        if (callCount >= 3) {
                            AlertHelper.vibrateUrgent(context)
                            AlertHelper.scheduleIntervals(context, phone)
                            AlertHelper.showNotification(context, phone, isEmergency = true)
                        } else {
                            AlertHelper.showNotification(context, phone, isEmergency = false)
                        }
                    }
                }
                lastState = TelephonyManager.CALL_STATE_IDLE
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                callerNumber?.let { CallTracker.clear(it) }
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
            }
        }
    }
}
