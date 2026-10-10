package com.bharatcall.app

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
        }
        root.addView(TextView(this).apply { text = "🇮🇳 BharatCall"; textSize = 30f; gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "Private-first Indian dialer • MVP"; gravity = Gravity.CENTER; setPadding(0, 8, 0, 18) })
        status = TextView(this)
        root.addView(status)
        number = EditText(this).apply { hint = "Enter phone number"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textSize = 24f; gravity = Gravity.CENTER; isSingleLine = true }
        root.addView(number, LinearLayout.LayoutParams(-1, 64))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { keys ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            keys.forEach { key -> row.addView(Button(this).apply { text = key; setOnClickListener { number.append(key) } }, LinearLayout.LayoutParams(0, 58, 1f)) }
            root.addView(row, LinearLayout.LayoutParams(-1, 60))
        }
        root.addView(Button(this).apply { text = "Delete"; setOnClickListener { val t = number.text; if (t.isNotEmpty()) t.delete(t.length - 1, t.length) } }, LinearLayout.LayoutParams(-1, 48))
        root.addView(Button(this).apply { text = "Set as default phone app"; setOnClickListener { requestDialerRole() } }, LinearLayout.LayoutParams(-1, 50))
        root.addView(Button(this).apply { text = "Call"; setOnClickListener { placeCall() } }, LinearLayout.LayoutParams(-1, 56))
        root.addView(Button(this).apply { text = "Find saved contact name"; setOnClickListener { lookupContact() } }, LinearLayout.LayoutParams(-1, 50))
        root.addView(TextView(this).apply { text = "Online caller ID is disabled until a reviewed HTTPS service is configured."; textSize = 12f; setPadding(0, 12, 0, 0) })
        setContentView(root)
        intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart?.let { number.setText(it) }
        refreshStatus()
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
