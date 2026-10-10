package com.bharatcall.app

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile

object CallState {
    @Volatile var currentCall: Call? = null
    @Volatile var inCallService: CallService? = null
    @Volatile var muted: Boolean = false
    @Volatile var speaker: Boolean = false

    val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            currentCall = call
            InCallActivity.notifyCallChanged()
        }
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            InCallActivity.notifyCallChanged()
        }
    }

    fun stateLabel(): String {
        val call = currentCall ?: return "No active call"
        val name = call.details?.handle?.schemeSpecificPart.orEmpty()
        val state = when (call.state) {
            Call.STATE_NEW -> "Preparing call"
            Call.STATE_CONNECTING -> "Connecting…"
            Call.STATE_DIALING -> "Calling…"
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_ACTIVE -> "Connected"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTED -> "Call ended"
            else -> "Call in progress"
        }
        return if (name.isBlank()) state else name + "\n" + state
    }

    fun answer() { currentCall?.takeIf { it.state == Call.STATE_RINGING }?.answer(VideoProfile.STATE_AUDIO_ONLY) }
    fun disconnect() { currentCall?.disconnect() }
    fun toggleHold(): Boolean {
        val call = currentCall ?: return false
        return try {
            if (call.state == Call.STATE_HOLDING) call.unhold() else if (call.state == Call.STATE_ACTIVE) call.hold() else return false
            true
        } catch (_: Exception) { false }
    }
    fun toggleMute(): Boolean {
        val service = inCallService ?: return false
        muted = !muted
        service.setMuted(muted)
        return true
    }
    fun toggleSpeaker(): Boolean {
        val service = inCallService ?: return false
        speaker = !speaker
        service.setAudioRoute(if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        return true
    }
}
