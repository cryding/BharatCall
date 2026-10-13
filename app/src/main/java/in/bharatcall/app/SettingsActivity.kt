package com.bharatcall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.widget.EditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
        section("AI Intelligence")
        item("✦", "Gemini API key", "Encrypted on this device", action = { editGeminiKey() })
        item("⇄", "AI telephony backend URL", "Configure HTTPS API endpoint", action = { editTelephonyBackendUrl() })
        item("⌁", "Test telephony backend", "Checks health and reports capabilities", action = { testTelephonyBackend() })
        item("↗", "Test Gemini connection", "Check the saved key with a minimal request", action = { testGeminiConnection() })
        item("◉", "AI incoming-call assistant", "Experimental • not active for carrier calls", action = {
            info("AI incoming-call assistant", "BharatCall cannot yet stream cellular call audio to Gemini or inject AI speech into a live carrier call. Android restricts third-party access to call audio. This option is informational only; no calls are automatically answered by AI. The Gemini key is used only when you test the connection.")
        })
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

    private fun editTelephonyBackendUrl() {
        val prefs = getSharedPreferences("ai_telephony", MODE_PRIVATE)
        val input = EditText(this).apply {
            hint = "https://your-backend.example"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(this@SettingsActivity.text)
            setHintTextColor(muted)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setText(prefs.getString("base_url", "") ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("AI telephony backend")
            .setMessage("Enter your deployed HTTPS backend URL without a trailing slash. This saves only the URL. It does not connect a carrier call or stream call audio.")
            .setView(input)
            .setPositiveButton("Save URL") { _, _ ->
                val value = input.text.toString().trim().trimEnd('/')
                val uri = try { Uri.parse(value) } catch (_: Exception) { null }
                if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank() || !uri.userInfo.isNullOrBlank()) {
                    info("Invalid backend URL", "Use a valid HTTPS URL without username/password, for example https://api.example.com")
                } else {
                    prefs.edit().putString("base_url", value).apply()
                    Toast.makeText(this, "Backend URL saved on this device.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Clear") { _, _ ->
                prefs.edit().remove("base_url").apply()
                Toast.makeText(this, "Backend URL removed.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testTelephonyBackend() {
        val baseUrl = getSharedPreferences("ai_telephony", MODE_PRIVATE).getString("base_url", "").orEmpty().trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            info("Telephony backend", "Set the HTTPS backend URL first in Settings → AI telephony backend URL.")
            return
        }
        val parsed = try { Uri.parse(baseUrl) } catch (_: Exception) { null }
        if (parsed == null || parsed.scheme != "https" || parsed.host.isNullOrBlank() || !parsed.userInfo.isNullOrBlank()) {
            info("Telephony backend", "The saved URL is invalid. Save a valid HTTPS URL first.")
            return
        }
        Toast.makeText(this, "Checking backend health…", Toast.LENGTH_SHORT).show()
        Thread {
            var result = "Could not reach the backend. Check the URL and internet connection."
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "application/json")
                }
                val code = connection.responseCode
                if (code in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val data = JSONObject(response)
                    val ok = data.optBoolean("ok", false)
                    val connected = data.optBoolean("telephonyConnected", false)
                    val mode = data.optString("mode", "unknown")
                    val requestId = data.optString("requestId", connection.getHeaderField("X-Request-Id").orEmpty())
                    val capabilities = data.optJSONObject("capabilities")
                    val capabilitySummary = if (capabilities != null) {
                        "\nCapabilities: carrier calls=${capabilities.optBoolean("carrierCalls", false)}, live audio=${capabilities.optBoolean("liveAudio", false)}, AI speech=${capabilities.optBoolean("aiSpeech", false)}, human handoff=${capabilities.optBoolean("humanHandoff", false)}."
                    } else {
                        "\nCapabilities are not reported by this server version."
                    }
                    result = if (ok && !connected) {
                        "Health check PASSED. Mode: $mode. Telephony provider/media is NOT connected; no AI phone calls can be made yet.$capabilitySummary\nRequest ID: ${requestId.ifBlank { "not supplied" }}"
                    } else if (ok && connected) {
                        "Backend responded, but this health response alone does not verify a live call or bidirectional media connection. Mode: $mode.$capabilitySummary\nRequest ID: ${requestId.ifBlank { "not supplied" }}"
                    } else {
                        "Backend responded, but its health payload was not recognized (HTTP $code).\nRequest ID: ${requestId.ifBlank { "not supplied" }}"
                    }
                } else {
                    result = "Backend health check failed (HTTP $code). Request ID: ${connection.getHeaderField("X-Request-Id").orEmpty().ifBlank { "not supplied" }}"
                }
            } catch (_: Exception) {
                result = "Could not reach the backend. Confirm it is deployed with HTTPS and /health is publicly reachable. If Render just deployed or woke from sleep, wait and retry."
            } finally { connection?.disconnect() }
            runOnUiThread { info("Telephony backend diagnostics", result) }
        }.start()
    }

    private fun editGeminiKey() {
        val input = EditText(this).apply {
            hint = "Paste Gemini API key"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(this@SettingsActivity.text)
            setHintTextColor(muted)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val existing = try { readEncryptedKey() } catch (_: Exception) { "" }
        input.setText(existing)
        AlertDialog.Builder(this)
            .setTitle("Gemini API key")
            .setMessage("Your key is encrypted using Android Keystore and stored only on this device. Never share it or commit it to GitHub.")
            .setView(input)
            .setPositiveButton("Save securely") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isBlank()) {
                    Toast.makeText(this, "API key is empty. Nothing saved.", Toast.LENGTH_LONG).show()
                } else {
                    try {
                        saveEncryptedKey(key)
                        Toast.makeText(this, "Gemini key saved encrypted on this device.", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Could not securely save the key on this device.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNeutralButton("Remove key") { _, _ ->
                getSharedPreferences("gemini_secure", MODE_PRIVATE).edit().remove("key_blob").apply()
                Toast.makeText(this, "Saved Gemini key removed.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testGeminiConnection() {
        val key = try { readEncryptedKey() } catch (_: Exception) { "" }
        if (key.isBlank()) {
            info("Gemini connection", "Add your Gemini API key first in Settings → Gemini API key.")
            return
        }
        Toast.makeText(this, "Testing Gemini connection…", Toast.LENGTH_SHORT).show()
        Thread {
            var result = "Gemini request failed. Check internet access, API key and Google AI Studio quota."
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + java.net.URLEncoder.encode(key, "UTF-8"))
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = JSONObject().put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", "Reply with exactly: BharatCall Gemini connection OK")))))
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code in 200..299) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val reply = JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
                    result = if (reply.isNotBlank()) "Connection successful. Gemini replied: " + reply.take(180) else "Connection successful. Gemini returned a valid response."
                } else {
                    val errorBody = try {
                        (connection.errorStream ?: connection.inputStream).bufferedReader().use { it.readText() }
                    } catch (_: Exception) { "" }
                    val apiMessage = try {
                        JSONObject(errorBody).optJSONObject("error")?.optString("message").orEmpty()
                    } catch (_: Exception) { "" }
                    result = when (code) {
                        400 -> "Gemini rejected the request (HTTP 400). " + apiMessage.take(220)
                        401, 403 -> "Google rejected the API key or access (HTTP " + code + "). " + apiMessage.take(180)
                        404 -> "Gemini model/endpoint was not found or is unavailable to this API key (HTTP 404). This build now tests gemini-3.8-flash. " + apiMessage.take(180)
                        429 -> "Google quota/rate limit reached (HTTP 429). Check the model's free-tier availability and AI Studio quota. " + apiMessage.take(180)
                        else -> "Gemini request failed (HTTP " + code + "). " + apiMessage.take(180)
                    }
                }
            } catch (_: Exception) {
                result = "Could not reach Gemini. Check internet access and try again."
            } finally { connection?.disconnect() }
            runOnUiThread { info("Gemini connection test", result) }
        }.start()
    }

    private fun saveEncryptedKey(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val blob = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        getSharedPreferences("gemini_secure", MODE_PRIVATE).edit().putString("key_blob", blob).apply()
    }

    private fun readEncryptedKey(): String {
        val blob = getSharedPreferences("gemini_secure", MODE_PRIVATE).getString("key_blob", null) ?: return ""
        val parts = blob.split(":", limit = 2)
        if (parts.size != 2) return ""
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "bharatcall_gemini_key_encryption"
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun openSystem(action: String, fallback: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { Toast.makeText(this, "$fallback unavailable on this phone.", Toast.LENGTH_LONG).show() }
    }
}
