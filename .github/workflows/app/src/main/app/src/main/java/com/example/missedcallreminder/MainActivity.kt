package com.example.missedcallreminder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        scroll.addView(layout)
        setContentView(scroll)

        try {
            AlertHelper.createChannel(this)
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

            layout.addView(TextView(this).apply {
                text = "Missed Call Reminder"
                textSize = 22f
                setTextColor(Color.BLACK)
            })

            layout.addView(Switch(this).apply {
                text = "Busy mode (remind me about missed calls)"
                setPadding(0, 40, 0, 0)
                isChecked = prefs.getBoolean("busy", true)
                setOnCheckedChangeListener { _, c ->
                    prefs.edit().putBoolean("busy", c).apply()
                }
            })

            layout.addView(TextView(this).apply {
                text = "Remind me after a missed call at:"
                textSize = 16f
                setPadding(0, 48, 0, 8)
                setTextColor(Color.BLACK)
            })

            // Add more minutes here later, e.g. listOf(1, 2, 3, 5, 15, 20)
            val choices = listOf(1, 2, 3)
            choices.forEach { m ->
                layout.addView(CheckBox(this).apply {
                    text = "$m minute" + if (m > 1) "s" else ""
                    isChecked = prefs.getBoolean("min_$m", true)
                    setOnCheckedChangeListener { _, c ->
                        prefs.edit().putBoolean("min_$m", c).apply()
                    }
                })
            }

            layout.addView(TextView(this).apply {
                text = "Saved automatically. Each ticked time gives a notification with vibration."
                setPadding(0, 16, 0, 0)
            })

            layout.addView(Button(this).apply {
                text = "Test now (fake missed call)"
                setOnClickListener {
                    val count = MissedCallScheduler.scheduleChecked(this@MainActivity, "Test number")
                    Toast.makeText(
                        this@MainActivity,
                        if (count == 0) "Tick at least one time first" else "Test started: $count reminder(s) coming",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

            val perms = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
            )
            if (Build.VERSION.SDK_INT >= 33) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissions(perms.toTypedArray(), 101)
        } catch (t: Throwable) {
            layout.addView(TextView(this).apply {
                setTextColor(Color.RED)
                text = "CRASH CAUSE:\n\n" + t.stackTraceToString()
            })
        }
    }
}
