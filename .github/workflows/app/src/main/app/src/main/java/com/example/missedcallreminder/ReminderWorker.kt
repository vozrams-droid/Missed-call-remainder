package com.example.missedcallreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CallLog
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val number = inputData.getString("number") ?: "Unknown"
        val min = inputData.getLong("min", 0)
        val missedAt = inputData.getLong("missedAt", 0)
        val ctx = applicationContext

        // If you already called back, stop the remaining reminders
        if (calledBack(number, missedAt)) {
            WorkManager.getInstance(ctx).cancelAllWorkByTag("missed_$number")
            return Result.success()
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "missed_v2", "Missed call reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 150, 400)
        }
        nm.createNotificationChannel(channel)

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val dial = if (number == "Unknown" || number == "Test number") Intent(Intent.ACTION_DIAL)
        else Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        val callBack = PendingIntent.getActivity(ctx, number.hashCode(), dial, flags)
        val dismiss = PendingIntent.getBroadcast(
            ctx, number.hashCode(),
            Intent(ctx, DismissReceiver::class.java).putExtra("number", number), flags
        )

        val notif = Notification.Builder(ctx, "missed_v2")
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed call reminder ($min min)")
            .setContentText("From $number, $min min ago")
            .setContentIntent(callBack)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Call back", callBack).build())
            .addAction(Notification.Action.Builder(null, "Dismiss", dismiss).build())
            .build()

        nm.notify(number, min.toInt(), notif)
        vibrate(ctx)
        return Result.success()
    }

    private fun vibrate(ctx: Context) {
        try {
            val pattern = longArrayOf(0, 400, 150, 400)
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            // ignore vibration errors
        }
    }

    private fun calledBack(number: String, missedAt: Long): Boolean {
        if (number == "Unknown" || number == "Test number") return false
        return try {
            applicationContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(number, CallLog.Calls.OUTGOING_TYPE.toString(), missedAt.toString()),
                null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("number") ?: return
        WorkManager.getInstance(context).cancelAllWorkByTag("missed_$number")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }
}
