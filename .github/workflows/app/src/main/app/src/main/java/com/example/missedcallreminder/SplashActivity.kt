package com.example.missedcallreminder

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var opened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            try {
                window.statusBarColor = Color.parseColor("#4F46E5")
                window.navigationBarColor = Color.parseColor("#7C3AED")
            } catch (e: Throwable) { /* ignore */ }

            setContentView(buildUi())
            handler.postDelayed({ openMain() }, 2500)
        } catch (t: Throwable) {
            openMain()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun openMain() {
        if (opened) return
        opened = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildUi(): View {
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#4F46E5"), Color.parseColor("#7C3AED"))
            )
            setOnClickListener { openMain() }
        }

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
            alpha = 0f
        }

        val circle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
        }
        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.sym_call_missed)
            setColorFilter(Color.parseColor("#4F46E5"))
        }
        circle.addView(icon, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER))
        center.addView(circle, LinearLayout.LayoutParams(dp(112), dp(112)))

        center.addView(
            text("Missed Call Reminder", 28f, Color.WHITE, true).apply { setPadding(0, dp(28), 0, 0) }
        )
        center.addView(
            text("Never forget to call back", 16f, Color.parseColor("#E0E7FF")).apply {
                setPadding(0, dp(8), 0, 0)
            }
        )

        val pill = text(
            "Busy right now? We'll remind you after every missed call.",
            14f, Color.WHITE
        ).apply {
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#33FFFFFF"))
            }
        }
        center.addView(
            pill,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(36) }
        )

        root.addView(
            center,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        root.addView(
            text("Tap anywhere to continue", 12f, Color.parseColor("#C7D2FE")),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply { bottomMargin = dp(40) }
        )

        center.animate().alpha(1f).setDuration(700).start()
        return root
    }
}
