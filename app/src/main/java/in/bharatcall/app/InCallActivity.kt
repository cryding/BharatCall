package com.bharatcall.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class InCallActivity : Activity() {
    private lateinit var callInfo: TextView
    private lateinit var callerName: TextView
    private lateinit var callState: TextView
    private lateinit var answerButton: TextView
    private lateinit var rejectButton: TextView
    private lateinit var controls: LinearLayout
    private lateinit var incomingActions: LinearLayout
    private lateinit var muteButton: TextView
    private lateinit var speakerButton: TextView
    private lateinit var holdButton: TextView

    private val bg = Color.rgb(5, 15, 27)
    private val panel = Color.rgb(23, 35, 53)
    private val white = Color.rgb(247, 250, 255)
    private val muted = Color.rgb(174, 190, 211)
    private val blue = Color.rgb(20, 112, 245)
    private val green = Color.rgb(0, 195, 111)
    private val red = Color.rgb(244, 65, 75)

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 12, 29)
        window.navigationBarColor = Color.rgb(3, 12, 29)
        window.decorView.systemUiVisibility = 0

        val cinematicBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(3, 15, 43), Color.rgb(3, 20, 32), Color.rgb(8, 44, 30), Color.rgb(38, 17, 12)))
        val scroll = ScrollView(this).apply { isFillViewport = true; background = cinematicBg }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
            background = cinematicBg
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "☎  BharatCall"
            textSize = 26f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(white)
        }, LinearLayout.LayoutParams(-1, dp(44)))
        root.addView(TextView(this).apply {
            text = "India Connected  •  Safer Calling"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }, LinearLayout.LayoutParams(-1, dp(28)))

        val avatar = TextView(this).apply {
            text = "☎"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(20, 54, 91))
                setStroke(dp(3), Color.rgb(80, 205, 255))
            }
        }
        root.addView(avatar, LinearLayout.LayoutParams(dp(112), dp(112)).apply {
            gravity = Gravity.CENTER
            topMargin = dp(38)
            bottomMargin = dp(24)
        })

        callerName = TextView(this).apply {
            textSize = 29f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(white)
            setPadding(dp(8), dp(4), dp(8), dp(2))
        }
        root.addView(callerName, LinearLayout.LayoutParams(-1, -2))
        callInfo = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(211, 222, 239))
            setPadding(dp(8), dp(2), dp(8), dp(5))
        }
        root.addView(callInfo, LinearLayout.LayoutParams(-1, -2))
        callState = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(dp(8), dp(4), dp(8), dp(20))
        }
        root.addView(callState, LinearLayout.LayoutParams(-1, -2))

        incomingActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        answerButton = control("☎\nAnswer", green) { CallState.answer(); refresh() }
        rejectButton = control("✕\nDecline", red) { CallState.disconnect(); refresh() }
        incomingActions.addView(answerButton, LinearLayout.LayoutParams(0, dp(88), 1f).apply { setMargins(dp(6), dp(8), dp(6), dp(8)) })
        incomingActions.addView(rejectButton, LinearLayout.LayoutParams(0, dp(88), 1f).apply { setMargins(dp(6), dp(8), dp(6), dp(8)) })
        root.addView(incomingActions, LinearLayout.LayoutParams(-1, -2))

        controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val rowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val rowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        muteButton = control("♩\nMute", panel) {
            if (!CallState.toggleMute()) toast("Mute is not available yet.")
            refresh()
        }
        speakerButton = control("◖))\nSpeaker", panel) {
            if (!CallState.toggleSpeaker()) toast("Audio routing is not ready yet.")
            refresh()
        }
        val addCall = control("＋☎\nAdd call", panel) {
            try { startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { toast("Dial pad is unavailable.") }
        }
        val merge = control("⇄\nMerge", panel) { toast("Merge needs two active calls supported by your carrier.") }
        val video = control("▣\nVideo", panel) { toast("Video calling is not supported for this call type yet.") }
        holdButton = control("Ⅱ\nHold", panel) {
            if (!CallState.toggleHold()) toast("Hold is available during a connected call.")
            refresh()
        }
        listOf(muteButton, speakerButton, addCall).forEach { rowOne.addView(it, gridParams()) }
        listOf(merge, video, holdButton).forEach { rowTwo.addView(it, gridParams()) }
        controls.addView(rowOne)
        controls.addView(rowTwo)
        root.addView(controls, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        root.addView(control("End call", red) { CallState.disconnect(); finish() }, LinearLayout.LayoutParams(-1, dp(60)).apply {
            topMargin = dp(24)
        })
        setContentView(scroll)
        refresh()
    }

    private fun gridParams() = LinearLayout.LayoutParams(0, dp(104), 1f).apply {
        setMargins(dp(4), dp(5), dp(4), dp(5))
    }

    private fun control(label: String, color: Int, click: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setLineSpacing(dp(3).toFloat(), 1f)
        background = GradientDrawable().apply {
            if (color == panel) { shape = GradientDrawable.OVAL; setColor(Color.argb(110, 24, 38, 62)); setStroke(dp(1), Color.rgb(75, 94, 128)) } else { setColor(color); cornerRadius = dp(36).toFloat(); setStroke(dp(1), color) }
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { click() }
    }

    private fun lookupCallerName(phone: String): String? {
        if (phone.isBlank() || checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        return try {
            val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phone))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        } catch (_: Exception) { null }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        if (!::callInfo.isInitialized) return
        val call = CallState.currentCall
        val ringing = call?.state == Call.STATE_RINGING
        val phone = call?.details?.handle?.schemeSpecificPart.orEmpty()
        val resolvedName = lookupCallerName(phone)
        callerName.text = resolvedName ?: if (phone.isBlank()) "Unknown caller" else phone
        callInfo.text = if (resolvedName != null && phone.isNotBlank()) phone else ""
        callState.text = when (call?.state) {
            Call.STATE_RINGING -> "Incoming call…"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            Call.STATE_ACTIVE -> "Connected"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTED -> "Call ended"
            else -> "Call in progress"
        }
        incomingActions.visibility = if (ringing) View.VISIBLE else View.GONE
        controls.visibility = if (call != null && !ringing && call.state != Call.STATE_DISCONNECTED) View.VISIBLE else View.GONE
        muteButton.text = if (CallState.muted) "♩\nUnmute" else "♩\nMute"
        speakerButton.text = if (CallState.speaker) "◖))\nSpeaker on" else "◖))\nSpeaker"
        holdButton.text = if (call?.state == Call.STATE_HOLDING) "▶\nResume" else "Ⅱ\nHold"
    }

    companion object {
        @Volatile private var active: InCallActivity? = null
        fun notifyCallChanged() { active?.runOnUiThread { active?.refresh() } }
    }

    override fun onStart() { super.onStart(); active = this }
    override fun onStop() { if (active === this) active = null; super.onStop() }
}
