package com.example.missedcallreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

object AlertHelper {
    private const val CHANNEL_ID = "missed_call_channel"
    private const val CHANNEL_NAME = "Persistent Reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for missed and escalated calls"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun triggerEmergencyEscalation(context: Context, number: String) {
        triggerVibration(context, isEmergency = true)
        showNotification(
            context,
            title = "URGENT: 3+ Calls Received",
            message = "Caller $number rang 3 times. Immediate review needed."
        )
    }

    fun triggerUnlockReminder(context: Context, pendingCount: Int) {
        if (pendingCount <= 0) return
        triggerVibration(context, isEmergency = false)
        showNotification(
            context,
            title = "Missed Call Reminder",
            message = "You have $pendingCount pending missed call(s) waiting."
        )
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun triggerVibration(context: Context, isEmergency: Boolean) {
        val pattern = if (isEmergency) {
            // Rapid high-urgency buzz: pause 0ms, buzz 400ms, pause 100ms, buzz 400ms, pause 100ms, buzz 600ms
            longArrayOf(0, 400, 100, 400, 100, 600)
        } else {
            // Standard single buzz: pause 0ms, buzz 350ms
            longArrayOf(0, 350)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
