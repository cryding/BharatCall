package com.bharatcall.app

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var number: EditText
    private lateinit var status: TextView
    private val roleRequest = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 35, 55)
        window.navigationBarColor = Color.rgb(20, 35, 55)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply { text = "🇮🇳 BharatCall"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(Color.rgb(25, 40, 60)) })
        root.addView(TextView(this).apply { text = "Private-first Indian dialer • MVP"; gravity = Gravity.CENTER; setPadding(0, 8, 0, 18); setTextColor(Color.DKGRAY) })
        status = TextView(this).apply { setTextColor(Color.rgb(25, 40, 60)); textSize = 16f }
        root.addView(status)
        number = EditText(this).apply { hint = "Enter phone number"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textSize = 24f; gravity = Gravity.CENTER; isSingleLine = true; setTextColor(Color.BLACK); setHintTextColor(Color.GRAY); backgroundTintList = ColorStateList.valueOf(Color.rgb(0, 121, 107)) }
        root.addView(number, LinearLayout.LayoutParams(-1, 64))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { keys ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            keys.forEach { key -> row.addView(makeAction(key, false) { number.append(key) }, LinearLayout.LayoutParams(0, 58, 1f)) }
            root.addView(row, LinearLayout.LayoutParams(-1, 60))
        }
        root.addView(makeAction("Delete", false) { val t = number.text; if (t.isNotEmpty()) t.delete(t.length - 1, t.length) }, LinearLayout.LayoutParams(-1, 48))
        root.addView(makeAction("Set as default phone app", true) { requestDialerRole() }, LinearLayout.LayoutParams(-1, 50))
        root.addView(makeAction("Call", true) { placeCall() }, LinearLayout.LayoutParams(-1, 56))
        root.addView(makeAction("Find saved contact name", false) { lookupContact() }, LinearLayout.LayoutParams(-1, 50))
        root.addView(TextView(this).apply { text = "Online caller ID is disabled until a reviewed HTTPS service is configured."; textSize = 12f; setTextColor(Color.DKGRAY); setPadding(0, 12, 0, 0) })
        setContentView(root)
        intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart?.let { number.setText(it) }
        refreshStatus()
    }

    private fun makeAction(label: String, primary: Boolean, action: () -> Unit): TextView {
        val fill = if (primary) Color.rgb(0, 121, 107) else Color.rgb(232, 240, 242)
        val ink = if (primary) Color.WHITE else Color.rgb(25, 40, 60)
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(ink)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(12, 10, 12, 10)
            background = GradientDrawable().apply {
                setColor(fill)
                cornerRadius = 12f
            }
            setOnClickListener { action() }
        }
    }

    private fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            (getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage == packageName
        }
    }

    private fun refreshStatus() {
        status.text = if (isDefaultDialer()) "BharatCall is the default phone app." else "Set BharatCall as default to handle calls."
    }

    private fun requestDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), roleRequest)
            } else {
                Toast.makeText(this, "Dialer role unavailable on this device.", Toast.LENGTH_LONG).show()
            }
        } else {
            val request = android.content.Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivityForResult(request, roleRequest)
        }
    }

    @Deprecated("Uses role request callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == roleRequest) refreshStatus()
    }

    private fun placeCall() {
        val value = number.text.toString().trim()
        if (value.isBlank()) { Toast.makeText(this, "Enter a number first.", Toast.LENGTH_SHORT).show(); return }
        if (!isDefaultDialer()) { requestDialerRole(); return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 103); return
        }
        try {
            (getSystemService(Context.TELECOM_SERVICE) as TelecomManager).placeCall(Uri.fromParts("tel", value, null), Bundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start call: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Uses runtime permission callback for broad Android compatibility")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            103 -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) placeCall()
                else Toast.makeText(this, "Call permission is required to place a call.", Toast.LENGTH_LONG).show()
            }
            102 -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) lookupContact()
                else Toast.makeText(this, "Contacts permission was not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lookupContact() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 102); return
        }
        val name = CallerLookup.findContactName(this, number.text.toString().trim())
        Toast.makeText(this, name ?: "No matching saved contact found.", Toast.LENGTH_LONG).show()
    }
}
