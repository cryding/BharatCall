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
import android.graphics.BitmapFactory
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
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var suggestions: LinearLayout
    private var contactsPermissionRequested = false
    private var pendingContactQuery = ""
    private var selectedPhoneAccount: PhoneAccountHandle? = null
    private var pendingVideoCall = false
    private var contactSearchInput: EditText? = null
    private var currentTab = "Recents"
    private val roleRequest = 101
    private val teal = Color.rgb(0, 190, 110)
    private val ink = Color.rgb(235, 240, 247)
    private val muted = Color.rgb(164, 178, 194)
    private val panel = Color.rgb(18, 29, 39)
    private val bg = Color.rgb(5, 14, 21)
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
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(12), dp(5)) }
        val brandRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brandRow.addView(android.widget.ImageView(this).apply {
            setImageResource(com.bharatcall.app.R.drawable.ic_bharatcall)
            contentDescription = "BharatCall logo"
        }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(8) })
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = label("BharatCall", 26f, Color.rgb(15, 93, 218)).apply {
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            val logoText = android.text.SpannableString("BharatCall")
            logoText.setSpan(android.text.style.ForegroundColorSpan(Color.rgb(255, 132, 0)), 6, 10, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = logoText
            setPadding(0, 0, 0, 0)
        }
        brandText.addView(brand)
        brandText.addView(label("India Connected  •  Safer Calling", 12f, muted).apply { setPadding(0, 0, 0, 0) })
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(label("⌕", 31f, ink).apply {
            gravity = Gravity.CENTER; contentDescription = "Search saved contacts"; isClickable = true; isFocusable = true
            setOnClickListener {
                currentTab = "Contacts"
                render()
                contactSearchInput?.requestFocus()
                contactSearchInput?.post {
                    contactSearchInput?.requestFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                        .showSoftInput(contactSearchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
        brandRow.addView(label("⋮", 28f, ink).apply {
            gravity = Gravity.CENTER; contentDescription = "More options"; isClickable = true; isFocusable = true
            setOnClickListener {
                val items = arrayOf("Contacts", "Settings", "Clear call history", "Help & feedback")
                AlertDialog.Builder(this@MainActivity)
                    .setItems(items) { _, which ->
                        when (which) {
                            0 -> { currentTab = "Contacts"; render() }
                            1 -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            2 -> confirmClearCallHistory()
                            3 -> showHelpAndFeedback()
                        }
                    }.show()
            }
        }, LinearLayout.LayoutParams(dp(32), dp(48)))
        header.addView(brandRow)
        val topTabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = rounded(Color.rgb(9, 19, 28))
        }
        listOf("Recents", "Contacts", "Favourites").forEach { tab ->
            val title = when (tab) { "Recents" -> "◷  Recents"; "Contacts" -> "♙  Contacts"; else -> "★  Favourites" }
            topTabs.addView(label(title, 14f, if (tab == currentTab) Color.WHITE else Color.rgb(177, 192, 207)).apply {
                tag = tab
                gravity = Gravity.CENTER
                isClickable = true; isFocusable = true
                background = rounded(if (tab == currentTab) Color.rgb(20, 75, 145) else Color.TRANSPARENT)
                setOnClickListener { currentTab = tab; render() }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        header.addView(topTabs, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })
        root.addView(header)
        status = label("", 12f, muted)
        status.visibility = android.view.View.GONE
        root.addView(status)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottomInset = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0, topInset, 0, bottomInset)
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), dp(5), dp(6), dp(5)); setBackgroundColor(Color.rgb(9, 19, 28)) }
        listOf("Recents", "Contacts", "Keypad").forEach { tab ->
            val iconText = when (tab) { "Recents" -> "◷"; "Contacts" -> "♙"; else -> "▦" }
            val caption = if (tab == "Keypad") "Dial" else tab
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true; isFocusable = true
                background = rounded(if (tab == currentTab) Color.rgb(20, 75, 145) else Color.rgb(17, 27, 37))
                addView(label(iconText, 27f, if (tab == currentTab) Color.rgb(196, 222, 255) else Color.rgb(177, 192, 207)).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, 0) })
                addView(label(caption, 12f, if (tab == currentTab) Color.WHITE else Color.rgb(177, 192, 207)).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, 0) })
                setOnClickListener { currentTab = tab; render() }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        root.addView(nav)
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 106)
        }
        render()
    }

    override fun onDestroy() { tone?.release(); tone = null; super.onDestroy() }
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(16).toFloat() }
    private fun label(value: String, size: Float, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(5), dp(4), dp(5)) }
    private fun action(value: String, primary: Boolean = false, callback: () -> Unit) = TextView(this).apply {
        text = value; textSize = 16f; gravity = Gravity.CENTER; setTextColor(if (primary) Color.WHITE else if (currentTab == "Recents") Color.WHITE else Color.rgb(15, 93, 218))
        setPadding(dp(10), dp(10), dp(10), dp(10)); background = rounded(if (primary) Color.rgb(0, 190, 80) else if (currentTab == "Recents") Color.rgb(22, 34, 45) else Color.rgb(239, 245, 253))
        isClickable = true; isFocusable = true; setOnClickListener { callback() }
    }

    private fun dialKey(digit: String, letters: String, callback: () -> Unit) = TextView(this).apply {
        text = if (letters.isBlank()) digit else "$digit\n$letters"
        textSize = 23f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(232, 235, 242))
        setLineSpacing(0f, 0.9f)
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(40).toFloat()
            setColor(Color.rgb(17, 18, 23))
            setStroke(dp(1), Color.rgb(17, 18, 23))
        }
        isClickable = true
        isFocusable = true
        contentDescription = if (letters.isBlank()) digit else "$digit, $letters"
        setOnClickListener { callback() }
    }

    private fun render() {
        body.removeAllViews()
        val dark = true
        val shell = body.parent?.parent as? LinearLayout
        val screen = Color.rgb(5, 14, 21)
        shell?.setBackgroundColor(screen)
        shell?.getChildAt(0)?.setBackgroundColor(screen)
        shell?.getChildAt(1)?.setBackgroundColor(screen)
        shell?.getChildAt(3)?.setBackgroundColor(Color.rgb(9, 19, 28))
        body.setBackgroundColor(screen)
        status.setTextColor(Color.rgb(175, 193, 207))
        status.text = if (isDefaultDialer()) "Default phone app" else ""
        status.visibility = if (isDefaultDialer()) android.view.View.VISIBLE else android.view.View.GONE
        val headerTabs = shell?.getChildAt(0) as? LinearLayout
        val topTabs = headerTabs?.getChildAt(1) as? LinearLayout
        topTabs?.let { bar ->
            for (i in 0 until bar.childCount) {
                val item = bar.getChildAt(i) as? TextView ?: continue
                val tab = item.tag as? String ?: continue
                item.setTextColor(if (tab == currentTab) Color.WHITE else Color.rgb(177, 192, 207))
                item.background = rounded(if (tab == currentTab) Color.rgb(20, 75, 145) else Color.TRANSPARENT)
            }
        }
        val nav = shell?.getChildAt(3) as? LinearLayout
        nav?.let { bar -> for (i in 0 until bar.childCount) {
            val item = bar.getChildAt(i) as? LinearLayout ?: continue
            val tab = listOf("Recents", "Contacts", "Keypad").getOrNull(i) ?: continue
            item.background = rounded(if (tab == currentTab) Color.rgb(20, 75, 145) else Color.rgb(17, 27, 37))
            for (j in 0 until item.childCount) {
                val textView = item.getChildAt(j) as? TextView ?: continue
                textView.setTextColor(if (tab == currentTab) if (j == 0) Color.rgb(196, 222, 255) else Color.WHITE else Color.rgb(177, 192, 207))
            }
        } }
        when (currentTab) { "Keypad" -> showKeypad(); "Recents" -> showRecents(); "Contacts" -> showContacts(false); else -> showContacts(true) }
    }

    private fun showKeypad() {
        number = EditText(this).apply {
            hint = "Enter number"; textSize = 28f; gravity = Gravity.CENTER_VERTICAL
            inputType = android.text.InputType.TYPE_CLASS_PHONE; isSingleLine = true
            setTextColor(Color.rgb(238, 240, 246)); setHintTextColor(Color.rgb(177, 182, 193)); background = null
            setPadding(dp(8), 0, dp(8), 0)
            intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart?.let { setText(it) }
        }
        val numberRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = rounded(Color.rgb(39, 41, 47)); setPadding(dp(12), 0, dp(10), 0) }
        numberRow.addView(label("🇮🇳  +91", 17f, Color.rgb(232, 235, 242)).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-2, dp(56)))
        numberRow.addView(number, LinearLayout.LayoutParams(0, dp(56), 1f))
        numberRow.addView(TextView(this).apply {
            text = "⌫"; textSize = 25f; gravity = Gravity.CENTER; setTextColor(Color.rgb(166, 202, 255))
            isClickable = true; isFocusable = true; contentDescription = "Delete last digit"
            setOnClickListener { val t = number.text; if (t.isNotEmpty()) t.delete(t.length - 1, t.length) }
        }, LinearLayout.LayoutParams(dp(42), dp(46)))
        body.addView(numberRow, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(4) })

        suggestions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(2), 0, dp(2)) }
        body.addView(suggestions, LinearLayout.LayoutParams(-1, -2))
        number.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateContactSuggestions(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        updateContactSuggestions(number.text.toString())

        val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#"))
        keys.forEach { rowKeys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            rowKeys.forEach { key ->
                row.addView(dialKey(key, when (key) { "1" -> "••"; "2" -> "ABC"; "3" -> "DEF"; "4" -> "GHI"; "5" -> "JKL"; "6" -> "MNO"; "7" -> "PQRS"; "8" -> "TUV"; "9" -> "WXYZ"; "0" -> "+"; else -> "" }) {
                    number.append(key)
                    val t = when (key) {
                        "0" -> ToneGenerator.TONE_DTMF_0; "1" -> ToneGenerator.TONE_DTMF_1; "2" -> ToneGenerator.TONE_DTMF_2
                        "3" -> ToneGenerator.TONE_DTMF_3; "4" -> ToneGenerator.TONE_DTMF_4; "5" -> ToneGenerator.TONE_DTMF_5
                        "6" -> ToneGenerator.TONE_DTMF_6; "7" -> ToneGenerator.TONE_DTMF_7; "8" -> ToneGenerator.TONE_DTMF_8
                        "9" -> ToneGenerator.TONE_DTMF_9; "*" -> ToneGenerator.TONE_DTMF_S; else -> ToneGenerator.TONE_DTMF_P
                    }
                    tone?.startTone(t, 100)
                }, LinearLayout.LayoutParams(0, dp(72), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            body.addView(row)
        }
        val callActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(5), 0, dp(2)) }
        fun footerAction(icon: String, caption: String, callback: () -> Unit) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; isFocusable = true
            addView(label(icon, 25f, Color.rgb(166, 202, 255)).apply { gravity = Gravity.CENTER })
            addView(label(caption, 12f, Color.rgb(166, 202, 255)).apply { gravity = Gravity.CENTER })
            setOnClickListener { callback() }
        }
        callActions.addView(footerAction("♙+", "Create contact") { try { startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)) } catch (_: Exception) { Toast.makeText(this, "Contacts app unavailable.", Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(0, dp(58), 1f))
        callActions.addView(TextView(this).apply {
            text = "☎   Call"; textSize = 18f; gravity = Gravity.CENTER; setTextColor(Color.rgb(10, 42, 25))
            background = GradientDrawable().apply { setColor(Color.rgb(89, 190, 119)); cornerRadius = dp(40).toFloat() }
            isClickable = true; isFocusable = true; contentDescription = "Call"
            setOnClickListener { placeCall(number.text.toString()) }
        }, LinearLayout.LayoutParams(dp(156), dp(60)).apply { setMargins(dp(6), 0, dp(6), 0) })
        callActions.addView(footerAction("▣", "Video call") { placeCall(number.text.toString(), true) }, LinearLayout.LayoutParams(0, dp(58), 1f))
        body.addView(callActions)
    }

    private fun updateContactSuggestions(query: String) {
        if (!::suggestions.isInitialized) return
        suggestions.removeAllViews()
        val digits = query.filter { it.isDigit() }
        if (digits.isBlank()) {
            suggestions.visibility = android.view.View.GONE
            return
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            suggestions.visibility = android.view.View.GONE
            if (!contactsPermissionRequested) {
                contactsPermissionRequested = true
                pendingContactQuery = query
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 102)
            }
            return
        }
        val matches = mutableListOf<Pair<String, String>>()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
            )?.use { cursor ->
                while (cursor.moveToNext() && matches.size < 4) {
                    val name = cursor.getString(0).orEmpty()
                    val phone = cursor.getString(1).orEmpty()
                    val normalized = phone.filter { it.isDigit() }
                    if (name.isNotBlank() && normalized.contains(digits)) matches.add(name to phone)
                }
            }
        } catch (_: Exception) {}
        suggestions.visibility = if (matches.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        matches.forEach { (name, phone) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(3), dp(12), dp(3)); background = rounded(Color.rgb(37, 41, 48))
                isClickable = true; isFocusable = true
                setOnClickListener {
                    number.setText(phone)
                    number.setSelection(number.text.length)
                    suggestions.visibility = android.view.View.GONE
                }
            }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(label(name, 15f, Color.rgb(225, 232, 244)))
            details.addView(label(phone, 12f, Color.rgb(166, 178, 196)))
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(action("☎", false) { placeCall(phone) }, LinearLayout.LayoutParams(dp(44), dp(42)))
            suggestions.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(3) })
        }
    }

        private fun showRecents() {
        body.addView(label("Recent calls", 23f, Color.rgb(240, 246, 252)))
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
        body.addView(label(if (onlyFavorites) "Favourites" else "Contacts", 23f, Color.rgb(235, 240, 247)))
        if (!onlyFavorites) {
            contactSearchInput = EditText(this).apply {
                hint = "Search contacts or numbers"
                textSize = 16f
                isSingleLine = true
                setTextColor(Color.rgb(235, 240, 247))
                setHintTextColor(muted)
                background = rounded(panel)
                setPadding(dp(14), dp(4), dp(14), dp(4))
                contentDescription = "Search saved contacts"
            }
            body.addView(contactSearchInput, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })
        } else contactSearchInput = null
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
        val contactRows = mutableListOf<Pair<LinearLayout, Triple<String, String, String>>>()
        items.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(6), dp(6)); background = rounded(panel); tag = (item.first + " " + item.second).lowercase() }
            row.addView(contactAvatar(item.first, item.second, 48))
            val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            detail.addView(label(item.first, 16f, Color.rgb(235, 240, 247))); detail.addView(label(item.second, 13f, muted))
            detail.setOnClickListener { currentTab = "Keypad"; render(); number.setText(item.second); number.setSelection(number.text.length) }
            row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(action(if (favorites.getBoolean(item.third, false)) "★" else "☆") { favorites.edit().putBoolean(item.third, !favorites.getBoolean(item.third, false)).apply(); render() }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(action("☎", true) { placeCall(item.second) }, LinearLayout.LayoutParams(dp(52), dp(48)))
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            contactRows.add(row to item)
        }
        contactSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                contactRows.forEach { (row, _) ->
                    row.visibility = if (query.isBlank() || (row.tag as? String).orEmpty().contains(query)) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun confirmClearCallHistory() {
        AlertDialog.Builder(this)
            .setTitle("Clear call history?")
            .setMessage("This removes call log entries from this phone. Contacts will not be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear history") { _, _ ->
                if (!isDefaultDialer()) {
                    Toast.makeText(this, "Set BharatCall as the default phone app first; Android restricts call-log deletion.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (checkSelfPermission(Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_CALL_LOG), 107)
                } else deleteCallHistory()
            }.show()
    }

    private fun deleteCallHistory() {
        try {
            val deleted = contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
            Toast.makeText(this, if (deleted > 0) "Call history cleared." else "No call history to clear.", Toast.LENGTH_LONG).show()
            if (currentTab == "Recents") render()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android did not allow clearing call history. Check default phone role and permissions.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not clear call history: " + (e.message ?: "unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private fun showHelpAndFeedback() {
        AlertDialog.Builder(this)
            .setTitle("Help & feedback")
            .setMessage("BharatCall uses Android Telecom for SIM calls. Grant Contacts permission for saved-contact search and set BharatCall as your default phone app for incoming and ongoing call screens. For feedback, include your phone model, Android version, and the steps that caused the issue.")
            .setPositiveButton("OK", null)
            .setNeutralButton("App info") { _, _ ->
                try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
            }.show()
    }

    private fun contactAvatar(name: String, phone: String, sizeDp: Int): android.view.View {
        val size = dp(sizeDp)
        val frame = android.widget.FrameLayout(this)
        val image = android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(size, size)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(63, 76, 94)) }
            clipToOutline = true
            contentDescription = "$name contact photo"
        }
        var photoUri: String? = null
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI), null, null, null)?.use { c ->
                if (c.moveToFirst()) photoUri = c.getString(0)
            }
            if (!photoUri.isNullOrBlank()) contentResolver.openInputStream(Uri.parse(photoUri!!))?.use { input ->
                BitmapFactory.decodeStream(input)?.let { image.setImageBitmap(it) }
            }
        } catch (_: Exception) { }
        frame.addView(image)
        if (image.drawable == null) {
            frame.addView(TextView(this).apply {
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                textSize = if (sizeDp >= 48) 23f else 18f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(220, 229, 241))
                layoutParams = android.widget.FrameLayout.LayoutParams(size, size)
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(57, 70, 88)) }
            })
        }
        frame.layoutParams = LinearLayout.LayoutParams(size, size).apply { rightMargin = dp(12) }
        return frame
    }

    private fun addCallRow(name: String, phone: String, info: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(8), dp(8), dp(8)); background = rounded(Color.rgb(17, 29, 39)) }
        row.addView(contactAvatar(name, phone, 54))
        val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detail.addView(label(name, 17f, Color.rgb(239, 243, 249))); detail.addView(label(phone, 13f, Color.rgb(165, 180, 197))); detail.addView(label(info, 12f, if (info.startsWith("Missed")) Color.rgb(255, 91, 105) else if (info.startsWith("Outgoing")) Color.rgb(43, 220, 145) else muted))
        row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = "☎"; textSize = 27f; gravity = Gravity.CENTER; setTextColor(Color.rgb(43, 224, 145))
            contentDescription = "Call $name"; isClickable = true; isFocusable = true
            setOnClickListener { placeCall(phone) }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))
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

    private fun placeCall(raw: String, video: Boolean = false) {
        val value = raw.trim()
        if (value.isBlank()) { Toast.makeText(this, "Enter a phone number first.", Toast.LENGTH_SHORT).show(); return }
        if (!isDefaultDialer()) { requestDialerRole(); return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { pendingVideoCall = video; requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 103); return }
        try {
            val extras = Bundle().apply {
                selectedPhoneAccount?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
                if (video) putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, android.telecom.VideoProfile.STATE_BIDIRECTIONAL)
            }
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == roleRequest) {
            render()
            if (!isDefaultDialer()) {
                AlertDialog.Builder(this)
                    .setTitle("Default phone access blocked")
                    .setMessage("If this APK was installed outside an app store, open App info > menu > Allow restricted settings, then try again. Android controls this security step; BharatCall cannot bypass it.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open App info") { _, _ ->
                        try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (_: Exception) { }
                    }
                    .show()
            }
        }
    }

    @Deprecated("Runtime permission callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            102 -> if (granted) {
                val pending = pendingContactQuery
                pendingContactQuery = ""
                render()
                if (currentTab == "Keypad" && pending.isNotBlank()) {
                    number.setText(pending)
                    number.setSelection(number.text.length)
                }
            } else Toast.makeText(this, "Contacts permission denied; suggestions are disabled.", Toast.LENGTH_SHORT).show()
            103 -> if (granted) { val video = pendingVideoCall; pendingVideoCall = false; placeCall(number.text.toString(), video) } else { pendingVideoCall = false; Toast.makeText(this, "Call permission denied.", Toast.LENGTH_SHORT).show() }
            104 -> if (granted) chooseSimAccount() else Toast.makeText(this, "SIM selection permission denied.", Toast.LENGTH_SHORT).show()
            105 -> if (granted) render() else Toast.makeText(this, "Call history permission denied.", Toast.LENGTH_SHORT).show()
            106 -> if (granted) Toast.makeText(this, "Call notifications enabled.", Toast.LENGTH_SHORT).show() else Toast.makeText(this, "Enable notifications so incoming-call UI can appear.", Toast.LENGTH_LONG).show()
            107 -> if (granted) deleteCallHistory() else Toast.makeText(this, "Android denied permission to clear call history.", Toast.LENGTH_LONG).show()
        }
    }
}
