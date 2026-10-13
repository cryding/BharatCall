package com.bharatcall.app.telephony

/**
 * Provider-neutral contract for an AI-assisted PSTN/SIP call.
 * Android InCallService cannot provide ordinary cellular call audio here;
 * a trusted telephony backend/provider must own the PSTN/SIP media path.
 */
enum class AiCallDirection { INCOMING, OUTGOING }
enum class AiBridgeState {
    NOT_CONFIGURED, CONFIGURED_NOT_CONNECTED, READY, STARTING, ACTIVE,
    TRANSFERRING_TO_HUMAN, ENDED, FAILED
}

data class AiTelephonyConfig(
    val backendBaseUrl: String,
    val agentId: String,
    val disclosureText: String,
    val allowHumanHandoff: Boolean = true
) {
    fun validationError(): String? {
        if (backendBaseUrl.trim().isEmpty()) return "Set the AI telephony backend URL first."
        if (!backendBaseUrl.trim().startsWith("https://", ignoreCase = true)) {
            return "The production AI telephony backend must use HTTPS."
        }
        if (agentId.isBlank()) return "Set an AI agent ID."
        if (disclosureText.isBlank()) return "Set the caller disclosure message."
        return null
    }
}

data class AiBridgeSession(
    val sessionId: String,
    val direction: AiCallDirection,
    val state: AiBridgeState,
    val statusMessage: String
)

/**
 * Implementations must call a server-side telephony provider. Never embed
 * provider credentials or AI provider secrets in the APK. A session must not
 * be ACTIVE until the backend confirms call routing and media connection.
 */
interface AiTelephonyBridge {
    suspend fun checkReadiness(config: AiTelephonyConfig): AiBridgeState
    suspend fun startSession(
        config: AiTelephonyConfig,
        direction: AiCallDirection,
        destination: String?
    ): AiBridgeSession
    suspend fun transferToHuman(sessionId: String): Boolean
    suspend fun endSession(sessionId: String)
}
