package com.example.missedcallreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {
    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var savedNumber: String? = null
        private var isIncoming = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        var state = TelephonyManager.CALL_STATE_IDLE
        if (stateStr == TelephonyManager.EXTRA_STATE_RINGING) {
            state = TelephonyManager.CALL_STATE_RINGING
        } else if (stateStr == TelephonyManager.EXTRA_STATE_OFFHOOK) {
            state = TelephonyManager.CALL_STATE_OFFHOOK
        }

        onCustomCallStateChanged(context, state, incomingNumber)
    }

    private fun onCustomCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                savedNumber = number
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call was answered
                isIncoming = false
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isIncoming) {
                    // Call was rejected or missed
                    val isEscalated = CallTracker.recordCall(savedNumber)
                    if (isEscalated) {
                        AlertHelper.triggerEmergencyEscalation(context, savedNumber ?: "Unknown")
                    }
                }
                isIncoming = false
            }
        }
        lastState = state
    }
}
