package com.example.missedcallreminder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val perms = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        val title = TextView(this).apply {
            text = "Missed Call Reminder"
            textSize = 22f
        }
        val busy = Switch(this).apply {
            text = "Busy mode (remind me)"
            isChecked = prefs.getBoolean("busy", true)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("busy", c).apply() }
        }
        val test = Switch(this).apply {
            text = "Test mode (1, 2, 3 min)"
            isChecked = prefs.getBoolean("test", false)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("test", c).apply() }
        }
        val info = TextView(this).apply {
            text = "Reminders come 5, 15 and 20 minutes after a missed or declined call."
            setPadding(0, 32, 0, 0)
        }
        layout.addView(title)
        layout.addView(busy)
        layout.addView(test)
        layout.addView(info)
        setContentView(layout)
    }
}
