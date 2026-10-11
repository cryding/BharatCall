package com.bharatcall.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "ANSWER" -> CallState.answer()
            "DECLINE" -> CallState.disconnect()
        }
    }
}
