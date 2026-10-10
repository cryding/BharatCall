package com.bharatcall.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.Button
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
        root.addView(Button(this).apply { text = "Answer"; setTextColor(Color.WHITE); backgroundTintList = ColorStateList.valueOf(Color.rgb(0, 121, 107)); setOnClickListener { CallState.answer(); refresh() } })
        root.addView(Button(this).apply { text = "End call"; setTextColor(Color.WHITE); backgroundTintList = ColorStateList.valueOf(Color.rgb(183, 28, 28)); setOnClickListener { CallState.disconnect(); refresh() } })
        root.addView(Button(this).apply { text = "Back to dial pad"; setTextColor(Color.BLACK); backgroundTintList = ColorStateList.valueOf(Color.rgb(232, 240, 242)); setOnClickListener { finish() } })
        setContentView(root)
        refresh()
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() { status.text = CallState.stateLabel() }
}
