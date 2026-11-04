package com.example.missedcallreminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun schedule(context: Context, number: String, min: Long, missedAt: Long) {
        val test = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("test", false)
        val unit = if (test) TimeUnit.SECONDS else TimeUnit.MINUTES
        val tag = "missed_$number"
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(tag)
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(min, unit)
            .setInputData(workDataOf("number" to number, "min" to min, "missedAt" to missedAt))
            .addTag(tag)
            .build()
        wm.enqueue(req)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("number") ?: return
        val min = intent.getLongExtra("min", 5L)
        val missedAt = intent.getLongExtra("missedAt", System.currentTimeMillis())
        ReminderScheduler.schedule(context, number, min, missedAt)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel("ask_$number", 2)
    }
}
