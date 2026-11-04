package com.example.missedcallreminder

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val statusView = TextView(this).apply {
            setTextColor(Color.parseColor("#1B5E20"))
            textSize = 16f
            setPadding(32, 96, 32, 48)
        }
        scroll.addView(statusView)
        setContentView(scroll)

        try {
            AlertHelper.createChannel(this)

            val perms = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
            )
            if (Build.VERSION.SDK_INT >= 33) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissions(perms.toTypedArray(), 101)

            statusView.text = "Missed Call Reminder is ready.\n\n" +
                "Allow Phone, Call logs and Notifications when asked.\n" +
                "Missed or declined calls will remind you at the set times."
        } catch (t: Throwable) {
            statusView.setTextColor(Color.RED)
            statusView.text = "CRASH CAUSE:\n\n" + t.stackTraceToString()
        }
    }
}
