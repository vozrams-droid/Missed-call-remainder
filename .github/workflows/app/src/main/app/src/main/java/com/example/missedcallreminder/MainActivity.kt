package com.example.missedcallreminder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val unlockReceiver = ScreenUnlockReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F8F9FA"))
            setPadding(48, 48, 48, 48)
        }

        val titleView = TextView(this).apply {
            text = "Missed Call Reminder"
            textSize = 22f
            setTextColor(Color.parseColor("#1F2937"))
            gravity = Gravity.CENTER
        }

        val statusView = TextView(this).apply {
            text = "Service is active.\nListening for incoming calls & screen unlocks."
            textSize = 15f
            setTextColor(Color.parseColor("#4B5563"))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        rootLayout.addView(titleView)
        rootLayout.addView(statusView)
        setContentView(rootLayout)

        try {
            AlertHelper.createChannel(this)
        } catch (_: Throwable) {}

        requestRequiredPermissions()

        try {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(unlockReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(unlockReceiver, filter)
            }
        } catch (_: Throwable) {}
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.VIBRATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Throwable) {}
    }
}
