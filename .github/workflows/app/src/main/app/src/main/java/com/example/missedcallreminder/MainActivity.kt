package com.example.missedcallreminder

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val errorView = TextView(this).apply {
            setTextColor(Color.RED)
            textSize = 14f
            setPadding(32, 48, 32, 48)
        }
        scroll.addView(errorView)
        setContentView(scroll)

        try {
            // Test Step 1: Notifications
            AlertHelper.createChannel(this)

            // Test Step 2: Show success if no crash
            errorView.setTextColor(Color.GREEN)
            errorView.text = "App initialized successfully! No crash detected.\nChecking permissions..."

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.READ_CALL_LOG,
                    android.Manifest.permission.VIBRATE
                ), 101
            )
        } catch (t: Throwable) {
            // If anything fails, print the exact stack trace directly on your screen
            errorView.text = "CRASH CAUSE:\n\n" + t.stackTraceToString()
        }
    }
}
