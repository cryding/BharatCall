package com.bharatcall.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class InCallActivity : Activity() {
    private lateinit var callInfo: TextView
    private lateinit var muteButton: TextView
    private lateinit var speakerButton: TextView
    private lateinit var holdButton: TextView
    private lateinit var answerButton: TextView

    private val bg = Color.rgb(7, 17, 23)
    private val panel = Color.rgb(19, 32, 42)
    private val white = Color.rgb(238, 245, 250)
    private val green = Color.rgb(0, 145, 100)
    private val red = Color.rgb(190, 48, 60)

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "📞 BharatCall\nIndia Connected • Safer Calling"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(white)
        }, LinearLayout.LayoutParams(-1, dp(80)))
        callInfo = TextView(this).apply {
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(8), dp(18), dp(8), dp(28))
        }
        root.addView(callInfo, LinearLayout.LayoutParams(-1, 0, 1f))
        answerButton = control("Answer", green) { CallState.answer(); refresh() }
        root.addView(answerButton, fullButton())
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        muteButton = control("Mute", panel) {
            if (!CallState.toggleMute()) showMessage("Mute is not available yet.")
            refresh()
        }
        speakerButton = control("Speaker", panel) {
            if (!CallState.toggleSpeaker()) showMessage("Audio routing is not ready yet.")
            refresh()
        }
        row.addView(muteButton, LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(3), dp(5), dp(3), dp(5)) })
        row.addView(speakerButton, LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(3), dp(5), dp(3), dp(5)) })
        root.addView(row)
        holdButton = control("Hold", panel) {
            if (!CallState.toggleHold()) showMessage("Hold is available only during a connected call.")
            refresh()
        }
        root.addView(holdButton, fullButton())
        root.addView(control("End call", red) { CallState.disconnect(); refresh() }, fullButton())
        root.addView(control("Back", panel) { finish() }, fullButton())
        setContentView(root)
        refresh()
    }

    private fun fullButton() = LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(4), 0, dp(4)) }

    private fun control(label: String, color: Int, click: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 16f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(18).toFloat() }
        isClickable = true
        isFocusable = true
        setOnClickListener { click() }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        if (!::callInfo.isInitialized) return
        callInfo.text = CallState.stateLabel()
        answerButton.visibility = if (CallState.currentCall?.state == android.telecom.Call.STATE_RINGING) android.view.View.VISIBLE else android.view.View.GONE
        muteButton.text = if (CallState.muted) "Unmute" else "Mute"
        speakerButton.text = if (CallState.speaker) "Speaker on" else "Speaker"
        holdButton.text = if (CallState.currentCall?.state == android.telecom.Call.STATE_HOLDING) "Resume call" else "Hold"
    }

    companion object {
        @Volatile private var active: InCallActivity? = null
        fun notifyCallChanged() { active?.runOnUiThread { active?.refresh() } }
    }

    override fun onStart() { super.onStart(); active = this }
    override fun onStop() { if (active === this) active = null; super.onStop() }
}
