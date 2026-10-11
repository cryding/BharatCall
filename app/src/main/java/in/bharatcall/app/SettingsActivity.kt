package com.bharatcall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private val bg = Color.rgb(27, 30, 34)
    private val card = Color.rgb(17, 19, 23)
    private val text = Color.rgb(222, 225, 233)
    private val muted = Color.rgb(169, 190, 220)
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun pill(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(20).toFloat() }
    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(5), dp(4), dp(5))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        header.addView(label("‹", 36f, text).apply { gravity = Gravity.CENTER; isClickable = true; setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(44), dp(52)))
        header.addView(label("Settings", 27f, text).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(0), dp(20), dp(20)) }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        fun section(title: String) {
            list.addView(label(title, 17f, muted).apply { setPadding(dp(30), dp(12), dp(4), dp(12)); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
        }
        fun item(icon: String, title: String, summary: String? = null, action: () -> Unit) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(26), dp(12), dp(16), dp(12)); background = pill(card)
                isClickable = true; isFocusable = true; setOnClickListener { action() }
            }
            row.addView(label(icon, 24f, text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), -2))
            val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
            copy.addView(label(title, 18f, text))
            if (summary != null) copy.addView(label(summary, 12f, Color.rgb(164, 170, 181)))
            row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(3) })
        }
        section("Call Assist")
        item("⬡", "Caller ID and spam", action = { info("Caller ID and spam", "Saved contacts are searched on-device. Online caller-ID and spam detection are not configured in this build yet.") })
        section("General")
        item("⚝", "Accessibility", action = { openSystem(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility settings") })
        item("◉", "Assisted dialling", action = { info("Assisted dialling", "International dialing assistance is not implemented yet. You can still dial numbers manually using the keypad.") })
        item("⊘", "Blocked numbers", action = { info("Blocked numbers", "BharatCall does not yet manage a local block list. Use your phone's system call-blocking controls if available.") })
        item("▣", "Calling accounts", action = { openSystem(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, "Default apps settings") })
        item("◉", "Call recording", action = { info("Call recording", "Call recording is not implemented. Recording availability and consent rules vary by device and region.") })
        item("☷", "Display options", action = { info("Display options", "The keypad and recents use the dark style shown in your reference. Additional display controls are not implemented yet.") })
        item("⇄", "Incoming call gesture", action = { info("Incoming call gesture", "Answer and decline controls are provided by the incoming-call screen. Custom gesture settings are not implemented yet.") })
        item("▱", "Quick responses", action = { info("Quick responses", "Custom SMS quick responses are not implemented yet.") })
        item("◖", "Sounds and vibration", action = { openSystem(Settings.ACTION_SOUND_SETTINGS, "Sound settings") })
        item("☷", "Voicemail", action = { info("Voicemail", "Voicemail availability depends on your SIM carrier. Check your phone app or carrier settings.") })
        item("♫", "Contact ringtones", action = {
            try { startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) }
            catch (_: Exception) { info("Contact ringtones", "The system Contacts app is unavailable.") }
        })
        item("ⓘ", "About BharatCall", action = { info("About BharatCall", "BharatCall • India Connected • Safer Calling\nSIM calls are handled by Android Telecom.") })
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0, top, 0, bottom)
            insets
        }
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun openSystem(action: String, fallback: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { Toast.makeText(this, "$fallback unavailable on this phone.", Toast.LENGTH_LONG).show() }
    }
}