package com.example.missedcallreminder

import android.content.Context

object Prefs {
    val CHOICES = listOf(1, 2, 3, 5, 10, 15, 20)
    private val DEFAULTS = setOf(5, 15, 20)

    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun busy(c: Context): Boolean = sp(c).getBoolean("busy", true)
    fun setBusy(c: Context, v: Boolean) { sp(c).edit().putBoolean("busy", v).apply() }

    fun vibrate(c: Context): Boolean = sp(c).getBoolean("vibrate", true)
    fun setVibrate(c: Context, v: Boolean) { sp(c).edit().putBoolean("vibrate", v).apply() }

    fun isSelected(c: Context, m: Int): Boolean = sp(c).getBoolean("sel_$m", m in DEFAULTS)
    fun setSelected(c: Context, m: Int, v: Boolean) { sp(c).edit().putBoolean("sel_$m", v).apply() }
    fun selected(c: Context): List<Int> = CHOICES.filter { isSelected(c, it) }

    fun saveLastMissed(c: Context, number: String) {
        sp(c).edit().putString("last_number", number)
            .putLong("last_time", System.currentTimeMillis()).apply()
    }
    fun lastNumber(c: Context): String? = sp(c).getString("last_number", null)
    fun lastTime(c: Context): Long = sp(c).getLong("last_time", 0L)
}
