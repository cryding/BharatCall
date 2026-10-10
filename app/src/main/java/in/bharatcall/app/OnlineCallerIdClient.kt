package com.bharatcall.app

/**
 * Online caller ID intentionally remains disabled until a reviewed HTTPS API,
 * consent UX, server-side abuse controls, and retention policy are configured.
 * Never embed private API keys in an APK.
 */
object OnlineCallerIdClient {
    fun isConfigured(): Boolean = false
    fun lookup(number: String): String? = null
}
