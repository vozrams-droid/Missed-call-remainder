package com.example.missedcallreminder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val primary = Color.parseColor("#4F46E5")
    private val primaryDark = Color.parseColor("#4338CA")
    private val violet = Color.parseColor("#7C3AED")
    private val bg = Color.parseColor("#F3F4F9")
    private val dark = Color.parseColor("#111827")
    private val gray = Color.parseColor("#6B7280")
    private val green = Color.parseColor("#059669")
    private val red = Color.parseColor("#DC2626")
    private val chipOff = Color.parseColor("#EEF0F6")

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private var busyStatus: TextView? = null
    private var summary: TextView? = null
    private var permStatus: TextView? = null
    private var lastMissed: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            styleWindow()
            setContentView(buildUi())
            AlertHelper.createChannel(this)
            if (missingPermissions().isNotEmpty()) askPermissions()
        } catch (t: Throwable) {
            showCrash(t)
        }
    }

    override fun onResume() {
        super.onResume()
        try { refresh() } catch (t: Throwable) { /* ignore */ }
    }

    private fun showCrash(t: Throwable) {
        val tv = TextView(this).apply {
            setTextColor(Color.RED)
            textSize = 13f
            setPadding(32, 96, 32, 32)
            text = "Something went wrong:\n\n" + t.stackTraceToString()
        }
        setContentView(ScrollView(this).apply { addView(tv) })
    }

    private fun styleWindow() {
        try {
            window.statusBarColor = primaryDark
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } catch (e: Throwable) { /* ignore */ }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        scroll.addView(root)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(26))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(primary, violet)
            ).apply { cornerRadius = dp(24).toFloat() }
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }
        header.addView(label("Missed Call Reminder", 24f, Color.WHITE, true))
        header.addView(
            label(
                "Never forget to call back. Get a nudge after every missed or declined call.",
                14f, Color.parseColor("#E0E7FF")
            ).apply { setPadding(0, dp(6), 0, 0) }
        )
        root.addView(header)

        // Busy + vibration card
        val busyCard = card()
        busyCard.addView(
            switchRow("Busy mode", "Remind me about calls I miss or decline", Prefs.busy(this)) { v ->
                Prefs.setBusy(this, v)
                refresh()
            }
        )
        val bs = label("", 13f, green, true).apply { setPadding(0, dp(8), 0, 0) }
        busyStatus = bs
        busyCard.addView(bs)
        busyCard.addView(
            switchRow("Vibration", "Buzz with every reminder", Prefs.vibrate(this)) { v ->
                Prefs.setVibrate(this, v)
            }.apply { setPadding(0, dp(14), 0, 0) }
        )
        root.addView(busyCard)

        // Reminder times card
        val timesCard = card()
        timesCard.addView(label("Remind me after a missed call", 16f, dark, true))
        timesCard.addView(
            label("Tap to select. Each selected time gives a notification.", 13f, gray)
                .apply { setPadding(0, dp(2), 0, dp(10)) }
        )
        Prefs.CHOICES.chunked(4).forEach { items ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            items.forEach { m ->
                row.addView(chip(m), chipParams())
            }
            repeat(4 - items.size) { row.addView(View(this), chipParams()) }
            timesCard.addView(row)
        }
        val sm = label("", 13f, primary, true).apply { setPadding(0, dp(10), 0, 0) }
        summary = sm
        timesCard.addView(sm)
        })
        root.addView(timesCard)

        // Last missed call card
        val lastCard = card()
        lastCard.addView(label("Last missed call", 16f, dark, true))
        val lm = label("", 14f, gray).apply { setPadding(0, dp(6), 0, 0) }
        lastMissed = lm
        lastCard.addView(lm)
        root.addView(lastCard)

        // Permissions card
        

    private fun refresh() {
        val on = Prefs.busy(this)
        busyStatus?.apply {
            text = if (on) "● Busy mode is ON" else "● Busy mode is OFF"
            setTextColor(if (on) green else gray)
        }

        val sel = Prefs.selected(this)
        summary?.text =
            if (sel.isEmpty()) "No time selected: you won't get reminders."
            else "You'll be reminded after " + sel.joinToString(", ") + " min."

        val missing = missingPermissions()
        permStatus?.apply {
            text = if (missing.isEmpty()) "✓ All permissions allowed"
            else "Missing: " + missing.joinToString(", ")
            setTextColor(if (missing.isEmpty()) green else red)
        }

        val num = Prefs.lastNumber(this)
        lastMissed?.text =
            if (num == null) "No missed calls yet."
            else num + " at " + SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                .format(Date(Prefs.lastTime(this)))
    }

    private fun granted(p: String): Boolean =
        checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun missingPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (!granted(Manifest.permission.READ_PHONE_STATE)) list.add("Phone")
        if (!granted(Manifest.permission.READ_CALL_LOG)) list.add("Call logs")
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            list.add("Notifications")
        }
        return list
    }

    private fun askPermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 101)
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Open Settings > Apps > Missed Call Reminder", Toast.LENGTH_LONG).show()
        }
    }

    // ---------- small UI helpers ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(18).toFloat()
        }
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0, 0, 0, dp(14)) }
    }

    private fun switchRow(
        title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label(title, 16f, dark, true))
        col.addView(label(sub, 13f, gray))
        row.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        val sw = Switch(this)
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        row.addView(sw)
        return row
    }

    private fun chipParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }

    private fun styleChip(c: TextView, on: Boolean) {
        c.background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(if (on) primary else chipOff)
        }
        c.setTextColor(if (on) Color.WHITE else Color.parseColor("#374151"))
    }

    private fun chip(m: Int): TextView {
        val c = TextView(this)
        c.text = "$m min"
        c.textSize = 14f
        c.gravity = Gravity.CENTER
        c.setTypeface(c.typeface, Typeface.BOLD)
        styleChip(c, Prefs.isSelected(this, m))
        c.setOnClickListener {
            val now = !Prefs.isSelected(this, m)
            Prefs.setSelected(this, m, now)
            styleChip(c, now)
            refresh()
        }
        return c
    }

    private fun button(t: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.isAllCaps = false
        b.textSize = 15f
        b.stateListAnimator = null
        b.setTextColor(if (filled) Color.WHITE else primary)
        b.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (filled) setColor(primary) else {
                setColor(Color.WHITE)
                setStroke(dp(2), primary)
            }
        }
        b.layoutParams = LinearLayout.LayoutParams(MATCH, dp(50)).apply { setMargins(0, dp(12), 0, 0) }
        b.setOnClickListener { onClick() }
        return b
    }
}
