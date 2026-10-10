package com.bharatcall.app

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class CallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallState.inCallService = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallState.currentCall = call
        call.registerCallback(CallState.callback)
        try {
            val launch = Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(launch)
        } catch (_: Exception) {
            // Android/OEM may restrict background activity launch; system call UI can remain visible.
        }
        InCallActivity.notifyCallChanged()
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(CallState.callback)
        if (CallState.currentCall == call) CallState.currentCall = null
        InCallActivity.notifyCallChanged()
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        if (CallState.inCallService == this) CallState.inCallService = null
        super.onDestroy()
    }
}
