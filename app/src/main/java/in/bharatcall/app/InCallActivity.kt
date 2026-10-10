package com.bharatcall.app

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class InCallActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24,24,24,24) }
        root.addView(TextView(this).apply { text = "BharatCall"; textSize = 28f })
        status = TextView(this).apply { textSize = 20f; setPadding(0,20,0,20) }
        root.addView(status)
        root.addView(Button(this).apply { text = "Answer"; setOnClickListener { CallState.answer(); refresh() } })
        root.addView(Button(this).apply { text = "End call"; setOnClickListener { CallState.disconnect(); refresh() } })
        root.addView(Button(this).apply { text = "Back to dial pad"; setOnClickListener { finish() } })
        setContentView(root)
        refresh()
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() { status.text = CallState.stateLabel() }
}
