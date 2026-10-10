package com.bharatcall.app

import android.telecom.Call

object CallState {
    @Volatile var currentCall: Call? = null
    val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) { /* UI refreshes on resume */ }
    }
    fun stateLabel(): String = when (currentCall?.state) {
        Call.STATE_NEW -> "New call"
        Call.STATE_CONNECTING -> "Connecting…"
        Call.STATE_DIALING -> "Dialing…"
        Call.STATE_RINGING -> "Incoming call"
        Call.STATE_ACTIVE -> "Connected"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_DISCONNECTED -> "Call ended"
        else -> if (currentCall == null) "No active call" else "Call in progress"
    }
    fun answer() { currentCall?.answer(0) }
    fun disconnect() { currentCall?.disconnect() }
}
