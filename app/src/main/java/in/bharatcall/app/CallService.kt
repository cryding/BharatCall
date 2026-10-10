package in.bharatcall.app

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallState.currentCall = call
        call.registerCallback(CallState.callback)
        try {
            startActivity(Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        } catch (_: Exception) { /* Device may fall back to system call UI. */ }
    }
    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(CallState.callback)
        if (CallState.currentCall == call) CallState.currentCall = null
        super.onCallRemoved(call)
    }
}
