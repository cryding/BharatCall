package com.bharatcall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.PhoneAccountHandle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var number: EditText
    private lateinit var status: TextView
    private lateinit var simButton: TextView
    private var selectedPhoneAccount: PhoneAccountHandle? = null
    private val roleRequest = 101
    private val teal = Color.rgb(0, 121, 107)
    private val ink = Color.rgb(25, 40, 60)
    private val pale = Color.rgb(232, 240, 242)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 35, 55)
        window.navigationBarColor = Color.rgb(20, 35, 55)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(Color.WHITE)
        }
        content.addView(TextView(this).apply {
            text = "🇮🇳 BharatCall"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(ink)
            setPadding(0, 0, 0, dp(4))
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "Simple, private phone dialer"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(20))
        }, matchWrap())

        status = TextView(this).apply {
            textSize = 14f
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = rounded(pale)
        }
        content.addView(status, LinearLayout.LayoutParams(-1, dp(44)).apply { bottomMargin = dp(12) })

        number = EditText(this).apply {
            hint = "Enter phone number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            textSize = 28f
            gravity = Gravity.CENTER
            isSingleLine = true
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(teal)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            contentDescription = "Phone number"
        }
        content.addView(number, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(12) })

        val keypad = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#"))
        keypad.forEach { keys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            keys.forEach { key ->
                val keyView = actionView(key, false) { number.append(key) }
                keyView.textSize = 24f
                row.addView(keyView, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                    marginStart = dp(3); marginEnd = dp(3); bottomMargin = dp(6)
                })
            }
            content.addView(row, LinearLayout.LayoutParams(-1, dp(60)))
        }

        content.addView(actionView("⌫  Delete", false) {
            val editable = number.text
            if (editable.isNotEmpty()) editable.delete(editable.length - 1, editable.length)
        }, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        content.addView(actionView("Set as default phone app", true) { requestDialerRole() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
        simButton = actionView("Choose SIM for calls", false) { chooseSimAccount() }
        content.addView(simButton,
            LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        content.addView(actionView("Call", true) { placeCall() },
            LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(8) })
        content.addView(actionView("Find saved contact name", false) { lookupContact() },
            LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(12) })
        content.addView(TextView(this).apply {
            text = "Online caller ID is currently disabled. Saved-contact lookup stays on this device."
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }, matchWrap())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }
        setContentView(scroll)
        intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart?.let { number.setText(it) }
        refreshStatus()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)

    private fun rounded(fill: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(10).toFloat()
    }

    private fun actionView(label: String, primary: Boolean, action: () -> Unit): TextView {
        val fill = if (primary) teal else pale
        val foreground = if (primary) Color.WHITE else ink
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(foreground)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minHeight = dp(48)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(fill)
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
        status.text = if (isDefaultDialer()) "Default phone app: BharatCall" else "Choose BharatCall as your default phone app to handle calls."
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

    private fun chooseSimAccount() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 104)
            return
        }
        val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val accounts = try {
            telecom.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            Toast.makeText(this, "Android did not allow SIM account access. Use the system SIM selector.", Toast.LENGTH_LONG).show()
            return
        }

        if (accounts.isEmpty()) {
            selectedPhoneAccount = null
            simButton.text = "SIM: system default"
            Toast.makeText(this, "No call-capable SIM accounts found. Android will choose the SIM.", Toast.LENGTH_LONG).show()
            return
        }

        val labels = accounts.mapIndexed { index, handle ->
            val accountLabel = telecom.getPhoneAccount(handle)?.label?.toString()?.takeIf { it.isNotBlank() }
            accountLabel ?: "SIM ${index + 1}"
        }.toTypedArray()

        val selectedIndex = accounts.indexOf(selectedPhoneAccount).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Choose SIM for outgoing calls")
            .setSingleChoiceItems(labels, if (selectedPhoneAccount == null) -1 else selectedIndex) { dialog, which ->
                selectedPhoneAccount = accounts[which]
                simButton.text = "SIM: ${labels[which]}"
                dialog.dismiss()
            }
            .setNeutralButton("Use system default") { _, _ ->
                selectedPhoneAccount = null
                simButton.text = "SIM: system default"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun placeCall() {
        val value = number.text.toString().trim()
        if (value.isBlank()) {
            Toast.makeText(this, "Enter a number first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isDefaultDialer()) {
            requestDialerRole()
            return
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 103)
            return
        }
        try {
            val extras = Bundle().apply {
                selectedPhoneAccount?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
            }
            (getSystemService(Context.TELECOM_SERVICE) as TelecomManager)
                .placeCall(Uri.fromParts("tel", value, null), extras)
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
            104 -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) chooseSimAccount()
                else Toast.makeText(this, "Phone-state permission was not granted. Android may use the system SIM selector.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun lookupContact() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 102)
            return
        }
        val value = number.text.toString().trim()
        val name = CallerLookup.findContactName(this, value)
        Toast.makeText(this, name ?: "No matching saved contact found.", Toast.LENGTH_LONG).show()
    }
}
