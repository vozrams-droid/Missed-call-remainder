package com.example.missedcallreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val number = inputData.getString("number") ?: "Unknown"
        val min = inputData.getLong("min", 0)
        val missedAt = inputData.getLong("missedAt", 0)
        val ctx = applicationContext

        // If you already called back, stop all remaining reminders
        if (calledBack(number, missedAt)) {
            WorkManager.getInstance(ctx).cancelAllWorkByTag("missed_$number")
            return Result.success()
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("missed", "Missed call reminders", NotificationManager.IMPORTANCE_HIGH)
        )

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val callBack = PendingIntent.getActivity(
            ctx, number.hashCode(),
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")), flags
        )
        val dismiss = PendingIntent.getBroadcast(
            ctx, number.hashCode(),
            Intent(ctx, DismissReceiver::class.java).putExtra("number", number), flags
        )

        val notif = Notification.Builder(ctx, "missed")
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed call")
            .setContentText("From $number, $min min ago")
            .setContentIntent(callBack)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Call back", callBack).build())
            .addAction(Notification.Action.Builder(null, "Dismiss", dismiss).build())
            .build()

        nm.notify(number, 1, notif)
        return Result.success()
    }

    private fun calledBack(number: String, missedAt: Long): Boolean {
        if (number == "Unknown") return false
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
        nm.cancel(number, 1)
    }
}
