package in.bharatcall.app

/**
 * Online caller ID intentionally remains disabled until a reviewed HTTPS API,
 * consent UX, server-side abuse controls, and retention policy are configured.
 * Never embed private API keys in a public APK.
 */
object OnlineCallerIdClient {
    fun isConfigured(): Boolean = BuildConfig.CALLER_ID_API_BASE_URL.startsWith("https://")
    fun lookup(number: String): String? = null
}
