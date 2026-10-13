package com.bharatcall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AiVoiceActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(5, 14, 21)
    private val panel = Color.rgb(17, 29, 39)
    private val ink = Color.rgb(235, 240, 247)
    private val muted = Color.rgb(164, 178, 194)
    private lateinit var transcript: TextView
    private lateinit var status: TextView
    private lateinit var listenButton: TextView
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var busy = false
    private val history = JSONArray()
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(18).toFloat() }
    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(16), dp(8), dp(16), dp(12)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("‹", 32f, ink).apply { gravity = Gravity.CENTER; contentDescription = "Back"; setOnClickListener { finish() } }, LinearLayout.LayoutParams(dp(48), dp(52)))
        header.addView(label("BharatCall AI Voice", 23f, ink).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(header)
        root.addView(label("Talk with Gemini inside the app. This is not a SIM phone call.", 14f, muted))
        status = label("Ready. Tap Listen to speak.", 14f, Color.rgb(43, 220, 145))
        root.addView(status)
        val scroll = ScrollView(this).apply { isFillViewport = true; background = rounded(panel) }
        transcript = label("Conversation will appear here.\n\nYour speech is sent to Android speech recognition; recognized text is sent to Gemini using your saved API key.", 16f, ink)
        scroll.addView(transcript)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(10); bottomMargin = dp(12) })
        listenButton = label("🎙  Listen", 19f, Color.WHITE).apply {
            gravity = Gravity.CENTER; background = rounded(Color.rgb(0, 140, 90)); isClickable = true; isFocusable = true
            setOnClickListener { startListening() }
        }
        root.addView(listenButton, LinearLayout.LayoutParams(-1, dp(58)))
        val stop = label("Stop speaking", 15f, ink).apply {
            gravity = Gravity.CENTER; background = rounded(panel); isClickable = true
            setOnClickListener { tts?.stop(); status.text = "Speech stopped." }
        }
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        setContentView(root)
        tts = TextToSpeech(this, this)
    }

    override fun onInit(result: Int) {
        ttsReady = result == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }

    private fun startListening() {
        if (busy) { Toast.makeText(this, "Please wait for the current answer.", Toast.LENGTH_SHORT).show(); return }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            info("Speech recognition unavailable", "This phone has no speech recognition service available. Install/enable a speech recognition service and try again.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 501)
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…"; listenButton.text = "🎙  Listening…" }
                    override fun onBeginningOfSpeech() { status.text = "I can hear you…" }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { status.text = "Recognizing speech…" }
                    override fun onError(error: Int) {
                        busy = false
                        status.text = "Speech recognition failed (code $error). Tap Listen and try again."
                        listenButton.text = "🎙  Listen"
                    }
                    override fun onResults(results: Bundle?) {
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (spoken.isBlank()) {
                            busy = false; status.text = "No speech recognized. Try again."; listenButton.text = "🎙  Listen"
                        } else askGemini(spoken)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            busy = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            busy = false; status.text = "Could not start listening."; listenButton.text = "🎙  Listen"
        }
    }

    private fun askGemini(spoken: String) {
        val key = try { readEncryptedKey() } catch (_: Exception) { "" }
        if (key.isBlank()) {
            busy = false; status.text = "Add a Gemini API key in Settings first."; listenButton.text = "🎙  Listen"
            info("Gemini API key missing", "Open BharatCall Settings → Gemini API key, save your key, then return here.")
            return
        }
        runOnUiThread {
            transcript.append("\n\nYou: $spoken")
            status.text = "Thinking…"
            listenButton.text = "⏳  Thinking…"
        }
        Thread {
            var answer: String? = null
            var error = "Gemini request failed. Check internet, API key and available quota."
            var conn: HttpURLConnection? = null
            try {
                conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 12000; readTimeout = 20000; doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("x-goog-api-key", key)
                }
                val contents = JSONArray()
                for (i in 0 until history.length()) contents.put(history.getJSONObject(i))
                contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", spoken))))
                val payload = JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are BharatCall AI, a helpful concise voice assistant. Reply naturally in the language the user speaks. You are inside an app, not on a phone call.")))).put("contents", contents)
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    answer = JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()?.takeIf { it.isNotBlank() }
                    if (answer == null) error = "Gemini returned no text. Try asking again."
                } else {
                    val body = try { (conn.errorStream ?: conn.inputStream).bufferedReader().use { it.readText() } } catch (_: Exception) { "" }
                    val message = try { JSONObject(body).optJSONObject("error")?.optString("message").orEmpty() } catch (_: Exception) { "" }
                    error = when (code) {
                        401, 403 -> "Google rejected this API key or model access (HTTP $code). $message"
                        429 -> "Gemini quota/rate limit reached (HTTP 429). Check free-tier quota. $message"
                        503 -> "Gemini is temporarily overloaded (HTTP 503). Wait a little and retry. $message"
                        else -> "Gemini request failed (HTTP $code). $message"
                    }
                }
            } catch (_: Exception) {
                error = "Could not reach Gemini. Check internet access and try again."
            } finally { conn?.disconnect() }
            runOnUiThread {
                busy = false
                listenButton.text = "🎙  Listen"
                if (answer != null) {
                    val response = answer!!
                    history.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", spoken))))
                    history.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", response))))
                    while (history.length() > 12) history.remove(0)
                    transcript.append("\nBharatCall AI: $response")
                    status.text = "Answer ready. Tap Listen to continue."
                    if (ttsReady) tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "bharatcall-ai-response")
                } else {
                    transcript.append("\nError: $error")
                    status.text = "Could not get an answer."
                    Toast.makeText(this, error.take(160), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
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
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
            else { status.text = "Microphone permission denied."; info("Microphone permission", "Allow microphone permission to use in-app voice chat.") }
        }
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    override fun onDestroy() {
        recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
