package com.bharatcall.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class InCallActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 35, 55)
        window.navigationBarColor = Color.rgb(20, 35, 55)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24,24,24,24); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "BharatCall"; textSize = 28f; setTextColor(Color.rgb(25, 40, 60)) })
        status = TextView(this).apply { textSize = 20f; setTextColor(Color.BLACK); setPadding(0,20,0,20) }
        root.addView(status)
        root.addView(makeAction("Answer", Color.rgb(0, 121, 107)) { CallState.answer(); refresh() })
        root.addView(makeAction("End call", Color.rgb(183, 28, 28)) { CallState.disconnect(); refresh() })
        root.addView(makeAction("Back to dial pad", Color.rgb(232, 240, 242), Color.rgb(25, 40, 60)) { finish() })
        setContentView(root)
        refresh()
    }
    private fun makeAction(label: String, fill: Int, ink: Int = Color.WHITE, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(ink)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply { setColor(fill); cornerRadius = 12f }
            setOnClickListener { action() }
        }
    }

    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() { status.text = CallState.stateLabel() }
}
