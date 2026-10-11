package com.bharatcall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telecom.PhoneAccountHandle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var number: EditText
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private var selectedPhoneAccount: PhoneAccountHandle? = null
    private var currentTab = "Keypad"
    private val roleRequest = 101
    private val teal = Color.rgb(0, 210, 145)
    private val ink = Color.rgb(235, 243, 249)
    private val muted = Color.rgb(153, 169, 184)
    private val panel = Color.rgb(19, 32, 42)
    private val bg = Color.rgb(7, 17, 23)
    private var tone: ToneGenerator? = null
    private val favorites by lazy { getSharedPreferences("favorites", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        tone = try { ToneGenerator(AudioManager.STREAM_DTMF, 75) } catch (_: Exception) { null }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(8)) }
        header.addView(label("📞 BharatCall", 27f, ink), LinearLayout.LayoutParams(-1, dp(42)))
        header.addView(label("India Connected  •  Safer Calling", 13f, muted), LinearLayout.LayoutParams(-1, dp(24)))
        root.addView(header)
        status = label("", 12f, muted)
        status.setPadding(dp(18), dp(6), dp(18), dp(8))
        root.addView(status)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), dp(5), dp(4), dp(5)); setBackgroundColor(Color.rgb(14, 26, 35)) }
        listOf("Favourites", "Recents", "Keypad", "Contacts").forEach { tab ->
            val title = when (tab) { "Favourites" -> "☆\nFavourites"; "Recents" -> "◷\nRecents"; "Keypad" -> "▦\nKeypad"; else -> "♙\nContacts" }
            nav.addView(label(title, 12f, if (tab == currentTab) teal else muted).apply {
                gravity = Gravity.CENTER; isClickable = true; isFocusable = true
                background = rounded(if (tab == currentTab) Color.rgb(24, 53, 62) else Color.TRANSPARENT)
                setOnClickListener { currentTab = tab; render() }
            }, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        root.addView(nav)
        setContentView(root)
        render()
    }

    override fun onDestroy() { tone?.release(); tone = null; super.onDestroy() }
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(16).toFloat() }
    private fun label(value: String, size: Float, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(5), dp(4), dp(5)) }
    private fun action(value: String, primary: Boolean = false, callback: () -> Unit) = TextView(this).apply {
        text = value; textSize = 16f; gravity = Gravity.CENTER; setTextColor(if (primary) Color.WHITE else ink)
        setPadding(dp(10), dp(10), dp(10), dp(10)); background = rounded(if (primary) Color.rgb(0, 137, 105) else Color.rgb(28, 43, 54))
        isClickable = true; isFocusable = true; setOnClickListener { callback() }
    }

    private fun render() {
        body.removeAllViews()
        status.text = if (isDefaultDialer()) "Default phone app enabled" else "Set BharatCall as your default phone app for full call handling."
        when (currentTab) { "Keypad" -> showKeypad(); "Recents" -> showRecents(); "Contacts" -> showContacts(false); else -> showContacts(true) }
    }

    private fun showKeypad() {
        number = EditText(this).apply {
            hint = "Enter number"; textSize = 27f; gravity = Gravity.CENTER_VERTICAL
            inputType = android.text.InputType.TYPE_CLASS_PHONE; isSingleLine = true
            setTextColor(ink); setHintTextColor(muted); background = rounded(panel)
            backgroundTintList = ColorStateList.valueOf(teal); setPadding(dp(14), dp(4), dp(14), dp(4))
            intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart?.let { setText(it) }
        }
        body.addView(number, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(12) })
        val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#"))
        keys.forEach { rowKeys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEach { key ->
                row.addView(action(key + if (key == "2") "\nABC" else if (key == "3") "\nDEF" else if (key == "4") "\nGHI" else if (key == "5") "\nJKL" else if (key == "6") "\nMNO" else if (key == "7") "\nPQRS" else if (key == "8") "\nTUV" else if (key == "9") "\nWXYZ" else if (key == "0") "\n+" else "", false) {
                    number.append(key)
                    val t = when (key) {
                        "0" -> ToneGenerator.TONE_DTMF_0; "1" -> ToneGenerator.TONE_DTMF_1; "2" -> ToneGenerator.TONE_DTMF_2
                        "3" -> ToneGenerator.TONE_DTMF_3; "4" -> ToneGenerator.TONE_DTMF_4; "5" -> ToneGenerator.TONE_DTMF_5
                        "6" -> ToneGenerator.TONE_DTMF_6; "7" -> ToneGenerator.TONE_DTMF_7; "8" -> ToneGenerator.TONE_DTMF_8
                        "9" -> ToneGenerator.TONE_DTMF_9; "*" -> ToneGenerator.TONE_DTMF_S; else -> ToneGenerator.TONE_DTMF_P
                    }
                    tone?.startTone(t, 120)
                }.apply { textSize = 20f }, LinearLayout.LayoutParams(0, dp(66), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            body.addView(row)
        }
        body.addView(action("⌫  Delete last digit") { val t = number.text; if (t.isNotEmpty()) t.delete(t.length - 1, t.length) })
        body.addView(action("Call", true) { placeCall(number.text.toString()) }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(12) })
        body.addView(action("Set as default phone app") { requestDialerRole() }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        body.addView(action("Choose SIM for outgoing calls") { chooseSimAccount() }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
    }

    private fun showRecents() {
        body.addView(label("Recent calls", 23f, ink))
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            body.addView(label("Grant call history permission to display your recent calls.", 14f, muted))
            body.addView(action("Grant call history permission", true) { requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), 105) })
            return
        }
        try {
            var count = 0
            contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION), null, null, CallLog.Calls.DATE + " DESC")?.use { c ->
                while (c.moveToNext() && count < 80) {
                    val name = c.getString(0).orEmpty()
                    val phone = c.getString(1).orEmpty()
                    val type = c.getInt(2)
                    val date = android.text.format.DateFormat.format("dd MMM, hh:mm a", c.getLong(3)).toString()
                    val duration = c.getLong(4)
                    val kind = when (type) { CallLog.Calls.INCOMING_TYPE -> "Incoming"; CallLog.Calls.OUTGOING_TYPE -> "Outgoing"; CallLog.Calls.MISSED_TYPE -> "Missed"; CallLog.Calls.REJECTED_TYPE -> "Rejected"; else -> "Call" }
                    addCallRow(name.ifBlank { phone }, phone, kind + " • " + date + if (duration > 0) " • " + duration + "s" else "")
                    count++
                }
            }
            if (count == 0) body.addView(label("No call history yet.", 14f, muted))
        } catch (_: SecurityException) { body.addView(label("Android denied call history access.", 14f, muted)) }
    }

    private fun showContacts(onlyFavorites: Boolean) {
        body.addView(label(if (onlyFavorites) "Favourites" else "Contacts", 23f, ink))
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            body.addView(label("Contacts stay on your phone. Grant permission to view them.", 14f, muted))
            body.addView(action("Grant contacts permission", true) { requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 102) })
            return
        }
        val items = mutableListOf<Triple<String, String, String>>()
        try {
            contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")?.use { c ->
                while (c.moveToNext() && items.size < 500) {
                    val n = c.getString(0).orEmpty(); val p = c.getString(1).orEmpty(); val key = p.filter { it.isDigit() || it == '+' }
                    if (n.isNotBlank() && p.isNotBlank() && (!onlyFavorites || favorites.getBoolean(key, false))) items.add(Triple(n, p, key))
                }
            }
        } catch (_: SecurityException) { }
        if (items.isEmpty()) body.addView(label(if (onlyFavorites) "No favourites yet. Tap ☆ beside a contact to save it." else "No contacts found.", 14f, muted))
        items.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(6), dp(6)); background = rounded(panel) }
            val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            detail.addView(label(item.first, 16f, ink)); detail.addView(label(item.second, 13f, muted))
            detail.setOnClickListener { currentTab = "Keypad"; render(); number.setText(item.second); number.setSelection(number.text.length) }
            row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(action(if (favorites.getBoolean(item.third, false)) "★" else "☆") { favorites.edit().putBoolean(item.third, !favorites.getBoolean(item.third, false)).apply(); render() }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(action("☎", true) { placeCall(item.second) }, LinearLayout.LayoutParams(dp(52), dp(48)))
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun addCallRow(name: String, phone: String, info: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(8), dp(8), dp(8)); background = rounded(panel) }
        val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detail.addView(label(name, 16f, ink)); detail.addView(label(phone, 13f, muted)); detail.addView(label(info, 12f, if (info.startsWith("Missed")) Color.rgb(255, 100, 110) else muted))
        row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(action("☎", true) { placeCall(phone) }, LinearLayout.LayoutParams(dp(54), dp(48)))
        body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun isDefaultDialer(): Boolean = if (Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true else (getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage == packageName
    private fun requestDialerRole() {
        if (isDefaultDialer()) {
            Toast.makeText(this, "BharatCall is already your default phone app.", Toast.LENGTH_SHORT).show()
            render()
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(this, "This phone does not expose the Android dialer role.", Toast.LENGTH_LONG).show()
                    return
                }
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), roleRequest)
            } else {
                startActivityForResult(
                    Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName),
                    roleRequest
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open default-phone selection: " + (e.message ?: "Android blocked the request"), Toast.LENGTH_LONG).show()
        }
    }

    private fun placeCall(raw: String) {
        val value = raw.trim()
        if (value.isBlank()) { Toast.makeText(this, "Enter a phone number first.", Toast.LENGTH_SHORT).show(); return }
        if (!isDefaultDialer()) { requestDialerRole(); return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 103); return }
        try {
            val extras = Bundle().apply { selectedPhoneAccount?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) } }
            (getSystemService(Context.TELECOM_SERVICE) as TelecomManager).placeCall(Uri.fromParts("tel", value, null), extras)
        } catch (e: Exception) { Toast.makeText(this, "Call failed: " + (e.message ?: "unknown error"), Toast.LENGTH_LONG).show() }
    }

    private fun chooseSimAccount() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 104); return }
        val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val accounts = try { telecom.callCapablePhoneAccounts } catch (_: Exception) { emptyList<PhoneAccountHandle>() }
        if (accounts.isEmpty()) { Toast.makeText(this, "No SIM accounts found; Android will choose the default.", Toast.LENGTH_LONG).show(); return }
        val names = accounts.mapIndexed { i, h -> telecom.getPhoneAccount(h)?.label?.toString()?.ifBlank { null } ?: "SIM " + (i + 1) }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choose SIM").setSingleChoiceItems(names, accounts.indexOf(selectedPhoneAccount)) { d, i -> selectedPhoneAccount = accounts[i]; d.dismiss() }.setNeutralButton("System default") { _, _ -> selectedPhoneAccount = null }.setNegativeButton("Cancel", null).show()
    }

    @Deprecated("Role callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == roleRequest) render() }

    @Deprecated("Runtime permission callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            102 -> if (granted) render() else Toast.makeText(this, "Contacts permission denied.", Toast.LENGTH_SHORT).show()
            103 -> if (granted) placeCall(number.text.toString()) else Toast.makeText(this, "Call permission denied.", Toast.LENGTH_SHORT).show()
            104 -> if (granted) chooseSimAccount() else Toast.makeText(this, "SIM selection permission denied.", Toast.LENGTH_SHORT).show()
            105 -> if (granted) render() else Toast.makeText(this, "Call history permission denied.", Toast.LENGTH_SHORT).show()
        }
    }
}
