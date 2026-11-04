package com.example.missedcallreminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

object AlertHelper {
    private const val CHANNEL_ID = "missed_call_reminders_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Missed Call Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun vibrateUrgent(context: Context) {
        val vibrator = getVibrator(context)
        val pattern = longArrayOf(0, 500, 200, 500, 200, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun vibratePulse(context: Context) {
        val vibrator = getVibrator(context)
        val pattern = longArrayOf(0, 200, 150, 200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun scheduleIntervals(context: Context, number: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervals = listOf(5, 10, 15)

        intervals.forEachIndexed { idx, min ->
            val triggerTime = System.currentTimeMillis() + (min * 60 * 1000L)
            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("PHONE", number)
                putExtra("MIN", min)
            }
            val pending = PendingIntent.getBroadcast(
                context, idx, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        }
    }

    fun showNotification(context: Context, number: String, isEmergency: Boolean, prefix: String = "Missed Call") {
        createChannel(context)

        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        val callPending = PendingIntent.getActivity(context, 0, callIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setContentTitle(if (isEmergency) "⚠️ Urgent: $prefix from $number" else "$prefix: $number")
            .setContentText("Tap to return call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(R.drawable.ic_phone, "Call Back", callPending)
            .setAutoCancel(true)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(1001, builder.build())
    }

    fun showDeclinedOptions(context: Context, number: String) {
        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setContentTitle("Call Declined: $number")
            .setContentText("Tap if you want a reminder in 15 minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(6000)
            .setAutoCancel(true)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(1002, builder.build())
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
